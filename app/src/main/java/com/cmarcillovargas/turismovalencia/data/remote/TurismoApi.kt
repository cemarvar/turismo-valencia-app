package com.cmarcillovargas.turismovalencia.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * API propia de la app: la función `api-v1` de Supabase (descrita en `docs/api.md`), la única puerta a la base de datos. No
 * lleva ninguna clave: la app solo conoce su dirección (`BuildConfig.API_BASE_URL`). Las respuestas siguen los ejemplos de
 * `backend/contract/examples/`, que las pruebas de la app leen tal cual.
 */
interface TurismoApi {
    /** Catálogo de ofertas. Con `If-None-Match` igual al `ETag` recibido antes responde 304 sin cuerpo. */
    @GET("offers")
    suspend fun offers(@Header("If-None-Match") etag: String?): Response<OffersResponse>

    /**
     * Crea una reserva (201; 200 con `Idempotent-Replayed: true` si la clave ya se había usado con los mismos datos). El servidor pone
     * el precio y el localizador. Sin cuentas: `X-Device-Id` es el UUID de este móvil e `Idempotency-Key` el de este intento.
     */
    @POST("reservations")
    suspend fun createReservation(
        @Header("X-Device-Id") deviceId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ReservationRequest,
    ): Response<ReservationDto>

    /** Las reservas de este móvil (hasta 100, las más recientes primero). */
    @GET("reservations")
    suspend fun reservations(@Header("X-Device-Id") deviceId: String): Response<ReservationsResponse>

    /**
     * Una reserva por localizador y correo (sirve para recuperarla en otro móvil). 404 `reservation_not_found` si no coinciden. Con
     * `X-Device-Id` (el identificador de quien la recupera) el servidor la apunta a ese móvil y su lista de reservas la trae desde entonces.
     */
    @GET("reservations/{locator}")
    suspend fun reservation(@Path("locator") locator: String, @Query("email") email: String, @Header("X-Device-Id") deviceId: String?): Response<ReservationDto>

    /**
     * Cancela una reserva: la hizo este móvil (`X-Device-Id`) o se conocen su localizador y su correo (`request.email`). 200 con la reserva ya
     * cancelada, también si ya lo estaba. 404 `reservation_not_found` si no es de quien lo pide y 409 `reservation_past` si la actividad ya empezó.
     */
    @POST("reservations/{locator}/cancel")
    suspend fun cancelReservation(
        @Path("locator") locator: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: CancelRequest,
    ): Response<ReservationDto>
}

/** Cuerpo de `POST /reservations/{locator}/cancel` (ver `backend/contract/examples/cancel-request.json`). Sin `email` solo vale el móvil que reservó. */
@Serializable
data class CancelRequest(val email: String? = null)

/** Cuerpo de `POST /reservations` (ver `backend/contract/examples/reservation-request.json`). `date` es `AAAA-MM-DD`; el correo, en cualquier caso. */
@Serializable
data class ReservationRequest(
    val offerId: String,
    val date: String,
    val time: String,
    val adults: Int,
    val children: Int,
    val email: String,
)

/** Una reserva tal como la sirve la API. `status` es `confirmed` o `cancelled`; el dinero va en céntimos; `createdAt`, en ISO-8601 UTC. */
@Serializable
data class ReservationDto(
    val id: String,
    val locator: String,
    val offerId: String,
    val date: String,
    val time: String,
    val adults: Int,
    val totalCents: Int,
    val title: String = "",
    val place: String = "",
    val children: Int = 0,
    val email: String = "",
    val status: String = "confirmed",
    val createdAt: String = "",
)

@Serializable
data class ReservationsResponse(val reservations: List<ReservationDto> = emptyList())

@Serializable
data class OffersResponse(val offers: List<OfferDto> = emptyList())

/**
 * Una oferta tal como la sirve la API. El dinero va en céntimos y `kind` es `entrada`, `tour` o `experiencia`. Los textos de arriba son
 * los originales (español); `translations` trae los de otros idiomas por código de idioma (`va`, `en`, `fr`), con solo los campos que
 * están traducidos.
 */
@Serializable
data class OfferDto(
    val id: String,
    val kind: String,
    val title: String,
    val priceFromCents: Int,
    val tag: String = "",
    val duration: String = "",
    val place: String = "",
    val extra: String = "",
    val description: String = "",
    val placeId: String? = null,
    val slots: List<String> = emptyList(),
    val translations: Map<String, OfferTranslationDto> = emptyMap(),
)

/** Los textos de una oferta en un idioma; lo que no viene se ve en el idioma original. */
@Serializable
data class OfferTranslationDto(
    val tag: String? = null,
    val title: String? = null,
    val duration: String? = null,
    val place: String? = null,
    val extra: String? = null,
    val description: String? = null,
)

/** Cliente de la API: Retrofit sobre el `OkHttpClient` de la app, con JSON de kotlinx.serialization. */
object TurismoApiClient {
    /** La API puede añadir campos sin cambiar de versión (`api-v1`): los que la app no conoce se ignoran. */
    val Json = Json { ignoreUnknownKeys = true }

    fun create(baseUrl: String, client: OkHttpClient = defaultClient()): TurismoApi =
        Retrofit.Builder()
            // Retrofit exige que la dirección base termine en «/».
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()
            .create(TurismoApi::class.java)

    /** Tiempos de la API; parte de [base] para compartir con el resto de la app el mismo grupo de conexiones. */
    fun defaultClient(base: OkHttpClient = OkHttpClient()): OkHttpClient = base.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
}
