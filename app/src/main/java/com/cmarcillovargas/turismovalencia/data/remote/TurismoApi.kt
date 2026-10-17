package com.cmarcillovargas.turismovalencia.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
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
}

@Serializable
data class OffersResponse(val offers: List<OfferDto> = emptyList())

/** Una oferta tal como la sirve la API. El dinero va en céntimos y `kind` es `entrada`, `tour` o `experiencia`. */
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
