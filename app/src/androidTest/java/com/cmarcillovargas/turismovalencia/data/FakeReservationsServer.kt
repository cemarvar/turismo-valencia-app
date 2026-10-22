package com.cmarcillovargas.turismovalencia.data

import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.CreatedReservation
import com.cmarcillovargas.turismovalencia.data.remote.ReservationDto
import com.cmarcillovargas.turismovalencia.data.remote.ReservationRequest
import com.cmarcillovargas.turismovalencia.data.remote.ReservationsService
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Servidor de reservas falso para las pruebas del emulador: se comporta como la API real en lo que importa a la app (reserva con
 * localizador y total que pone él, idempotencia por clave, cancelar y recuperar con las mismas reglas de quién puede, error con
 * código) y apunta todo lo que le llega. Con [priceCents] da el precio por adulto de cada oferta.
 */
class FakeReservationsServer(private val priceCents: (offerId: String) -> Int = { 1800 }) : ReservationsService {
    data class Call(val deviceId: String, val idempotencyKey: String, val request: ReservationRequest)

    /** Todas las peticiones de crear que le han llegado, en orden. */
    val calls = mutableListOf<Call>()

    /** Reservas que guardó, por clave de idempotencia. */
    private val byKey = LinkedHashMap<String, ReservationDto>()

    /** Qué móvil hizo cada reserva (por el identificador de la reserva). */
    private val deviceOf = HashMap<String, String>()

    /** Los móviles que han recuperado cada reserva (por el identificador de la reserva): `list` también se las trae. */
    private val linked = HashMap<String, MutableSet<String>>()

    /** Cancelaciones que le han llegado: móvil, localizador y correo, en orden. */
    data class CancelCall(val deviceId: String, val locator: String, val email: String)

    val cancelCalls = mutableListOf<CancelCall>()

    /** Búsquedas por localizador y correo que le han llegado, en orden, con el identificador del móvil que las mandó (si lo mandó). */
    val findCalls = mutableListOf<Triple<String, String, String?>>()

    /** El día de hoy para decidir si una actividad ya pasó (en las pruebas se puede fijar). */
    var today: () -> LocalDate = { LocalDate.now() }

    /** Fallos que lanza, uno por petición y sin guardar nada (por ejemplo, `409 sold_out`). Se consumen en orden. */
    val failures = ArrayDeque<ApiException>()

    /** Cuántas de las próximas peticiones **sí guardan** la reserva y luego pierden la respuesta (se corta la red). */
    var loseNextResponses = 0

    var nextNumber = 1000

    /** Lo que tarda en responder (para probar que una reserva ya confirmada no se pierde si quien la pide se va). */
    var delayMillis = 0L

    /** Si no es `null`, la reserva que devuelve (guardada o no) es esta en vez de la que corresponde (un servidor que responde algo ilegible). */
    var forcedReservation: ReservationDto? = null

    /** Reservas guardadas, de la más antigua a la más reciente. */
    val stored: List<ReservationDto> get() = byKey.values.toList()

    override suspend fun create(deviceId: String, idempotencyKey: String, request: ReservationRequest): CreatedReservation {
        calls += Call(deviceId, idempotencyKey, request)
        if (delayMillis > 0) delay(delayMillis)
        failures.removeFirstOrNull()?.let { throw it }
        forcedReservation?.let { return CreatedReservation(it, replayed = false) }
        val existing = byKey[idempotencyKey]
        val reservation = existing ?: ReservationDto(
            id = UUID.randomUUID().toString(),
            locator = "TV-${LocalDate.parse(request.date).year}-${nextNumber++}",
            offerId = request.offerId,
            date = request.date,
            time = request.time,
            adults = request.adults,
            totalCents = priceCents(request.offerId) * request.adults,
            title = "Oferta ${request.offerId}",
            place = "Valencia",
            children = request.children,
            email = request.email.lowercase(),
            status = "confirmed",
            createdAt = Instant.now().toString(),
        ).also {
            byKey[idempotencyKey] = it
            deviceOf[it.id] = deviceId
        }
        if (loseNextResponses > 0) {
            loseNextResponses--
            throw ApiException("No se pudo crear la reserva", RuntimeException("se cortó la red"))
        }
        return CreatedReservation(reservation, replayed = existing != null)
    }

    /** Si no es `null`, es lo que devuelve la lista en vez de lo guardado (por ejemplo, una reserva que el servidor canceló). */
    var forcedList: List<ReservationDto>? = null

    /** Los identificadores de móvil que han pedido la lista, en orden. */
    val listCalls = mutableListOf<String>()

    override suspend fun list(deviceId: String): List<ReservationDto> {
        listCalls += deviceId
        // Las suyas y las que ha recuperado, las más recientes primero (como el servidor).
        return forcedList ?: byKey.values.filter { deviceOf[it.id] == deviceId || deviceId in linked[it.id].orEmpty() }.asReversed()
    }

    private fun keyOf(locator: String): String? = byKey.entries.firstOrNull { it.value.locator.equals(locator.trim(), ignoreCase = true) }?.key

    private fun notFound() = error(404, "reservation_not_found")

    override suspend fun cancel(deviceId: String, locator: String, email: String): ReservationDto {
        cancelCalls += CancelCall(deviceId, locator, email)
        if (delayMillis > 0) delay(delayMillis)
        failures.removeFirstOrNull()?.let { throw it }
        forcedReservation?.let { return it }
        val key = keyOf(locator) ?: throw notFound()
        val reservation = byKey.getValue(key)
        // Como el servidor: la cancela el móvil que la hizo o quien tenga el localizador y el correo; si no, «no existe».
        val owner = deviceOf[reservation.id] == deviceId || (email.isNotBlank() && reservation.email.equals(email.trim(), ignoreCase = true))
        if (!owner) throw notFound()
        if (reservation.status == "cancelled") return reservation
        if (LocalDate.parse(reservation.date).isBefore(today())) throw error(409, "reservation_past")
        val cancelled = reservation.copy(status = "cancelled")
        byKey[key] = cancelled
        if (loseNextResponses > 0) {
            loseNextResponses--
            throw ApiException("No se pudo cancelar la reserva", RuntimeException("se cortó la red"))
        }
        return cancelled
    }

    override suspend fun find(locator: String, email: String, deviceId: String?): ReservationDto {
        findCalls += Triple(locator, email, deviceId)
        if (delayMillis > 0) delay(delayMillis)
        failures.removeFirstOrNull()?.let { throw it }
        forcedReservation?.let { return it }
        val reservation = keyOf(locator)?.let { byKey.getValue(it) } ?: throw notFound()
        if (!reservation.email.equals(email.trim(), ignoreCase = true)) throw notFound()
        if (deviceId != null && deviceOf[reservation.id] != deviceId) linked.getOrPut(reservation.id) { mutableSetOf() } += deviceId
        return reservation
    }

    companion object {
        fun error(status: Int, code: String, retryAfterSeconds: Int? = null) =
            ApiException("El servidor respondió $status ($code)", status = status, code = code, retryAfterSeconds = retryAfterSeconds)
    }
}
