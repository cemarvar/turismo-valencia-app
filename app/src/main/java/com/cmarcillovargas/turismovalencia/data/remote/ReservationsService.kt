package com.cmarcillovargas.turismovalencia.data.remote

/** Una reserva recién creada. [replayed] es que el servidor ya la tenía (misma clave de idempotencia y mismos datos): no se duplicó. */
data class CreatedReservation(val reservation: ReservationDto, val replayed: Boolean)

/** Reservas en el servidor. Es una interfaz para poder probar el repositorio sin red. */
interface ReservationsService {
    /**
     * Crea la reserva (`POST /reservations`). [idempotencyKey] identifica **este intento**: reintentar con la misma clave y los mismos
     * datos devuelve la misma reserva en vez de crear otra. Lanza [ApiException] (con el código del error de la API, si lo hay).
     */
    suspend fun create(deviceId: String, idempotencyKey: String, request: ReservationRequest): CreatedReservation

    /** Las reservas de este móvil (`GET /reservations`), las más recientes primero. */
    suspend fun list(deviceId: String): List<ReservationDto>
}

class RetrofitReservationsService(private val api: TurismoApi) : ReservationsService {
    override suspend fun create(deviceId: String, idempotencyKey: String, request: ReservationRequest): CreatedReservation =
        guardedApiCall("crear la reserva") {
            val response = api.createReservation(deviceId, idempotencyKey, request)
            if (!response.isSuccessful) throw response.toApiException()
            val body = response.body() ?: throw ApiException("El servidor respondió sin reserva", status = response.code())
            CreatedReservation(body, replayed = response.headers()["Idempotent-Replayed"] == "true")
        }

    override suspend fun list(deviceId: String): List<ReservationDto> = guardedApiCall("leer las reservas") {
        val response = api.reservations(deviceId)
        if (!response.isSuccessful) throw response.toApiException()
        (response.body() ?: throw ApiException("El servidor respondió sin lista", status = response.code())).reservations
    }
}

/** Sin servidor: para las pruebas que no deben tocar la red. */
object NoReservationsService : ReservationsService {
    override suspend fun create(deviceId: String, idempotencyKey: String, request: ReservationRequest): CreatedReservation =
        throw ApiException("Sin conexión con el servidor")

    override suspend fun list(deviceId: String): List<ReservationDto> = throw ApiException("Sin conexión con el servidor")
}
