package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.local.OfferDao
import com.cmarcillovargas.turismovalencia.data.local.OfferEntity
import com.cmarcillovargas.turismovalencia.data.local.asState
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.OffersFetch
import com.cmarcillovargas.turismovalencia.data.remote.OffersService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resultado de pedir el catálogo de ofertas al servidor. */
sealed interface OffersRefresh {
    /** Había una versión nueva: la caché se ha sustituido por ella. */
    data class Updated(val count: Int) : OffersRefresh

    /** El servidor confirmó que la caché sigue vigente (304). */
    data object NotModified : OffersRefresh

    /** No se pudo (sin red, error del servidor o catálogo inservible): se sigue con lo que ya había. */
    data class Failed(val reason: Throwable) : OffersRefresh
}

/**
 * Catálogo de entradas, tours y experiencias, que manda el servidor. La app lo lee de la caché de Room y, mientras no haya
 * recibido ninguno (primer arranque sin conexión), usa el catálogo incluido en la app ([bundled], con las fotos del diseño).
 * [refresh] pide la versión vigente al servidor con el `ETag` guardado y, si cambió, sustituye la caché; la interfaz se actualiza
 * sola cuando Room avisa del cambio.
 *
 * Una oferta del servidor que la app no entiende (por ejemplo, un tipo nuevo) se ignora; un catálogo vacío no sustituye al que
 * ya había (sería un error del servidor, no que no haya ofertas).
 */
class OffersRepository(
    private val dao: OfferDao,
    private val service: OffersService,
    private val bundled: List<Offer>,
    private val assetImageOf: (offerId: String) -> String?,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val bundledById = bundled.associateBy { it.id }
    private val refreshing = Mutex()

    /** El catálogo vigente, en el orden del servidor. */
    val offers: StateFlow<List<Offer>> = dao.observeAll().map(::toOffers).asState(scope)

    private fun toOffers(rows: List<OfferEntity>): List<Offer> =
        if (rows.isEmpty()) {
            bundled.map { it.copy(assetImage = assetImageOf(it.id) ?: it.assetImage) }
        } else {
            rows.map { it.toModel(bundledById[it.id], assetImageOf(it.id)) }
        }

    /** Pide el catálogo al servidor. Las llamadas simultáneas se turnan para no pisarse al escribir en la caché. */
    suspend fun refresh(): OffersRefresh = refreshing.withLock {
        // Sin nada guardado no se envía el ETag: un 304 nos dejaría sin catálogo.
        val etag = if (dao.count() > 0) dao.meta(OfferDao.OffersCache)?.etag else null
        val fetched = try {
            service.fetch(etag)
        } catch (e: ApiException) {
            return@withLock OffersRefresh.Failed(e)
        }
        when (fetched) {
            OffersFetch.NotModified -> {
                if (etag == null) return@withLock OffersRefresh.Failed(ApiException("El servidor dio por vigente un catálogo que no hay"))
                dao.touch(OfferDao.OffersCache, now())
                OffersRefresh.NotModified
            }
            is OffersFetch.Fresh -> {
                val entities = fetched.offers.mapNotNull { it.toEntity(0) }.distinctBy { it.id }.mapIndexed { index, entity -> entity.copy(position = index) }
                if (entities.isEmpty()) return@withLock OffersRefresh.Failed(ApiException("El servidor envió un catálogo vacío"))
                dao.replace(entities, fetched.etag, now())
                OffersRefresh.Updated(entities.size)
            }
        }
    }
}
