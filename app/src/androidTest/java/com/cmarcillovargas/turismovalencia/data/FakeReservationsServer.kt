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
 * localizador y total que pone él, idempotencia por clave, error con código) y apunta todo lo que le llega. Con [priceCents] da el
 * precio por adulto de cada oferta.
 */
class FakeReservationsServer(private val priceCents: (offerId: String) -> Int = { 1800 }) : ReservationsService {
    data class Call(val deviceId: String, val idempotencyKey: String, val request: ReservationRequest)

    /** Todas las peticiones de crear que le han llegado, en orden. */
    val calls = mutableListOf<Call>()

    /** Reservas que guardó, por clave de idempotencia. */
    private val byKey = LinkedHashMap<String, ReservationDto>()

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
        ).also { byKey[idempotencyKey] = it }
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
        return forcedList ?: byKey.values.toList().asReversed()
    }

    companion object {
        fun error(status: Int, code: String, retryAfterSeconds: Int? = null) =
            ApiException("El servidor respondió $status ($code)", status = status, code = code, retryAfterSeconds = retryAfterSeconds)
    }
}
