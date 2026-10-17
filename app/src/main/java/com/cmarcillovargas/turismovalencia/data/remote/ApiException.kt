package com.cmarcillovargas.turismovalencia.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import retrofit2.Response
import java.io.IOException

/**
 * No se pudo obtener algo de la API: sin red o tiempo agotado ([status] nulo), error del servidor o respuesta que la app no entiende.
 * Con respuesta del servidor llevan el estado HTTP, el `error.code` del cuerpo (`sold_out`, `invalid_slot`…; la app decide qué
 * enseñar según el código, no según el mensaje) y, si es un 429, los segundos de `Retry-After`.
 */
class ApiException(
    message: String,
    cause: Throwable? = null,
    val status: Int? = null,
    val code: String? = null,
    val retryAfterSeconds: Int? = null,
) : Exception(message, cause)

/** El error con la forma de la API: `{"error": {"code": "sold_out", "message": "…"}}`. */
@Serializable
internal data class ApiErrorEnvelope(val error: ApiErrorBody? = null)

@Serializable
internal data class ApiErrorBody(val code: String = "", val message: String = "")

/** Una respuesta que no es 2xx ni 304, como [ApiException] con su estado, su código y su `Retry-After`. */
internal fun Response<*>.toApiException(): ApiException {
    val raw = try {
        errorBody()?.string().orEmpty()
    } catch (_: IOException) {
        ""
    }
    val code = try {
        TurismoApiClient.Json.decodeFromString<ApiErrorEnvelope>(raw).error?.code?.takeIf { it.isNotBlank() }
    } catch (_: SerializationException) {
        null
    }
    return ApiException(
        message = "El servidor respondió ${code()}" + (code?.let { " ($it)" } ?: ""),
        status = code(),
        code = code,
        retryAfterSeconds = headers()["Retry-After"]?.trim()?.toIntOrNull(),
    )
}

/** Ejecuta una llamada a la API: todo lo que no sea una [ApiException] (sin red, JSON que no es el del contrato…) pasa a serlo, sin estado HTTP. */
internal suspend inline fun <T> guardedApiCall(what: String, block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    throw e
} catch (e: Exception) {
    throw ApiException("No se pudo $what", e)
}
