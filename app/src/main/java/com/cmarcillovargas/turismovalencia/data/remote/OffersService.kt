package com.cmarcillovargas.turismovalencia.data.remote

import kotlinx.coroutines.CancellationException

/** No se pudo obtener algo de la API (sin red, tiempo agotado, error del servidor o respuesta que la app no entiende). */
class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Resultado de pedir el catálogo al servidor. */
sealed interface OffersFetch {
    /** El catálogo no ha cambiado desde el `ETag` que se envió (304). */
    data object NotModified : OffersFetch

    /** Catálogo recibido, con el `ETag` que identifica esa versión. */
    data class Fresh(val offers: List<OfferDto>, val etag: String?) : OffersFetch
}

/** Catálogo de ofertas del servidor. Es una interfaz para poder probar el repositorio sin red. */
interface OffersService {
    /** Pide el catálogo (con [etag], si lo hay, para recibir 304 cuando no ha cambiado) o lanza [ApiException]. */
    suspend fun fetch(etag: String?): OffersFetch
}

class RetrofitOffersService(private val api: TurismoApi) : OffersService {
    override suspend fun fetch(etag: String?): OffersFetch = try {
        val response = api.offers(etag)
        when {
            response.code() == 304 -> OffersFetch.NotModified
            response.isSuccessful -> {
                val body = response.body() ?: throw ApiException("El servidor respondió sin catálogo")
                OffersFetch.Fresh(body.offers, response.headers()["ETag"])
            }
            else -> throw ApiException("El servidor respondió ${response.code()}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: Exception) {
        // IOException (sin red, tiempo agotado) y SerializationException (JSON que no es el del contrato): se sigue con lo guardado.
        throw ApiException("No se pudo obtener el catálogo", e)
    }
}

/** Sin servidor: para las pruebas que no deben tocar la red y para la versión de la app que no tenga API. */
object NoOffersService : OffersService {
    override suspend fun fetch(etag: String?): OffersFetch = throw ApiException("Sin conexión con el servidor")
}
