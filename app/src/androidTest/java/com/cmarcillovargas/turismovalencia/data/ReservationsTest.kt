package com.cmarcillovargas.turismovalencia.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.ReservationAttemptEntity
import com.cmarcillovargas.turismovalencia.data.local.ReservationEntity
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.NoReservationsService
import com.cmarcillovargas.turismovalencia.data.remote.ReservationDto
import com.cmarcillovargas.turismovalencia.data.repo.ReservationActionFailure
import com.cmarcillovargas.turismovalencia.data.repo.ReservationFailure
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRefresh
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

/**
 * Reservas con el servidor y su caché en Room: el servidor pone el localizador y el total, cada intento lleva una clave de
 * idempotencia que se conserva hasta tener una respuesta definitiva (un corte de red no duplica la reserva), el identificador del móvil
 * no cambia y las reservas del servidor se ponen al día; también cancelar (en el servidor, que libera las plazas, o solo en el móvil
 * si es de demostración) y recuperar una reserva de otro móvil con su localizador y su correo. El servidor es falso
 * ([FakeReservationsServer]): no se toca la red.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReservationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val toClose = mutableListOf<() -> Unit>()
    private val date: LocalDate = LocalDate.now().plusDays(10)

    @After
    fun tearDown() {
        toClose.forEach { it() }
    }

    private inner class Session(
        val server: FakeReservationsServer = FakeReservationsServer(),
        val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build(),
    ) {
        val scope = testScope()
        val store = SettingsStore(db.settings(), scope)
        var clock = 1_000_000L
        private var keys = 0
        val repo = ReservationsRepository(db.reservations(), db.reservationAttempts(), store, server, scope, now = { clock }, newKey = { "key-${++keys}" })

        init {
            toClose += { scope.closeAndJoin(); db.close() }
        }

        fun create(offer: String = "albufera-barca", day: LocalDate = date, time: String = "18:30", adults: Int = 2, children: Int = 1, email: String = "a@b.es") =
            runBlocking { repo.create(offer, day, time, adults, children, email) }

        fun attempts() = runBlocking {
            db.openHelper.readableDatabase.query("SELECT idempotencyKey FROM reservation_attempts ORDER BY createdAt, idempotencyKey").use { c ->
                generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList()
            }
        }
    }

    private fun failureOf(block: () -> Unit): ReservationFailure {
        try {
            block()
        } catch (e: ReservationFailure) {
            return e
        }
        fail("Debía lanzar ReservationFailure")
        error("inalcanzable")
    }

    private fun actionFailureOf(block: () -> Unit): ReservationActionFailure {
        try {
            block()
        } catch (e: ReservationActionFailure) {
            return e
        }
        fail("Debía lanzar ReservationActionFailure")
        error("inalcanzable")
    }

    private fun <T> StateFlow<T>.awaitThat(predicate: (T) -> Boolean) = runBlocking { withTimeout(5_000) { first(predicate) } }

    @Test
    fun reservaEnElServidorYGuardaLoQueConfirma() {
        val s = Session(FakeReservationsServer { 1850 })

        val reservation = s.create(adults = 2, children = 1)

        // El localizador y el total los pone el servidor (precio por adulto × adultos; los niños no pagan).
        assertEquals("TV-${date.year}-1000", reservation.locator)
        assertEquals(3700, reservation.totalCents)
        assertEquals(ReservationStatus.Confirmed, reservation.status)
        assertEquals(date, reservation.date)
        assertEquals(3, reservation.people)
        assertEquals(reservation, s.repo.get(reservation.id))
        assertEquals(listOf(reservation.id), s.repo.items.value.map { it.id })
        // Lo que se mandó es lo que el usuario eligió, con la fecha en ISO y el correo tal cual.
        val sent = s.server.calls.single()
        assertEquals("albufera-barca", sent.request.offerId)
        assertEquals(date.toString(), sent.request.date)
        assertEquals("18:30", sent.request.time)
        assertEquals(2, sent.request.adults)
        assertEquals(1, sent.request.children)
        assertEquals("a@b.es", sent.request.email)
        assertEquals("key-1", sent.idempotencyKey)
        // Con la respuesta definitiva el intento se acaba.
        assertEquals(emptyList<String>(), s.attempts())
    }

    @Test
    fun elIdentificadorDelMovilNoCambiaNiAlReabrirLaBaseDeDatos() {
        val name = "reservations-${System.nanoTime()}.db"
        try {
            fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val first = Session(db = open())
            first.create()
            first.create(day = date.plusDays(1))
            val id = first.server.calls[0].deviceId
            assertEquals(id, first.server.calls[1].deviceId)
            // Es un UUID.
            assertEquals(id, UUID.fromString(id).toString())
            first.scope.closeAndJoin()
            first.db.close()

            val second = Session(db = open())
            second.create(day = date.plusDays(2))
            assertEquals(id, second.server.calls.single().deviceId)
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun unCorteDeRedReintentaConLaMismaClaveYNoDuplica() {
        val s = Session()
        s.server.loseNextResponses = 1

        // El servidor guardó la reserva pero la respuesta se perdió: la app no lo sabe.
        val failure = failureOf { s.create() }
        assertEquals(ReservationFailure.Reason.Unreachable, failure.reason)
        assertEquals(emptyList<Reservation>(), s.repo.items.value)
        assertEquals(listOf("key-1"), s.attempts())

        // Reintentar con los mismos datos manda la misma clave: el servidor devuelve la misma reserva.
        val reservation = s.create()

        assertEquals(listOf("key-1", "key-1"), s.server.calls.map { it.idempotencyKey })
        assertEquals("solo hay una reserva en el servidor", 1, s.server.stored.size)
        assertEquals("TV-${date.year}-1000", reservation.locator)
        assertEquals(listOf(reservation.id), s.repo.items.value.map { it.id })
        assertEquals(emptyList<String>(), s.attempts())
    }

    @Test
    fun laClaveSeConservaAunqueSeCierreLaApp() {
        val name = "attempts-${System.nanoTime()}.db"
        try {
            fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val server = FakeReservationsServer()
            server.loseNextResponses = 1
            val first = Session(server, open())
            failureOf { first.create() }
            first.scope.closeAndJoin()
            first.db.close()

            // La app se cerró entre medias. Al volver y reintentar con lo mismo, la clave es la de antes: no se duplica.
            val second = Session(server, open())
            val reservation = second.create()

            assertEquals("key-1", server.calls[0].idempotencyKey)
            assertEquals("la clave guardada gana a la que daría esta sesión", "key-1", server.calls[1].idempotencyKey)
            assertEquals(1, server.stored.size)
            assertEquals("TV-${date.year}-1000", reservation.locator)
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun conOtrosDatosEsOtroIntentoConOtraClave() {
        val s = Session()
        s.server.loseNextResponses = 1
        failureOf { s.create(adults = 2) }

        s.create(adults = 3)

        assertEquals(listOf("key-1", "key-2"), s.server.calls.map { it.idempotencyKey })
        // El primer intento sigue sin respuesta; el segundo, resuelto.
        assertEquals(listOf("key-1"), s.attempts())
    }

    @Test
    fun unaRespuestaDefinitivaCierraElIntento() {
        val s = Session()
        s.server.failures += FakeReservationsServer.error(409, "sold_out")

        val failure = failureOf { s.create() }

        assertEquals(ReservationFailure.Reason.SoldOut, failure.reason)
        assertEquals(emptyList<String>(), s.attempts())
        assertEquals(emptyList<Reservation>(), s.repo.items.value)
        // Si luego quedan plazas, es un intento nuevo con clave nueva.
        s.create()
        assertEquals(listOf("key-1", "key-2"), s.server.calls.map { it.idempotencyKey })
    }

    @Test
    fun losRechazosDefinitivosSeDistinguen() {
        val cases = mapOf(
            FakeReservationsServer.error(404, "offer_not_found") to ReservationFailure.Reason.OfferGone,
            FakeReservationsServer.error(422, "invalid_date") to ReservationFailure.Reason.InvalidDate,
            FakeReservationsServer.error(422, "invalid_slot") to ReservationFailure.Reason.InvalidSlot,
            FakeReservationsServer.error(422, "invalid_party") to ReservationFailure.Reason.InvalidParty,
            FakeReservationsServer.error(422, "invalid_email") to ReservationFailure.Reason.InvalidEmail,
            FakeReservationsServer.error(400, "invalid_request") to ReservationFailure.Reason.Rejected,
        )
        cases.forEach { (error, expected) ->
            val s = Session()
            s.server.failures += error
            assertEquals(error.code, expected, failureOf { s.create() }.reason)
            assertEquals("un rechazo definitivo no deja intento pendiente (${error.code})", emptyList<String>(), s.attempts())
        }
    }

    @Test
    fun losFallosSinRespuestaDefinitivaConservanLaClave() {
        val s = Session()

        s.server.failures += FakeReservationsServer.error(503, "internal_error")
        assertEquals(ReservationFailure.Reason.Server, failureOf { s.create() }.reason)
        assertEquals(listOf("key-1"), s.attempts())

        s.server.failures += FakeReservationsServer.error(429, "rate_limited", retryAfterSeconds = 30)
        val limited = failureOf { s.create() }
        assertEquals(ReservationFailure.Reason.RateLimited, limited.reason)
        assertEquals(30, limited.retryAfterSeconds)

        s.create()

        // Los tres intentos llevaron la misma clave.
        assertEquals(listOf("key-1", "key-1", "key-1"), s.server.calls.map { it.idempotencyKey })
        assertEquals(emptyList<String>(), s.attempts())
    }

    @Test
    fun unaRespuestaQueLaAppNoEntiendeNoSeGuardaYSeReintentaConLaMismaClave() {
        val s = Session()
        s.server.forcedReservation = ReservationDto(id = "x", locator = "TV-1", offerId = "o", date = "no es una fecha", time = "10:00", adults = 1, totalCents = 100)

        val failure = failureOf { s.create() }

        assertEquals(ReservationFailure.Reason.Server, failure.reason)
        assertEquals(emptyList<Reservation>(), s.repo.items.value)
        assertEquals(listOf("key-1"), s.attempts())
    }

    @Test
    fun laReservaYaConfirmadaNoSePierdeSiQuienLaPideSeVa() {
        val s = Session()
        s.server.delayMillis = 400

        runBlocking {
            val caller = async(Dispatchers.Default) { s.repo.create("albufera-barca", date, "18:30", 2, 0, "a@b.es") }
            while (s.server.calls.isEmpty()) kotlinx.coroutines.delay(10)
            caller.cancel() // se cierra la pantalla mientras el servidor responde
        }

        // La reserva ya está confirmada en el servidor: tiene que acabar guardada.
        val saved = s.repo.items.awaitThat { it.isNotEmpty() }
        assertEquals("TV-${date.year}-1000", saved.single().locator)
        assertEquals(emptyList<String>(), s.attempts())
    }

    @Test
    fun lasReservasDelServidorSePonenAlDiaSinDuplicar() {
        val server = FakeReservationsServer()
        val other = Session(server) // otra instalación de la misma persona ya creó una reserva con este móvil
        other.create(email = "x@y.es")
        val s = Session(server, Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build())
        assertTrue(s.repo.items.value.isEmpty())
        runBlocking { s.store.deviceId() } // este móvil ya había reservado alguna vez: tiene su identificador

        val first = runBlocking { s.repo.refresh() }
        val again = runBlocking { s.repo.refresh() }

        assertEquals(ReservationsRefresh.Updated(1), first)
        assertEquals(ReservationsRefresh.Updated(1), again)
        val items = s.repo.items.awaitThat { it.isNotEmpty() }
        assertEquals(1, items.size)
        assertEquals("TV-${date.year}-1000", items.single().locator)
        assertEquals("x@y.es", items.single().email)
    }

    @Test
    fun unMovilQueNuncaHaReservadoNoPideNadaNiCreaIdentificador() {
        val server = FakeReservationsServer()
        val s = Session(server)

        val result = runBlocking { s.repo.refresh() }

        assertEquals(ReservationsRefresh.NothingToAsk, result)
        assertEquals("no debía llamar al servidor", emptyList<String>(), server.listCalls)
        assertNull(runBlocking { s.store.deviceIdOrNull() })

        // En cuanto reserva tiene identificador, y desde entonces sí se pone al día.
        s.create()
        assertNotNull(runBlocking { s.store.deviceIdOrNull() })
        assertEquals(ReservationsRefresh.Updated(1), runBlocking { s.repo.refresh() })
        assertEquals(listOf(server.calls.single().deviceId), server.listCalls)
    }

    @Test
    fun unaReservaCanceladaEnElServidorSeVeCancelada() {
        val server = FakeReservationsServer()
        val s = Session(server)
        val created = s.create()
        // El servidor la cancela (hoy solo se puede en la base de datos): al ponerlo al día, la app lo refleja.
        val cancelled = server.stored.single().copy(status = "cancelled")
        server.forcedList = listOf(cancelled)

        runBlocking { s.repo.refresh() }

        val updated = s.repo.items.awaitThat { list -> list.any { it.id == created.id && it.status == ReservationStatus.Cancelled } }
        assertEquals(1, updated.size)
    }

    @Test
    fun siElServidorNoRespondeSeSigueConLasGuardadas() {
        val s = Session(FakeReservationsServer())
        val reservation = s.create()
        val offline = ReservationsRepository(s.db.reservations(), s.db.reservationAttempts(), s.store, NoReservationsService, s.scope)

        val result = runBlocking { offline.refresh() }

        assertTrue(result is ReservationsRefresh.Failed)
        assertEquals(listOf(reservation.id), s.repo.items.value.map { it.id })
    }

    @Test
    fun losIntentosDeHaceMasDeUnDiaSeOlvidanAlPonerAlDia() {
        val s = Session()
        val old = ReservationAttemptEntity("key-viejo", "albufera-barca", date.toEpochDay(), "18:30", 2, 0, "a@b.es", s.clock - 25L * 3_600_000)
        val recent = ReservationAttemptEntity("key-reciente", "albufera-barca", date.toEpochDay(), "19:30", 2, 0, "a@b.es", s.clock - 1L * 3_600_000)
        runBlocking {
            s.db.reservationAttempts().insert(old)
            s.db.reservationAttempts().insert(recent)
        }

        runBlocking { s.repo.refresh() }

        assertEquals(listOf("key-reciente"), s.attempts())
    }

    @Test
    fun laMismaReservaDosVecesEnElMismoIntentoDevuelveLaMisma() {
        val s = Session()
        val one = s.create()
        // Un segundo toque con los mismos datos, después de tener respuesta, es otro intento: el servidor crea otra reserva.
        val two = s.create()

        assertNotEquals(one.id, two.id)
        assertEquals(listOf("key-1", "key-2"), s.server.calls.map { it.idempotencyKey })
        assertNotNull(s.repo.get(two.id))
    }

    // ---- Cancelar

    @Test
    fun cancelarLaReservaDelServidorLaDejaCanceladaConSusDatos() {
        val s = Session()
        val created = s.create(email = "A@B.es")

        val cancelled = runBlocking { s.repo.cancel(created.id) }

        assertEquals(created.copy(status = ReservationStatus.Cancelled), cancelled)
        assertEquals(ReservationStatus.Cancelled, s.repo.get(created.id)?.status)
        assertEquals("el servidor la tiene cancelada", "cancelled", s.server.stored.single().status)
        // Se mandó el móvil que reservó, el localizador y el correo con el que se hizo.
        val sent = s.server.cancelCalls.single()
        assertEquals(s.server.calls.single().deviceId, sent.deviceId)
        assertEquals(created.locator, sent.locator)
        assertEquals(created.email, sent.email)
        assertEquals(1, s.repo.items.value.size)
    }

    @Test
    fun cancelarDosVecesNoFalla() {
        val s = Session()
        val created = s.create()
        runBlocking { s.repo.cancel(created.id) }

        val again = runBlocking { s.repo.cancel(created.id) }

        assertEquals(ReservationStatus.Cancelled, again.status)
        assertEquals(1, s.server.stored.size)
    }

    @Test
    fun lasDeDemostracionSeCancelanSoloEnElMovil() {
        val s = Session()
        val demo = ReservationEntity(
            id = "seed-1", offerId = "albufera-barca", title = "Paseo en barca", place = "El Palmar", epochDay = date.toEpochDay(), time = "18:30",
            adults = 2, children = 0, email = "laura.garcia@example.com", totalCents = 3600, status = ReservationStatus.Confirmed,
            locator = "TV-${date.year}-0413", createdAt = s.clock,
        )
        runBlocking { s.db.reservations().upsert(demo) }
        s.repo.items.awaitThat { it.isNotEmpty() }

        val cancelled = runBlocking { s.repo.cancel("seed-1") }

        assertEquals(ReservationStatus.Cancelled, cancelled.status)
        assertEquals("el servidor no conoce las de demostración", emptyList<FakeReservationsServer.CancelCall>(), s.server.cancelCalls)
        assertNull("y no se crea un identificador de móvil para nada", runBlocking { s.store.deviceIdOrNull() })
    }

    @Test
    fun cancelarUnaQueNoExisteEnElMovilEsNoEncontrada() {
        val s = Session()

        assertEquals(ReservationActionFailure.Reason.NotFound, actionFailureOf { runBlocking { s.repo.cancel("no-existe") } }.reason)
        assertEquals(emptyList<FakeReservationsServer.CancelCall>(), s.server.cancelCalls)
    }

    @Test
    fun cancelarSinConexionDejaLaReservaComoEstaba() {
        val s = Session()
        val created = s.create()
        val offline = ReservationsRepository(s.db.reservations(), s.db.reservationAttempts(), s.store, NoReservationsService, s.scope)

        val failure = actionFailureOf { runBlocking { offline.cancel(created.id) } }

        assertEquals(ReservationActionFailure.Reason.Unreachable, failure.reason)
        assertEquals(ReservationStatus.Confirmed, s.repo.get(created.id)?.status)
    }

    @Test
    fun losRechazosDelServidorAlCancelarSeDistinguenYNoCambianNada() {
        val cases = mapOf(
            FakeReservationsServer.error(409, "reservation_past") to ReservationActionFailure.Reason.TooLate,
            FakeReservationsServer.error(404, "reservation_not_found") to ReservationActionFailure.Reason.NotFound,
            FakeReservationsServer.error(503, "internal_error") to ReservationActionFailure.Reason.Server,
            FakeReservationsServer.error(429, "rate_limited", retryAfterSeconds = 120) to ReservationActionFailure.Reason.RateLimited,
            FakeReservationsServer.error(400, "invalid_request") to ReservationActionFailure.Reason.Rejected,
        )
        cases.forEach { (error, expected) ->
            val s = Session()
            val created = s.create()
            s.server.failures += error

            val failure = actionFailureOf { runBlocking { s.repo.cancel(created.id) } }

            assertEquals(error.code, expected, failure.reason)
            assertEquals("no cambia nada (${error.code})", ReservationStatus.Confirmed, s.repo.get(created.id)?.status)
        }
    }

    @Test
    fun unaActividadQueYaEmpezoNoSeCancelaYSigueComoEstaba() {
        val s = Session()
        val created = s.create()
        s.server.today = { date.plusDays(1) } // para el servidor, la actividad ya pasó

        val failure = actionFailureOf { runBlocking { s.repo.cancel(created.id) } }

        assertEquals(ReservationActionFailure.Reason.TooLate, failure.reason)
        assertEquals(ReservationStatus.Confirmed, s.repo.get(created.id)?.status)
        assertEquals("confirmed", s.server.stored.single().status)
    }

    @Test
    fun unCorteDeRedAlCancelarNoRompeNadaYReintentarEsSeguro() {
        val s = Session()
        val created = s.create()
        s.server.loseNextResponses = 1 // el servidor la cancela pero la respuesta se pierde

        val failure = actionFailureOf { runBlocking { s.repo.cancel(created.id) } }

        assertEquals(ReservationActionFailure.Reason.Unreachable, failure.reason)
        assertEquals("la app aún no lo sabe", ReservationStatus.Confirmed, s.repo.get(created.id)?.status)
        // Cancelar otra vez una ya cancelada devuelve la misma: la app se pone al día.
        assertEquals(ReservationStatus.Cancelled, runBlocking { s.repo.cancel(created.id) }.status)
        assertEquals(1, s.server.stored.size)
    }

    @Test
    fun laCancelacionYaHechaNoSePierdeSiQuienLaPideSeVa() {
        val s = Session()
        val created = s.create()
        s.server.delayMillis = 400

        runBlocking {
            val caller = async(Dispatchers.Default) { s.repo.cancel(created.id) }
            while (s.server.cancelCalls.isEmpty()) kotlinx.coroutines.delay(10)
            caller.cancel() // se cierra la pantalla mientras el servidor responde
        }

        s.repo.items.awaitThat { list -> list.any { it.id == created.id && it.status == ReservationStatus.Cancelled } }
    }

    @Test
    fun unaReservaCanceladaNoVuelveAConfirmarseAlPonerlaAlDia() {
        val s = Session()
        val created = s.create()
        runBlocking { s.repo.cancel(created.id) }

        runBlocking { s.repo.refresh() }

        assertEquals(ReservationStatus.Cancelled, s.repo.get(created.id)?.status)
    }

    // ---- Recuperar

    @Test
    fun recuperarGuardaLaReservaDeOtroMovil() {
        val server = FakeReservationsServer()
        val original = Session(server).create(email = "laura@example.com")
        val s = Session(server)
        assertTrue(s.repo.items.value.isEmpty())

        val recovered = runBlocking { s.repo.recover(" tv-${date.year}-1000 ", " Laura@Example.com ") }

        assertEquals(original, recovered)
        assertEquals(listOf(original.id), s.repo.items.value.map { it.id })
        // Se pidió con el localizador en mayúsculas y sin espacios, y el correo sin espacios.
        assertEquals(original.locator to "Laura@Example.com", server.findCalls.single())
        assertNull("recuperar no crea identificador de móvil", runBlocking { s.store.deviceIdOrNull() })
    }

    @Test
    fun recuperarConUnCorreoOUnLocalizadorQueNoCoincidenNoGuardaNada() {
        val server = FakeReservationsServer()
        val original = Session(server).create(email = "laura@example.com")
        val s = Session(server)

        listOf(original.locator to "otra@example.com", "TV-${date.year}-9999" to "laura@example.com").forEach { (locator, email) ->
            val failure = actionFailureOf { runBlocking { s.repo.recover(locator, email) } }
            assertEquals(ReservationActionFailure.Reason.NotFound, failure.reason)
        }
        assertEquals(emptyList<Reservation>(), s.repo.items.value)
    }

    @Test
    fun unaReservaRecuperadaSePuedeCancelarConSuCorreo() {
        val server = FakeReservationsServer()
        val original = Session(server).create(email = "laura@example.com")
        val s = Session(server)
        runBlocking { s.repo.recover(original.locator, "laura@example.com") }

        val cancelled = runBlocking { s.repo.cancel(original.id) }

        assertEquals(ReservationStatus.Cancelled, cancelled.status)
        val sent = server.cancelCalls.single()
        assertNotEquals("la cancela otro móvil", server.calls.single().deviceId, sent.deviceId)
        assertEquals("laura@example.com", sent.email)
        assertEquals("cancelled", server.stored.single().status)
    }

    @Test
    fun recuperarUnaQueYaEstabaEnElMovilNoLaDuplica() {
        val s = Session()
        val created = s.create(email = "laura@example.com")

        runBlocking { s.repo.recover(created.locator, "laura@example.com") }

        assertEquals(listOf(created.id), s.repo.items.value.map { it.id })
    }

    @Test
    fun recuperarSinConexionFallaSinTocarLoGuardado() {
        val s = Session()
        val created = s.create()
        val offline = ReservationsRepository(s.db.reservations(), s.db.reservationAttempts(), s.store, NoReservationsService, s.scope)

        val failure = actionFailureOf { runBlocking { offline.recover(created.locator, created.email) } }

        assertEquals(ReservationActionFailure.Reason.Unreachable, failure.reason)
        assertEquals(listOf(created.id), s.repo.items.value.map { it.id })
    }

    @Test
    fun unaRespuestaQueLaAppNoEntiendeAlRecuperarNoSeGuarda() {
        val s = Session()
        s.server.forcedReservation = ReservationDto(id = "x", locator = "TV-1", offerId = "o", date = "no es una fecha", time = "10:00", adults = 1, totalCents = 100)

        val failure = actionFailureOf { runBlocking { s.repo.recover("TV-1", "a@b.es") } }

        assertEquals(ReservationActionFailure.Reason.Server, failure.reason)
        assertEquals(emptyList<Reservation>(), s.repo.items.value)
    }
}
