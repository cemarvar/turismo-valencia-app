package com.cmarcillovargas.turismovalencia.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.CacheMetaEntity
import com.cmarcillovargas.turismovalencia.data.local.OfferDao
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.NoOffersService
import com.cmarcillovargas.turismovalencia.data.remote.OfferDto
import com.cmarcillovargas.turismovalencia.data.remote.OfferTranslationDto
import com.cmarcillovargas.turismovalencia.data.remote.OffersFetch
import com.cmarcillovargas.turismovalencia.data.remote.OffersService
import com.cmarcillovargas.turismovalencia.data.repo.OffersRefresh
import com.cmarcillovargas.turismovalencia.data.repo.OffersRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * El catálogo de ofertas del servidor y su caché en Room: sin nada guardado se usa el catálogo incluido en la app, lo que llega
 * del servidor lo sustituye y se guarda (con su `ETag`), un fallo o un catálogo inservible no tocan lo que ya había y todo
 * sobrevive a cerrar y abrir la base de datos. El servidor es falso: estas pruebas no tocan la red.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OffersTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val toClose = mutableListOf<() -> Unit>()

    @After
    fun tearDown() {
        toClose.forEach { it() }
    }

    /** Servidor falso: responde lo que se le encola y apunta los `ETag` que le llegan. */
    private class FakeService : OffersService {
        val sentEtags = mutableListOf<String?>()
        val results = ArrayDeque<suspend () -> OffersFetch>()

        override suspend fun fetch(etag: String?): OffersFetch {
            sentEtags += etag
            return results.removeFirst()()
        }
    }

    private inner class Session(val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()) {
        val scope = testScope()
        val service = FakeService()
        var clock = 1_000L
        val dao: OfferDao = db.offers()
        val repo = OffersRepository(dao, service, FakeOfertas, { "img/$it.jpg" }, scope, now = { clock })

        init {
            toClose += { scope.closeAndJoin(); db.close() }
        }

        fun serve(vararg offers: OfferDto, etag: String? = "W/\"v1\"") {
            service.results += { OffersFetch.Fresh(offers.toList(), etag) }
        }
    }

    private fun dto(id: String, kind: String = "tour", title: String = "Oferta $id", cents: Int = 1500, slots: List<String> = listOf("10:00")) =
        OfferDto(id = id, kind = kind, title = title, priceFromCents = cents, slots = slots)

    /** Room avisa de los cambios de forma asíncrona: espera a que el catálogo llegue al estado esperado. */
    private fun StateFlow<List<Offer>>.awaitIds(expected: List<String>) =
        runBlocking { withTimeout(5_000) { first { list -> list.map { it.id } == expected } } }

    @Test
    fun sinNadaGuardadoSeUsaElCatalogoIncluidoEnLaApp() {
        val s = Session()

        assertEquals(FakeOfertas.map { it.id }, s.repo.offers.value.map { it.id })
        // Las fotos de la web se buscan por id también en el catálogo incluido.
        assertEquals("img/albufera-barca.jpg", s.repo.offers.value.first { it.id == "albufera-barca" }.assetImage)
        assertEquals(0, runBlocking { s.dao.count() })
        assertNull(runBlocking { s.dao.meta(OfferDao.OffersCache) })
    }

    @Test
    fun elCatalogoDelServidorSustituyeAlIncluidoYSeGuardaConSuVersion() {
        val s = Session()
        s.serve(dto("albufera-barca", kind = "experiencia", title = "Paseo en barca", cents = 2050, slots = listOf("09:00", "10:00")), dto("oferta-nueva", cents = 990))

        val result = runBlocking { s.repo.refresh() }

        assertEquals(OffersRefresh.Updated(2), result)
        val offers = s.repo.offers.awaitIds(listOf("albufera-barca", "oferta-nueva"))
        val barca = offers[0]
        // Los datos son los del servidor…
        assertEquals("Paseo en barca", barca.title)
        assertEquals(2050, barca.priceFromCents)
        assertEquals(listOf("09:00", "10:00"), barca.times)
        // …y las fotos del diseño de las ofertas que la app ya conocía se conservan. La nueva sale sin ellas.
        assertNotNull(barca.thumb)
        assertNotNull(barca.ticket)
        assertEquals("img/albufera-barca.jpg", barca.assetImage)
        assertNull(offers[1].thumb)
        assertEquals(990, offers[1].priceFromCents)
        val meta = runBlocking { s.dao.meta(OfferDao.OffersCache) }!!
        assertEquals("W/\"v1\"", meta.etag)
        assertEquals(1_000L, meta.fetchedAt)
    }

    @Test
    fun lasTraduccionesDelServidorSeGuardanEnLaCacheYSeLeenPorIdioma() {
        val s = Session()
        s.serve(
            dto("oferta-nueva").copy(
                translations = mapOf(
                    "en" to OfferTranslationDto(title = "New offer", description = "A nice one"),
                    "de" to OfferTranslationDto(title = "Neues Angebot"), // un idioma que la app no tiene
                ),
            ),
            dto("sin-traducciones"),
        )

        runBlocking { s.repo.refresh() }

        val offers = s.repo.offers.awaitIds(listOf("oferta-nueva", "sin-traducciones"))
        // El original sigue en español y las traducciones viajan con la oferta, solo las que la app entiende y con lo que traen.
        assertEquals("Oferta oferta-nueva", offers[0].title)
        assertEquals(setOf(LanguageCode.EN), offers[0].serverTexts.keys)
        assertEquals("New offer", offers[0].serverTexts.getValue(LanguageCode.EN).title)
        assertEquals("A nice one", offers[0].serverTexts.getValue(LanguageCode.EN).description)
        assertNull(offers[0].serverTexts.getValue(LanguageCode.EN).place)
        assertTrue(offers[1].serverTexts.isEmpty())
    }

    @Test
    fun siNoHaCambiadoSeEnviaElETagYSeSigueConLoGuardado() {
        val s = Session()
        s.serve(dto("a"), dto("b"))
        runBlocking { s.repo.refresh() }
        s.repo.offers.awaitIds(listOf("a", "b"))

        s.clock = 2_000L
        s.service.results += { OffersFetch.NotModified }
        val result = runBlocking { s.repo.refresh() }

        assertEquals(OffersRefresh.NotModified, result)
        assertEquals(listOf(null, "W/\"v1\""), s.service.sentEtags)
        assertEquals(listOf("a", "b"), s.repo.offers.value.map { it.id })
        val meta = runBlocking { s.dao.meta(OfferDao.OffersCache) }!!
        assertEquals("W/\"v1\"", meta.etag)
        assertEquals(2_000L, meta.fetchedAt)
    }

    @Test
    fun unaVersionNuevaSustituyeAlCatalogoYGuardaSuETag() {
        val s = Session()
        s.serve(dto("a"), dto("b"), etag = "W/\"v1\"")
        runBlocking { s.repo.refresh() }
        s.repo.offers.awaitIds(listOf("a", "b"))

        s.serve(dto("b", title = "B cambiada"), dto("c"), etag = "W/\"v2\"")
        runBlocking { s.repo.refresh() }

        val offers = s.repo.offers.awaitIds(listOf("b", "c"))
        assertEquals("B cambiada", offers[0].title)
        assertEquals("W/\"v2\"", runBlocking { s.dao.meta(OfferDao.OffersCache) }!!.etag)
        assertEquals(2, runBlocking { s.dao.count() })
    }

    @Test
    fun unFalloNoTocaLoQueYaHabia() {
        val s = Session()
        s.serve(dto("a"))
        runBlocking { s.repo.refresh() }
        s.repo.offers.awaitIds(listOf("a"))

        s.clock = 9_000L
        s.service.results += { throw ApiException("sin red") }
        val result = runBlocking { s.repo.refresh() }

        assertTrue(result is OffersRefresh.Failed)
        assertEquals(listOf("a"), s.repo.offers.value.map { it.id })
        val meta = runBlocking { s.dao.meta(OfferDao.OffersCache) }!!
        assertEquals("W/\"v1\"", meta.etag)
        assertEquals("el fallo no cuenta como una comprobación", 1_000L, meta.fetchedAt)
    }

    @Test
    fun unCatalogoVacioOInservibleNoSustituyeAlQueHabia() {
        val s = Session()

        // Sin nada guardado: se sigue con el catálogo incluido en la app.
        s.service.results += { OffersFetch.Fresh(emptyList(), "W/\"x\"") }
        assertTrue(runBlocking { s.repo.refresh() } is OffersRefresh.Failed)
        s.service.results += { OffersFetch.Fresh(listOf(dto("raro", kind = "abono")), "W/\"x\"") }
        assertTrue(runBlocking { s.repo.refresh() } is OffersRefresh.Failed)
        assertEquals(FakeOfertas.map { it.id }, s.repo.offers.value.map { it.id })
        assertNull(runBlocking { s.dao.meta(OfferDao.OffersCache) })

        // Con un catálogo guardado: tampoco lo sustituyen.
        s.serve(dto("a"))
        runBlocking { s.repo.refresh() }
        s.repo.offers.awaitIds(listOf("a"))
        s.service.results += { OffersFetch.Fresh(emptyList(), "W/\"y\"") }
        assertTrue(runBlocking { s.repo.refresh() } is OffersRefresh.Failed)
        assertEquals(listOf("a"), s.repo.offers.value.map { it.id })
        assertEquals("W/\"v1\"", runBlocking { s.dao.meta(OfferDao.OffersCache) }!!.etag)
    }

    @Test
    fun lasOfertasInvalidasSeIgnoranYLasRepetidasQuedanUnaVez() {
        val s = Session()
        s.serve(dto("a", title = "La buena"), dto("raro", kind = "abono"), dto("a", title = "La repetida"), dto(" ", title = "Sin id"), dto("b"))

        val result = runBlocking { s.repo.refresh() }

        assertEquals(OffersRefresh.Updated(2), result)
        val offers = s.repo.offers.awaitIds(listOf("a", "b"))
        assertEquals("La buena", offers[0].title)
    }

    @Test
    fun sinCatalogoGuardadoNoSeEnviaElETagAunqueQuedeLaFilaDeVersion() {
        val s = Session()
        runBlocking { s.dao.saveMeta(CacheMetaEntity(OfferDao.OffersCache, "W/\"viejo\"", 5L)) }
        s.service.results += { OffersFetch.NotModified }

        val result = runBlocking { s.repo.refresh() }

        // Un 304 nos dejaría sin catálogo: no se pide con ETag y, si el servidor lo da por vigente, es un fallo.
        assertEquals(listOf<String?>(null), s.service.sentEtags)
        assertTrue(result is OffersRefresh.Failed)
        assertEquals(FakeOfertas.map { it.id }, s.repo.offers.value.map { it.id })
    }

    @Test
    fun vaciarLaCacheVuelveAlCatalogoIncluidoEnLaApp() {
        val s = Session()
        s.serve(dto("a"))
        runBlocking { s.repo.refresh() }
        s.repo.offers.awaitIds(listOf("a"))

        runBlocking { s.dao.clearCache() }

        s.repo.offers.awaitIds(FakeOfertas.map { it.id })
        assertNull(runBlocking { s.dao.meta(OfferDao.OffersCache) })
    }

    @Test
    fun lasPeticionesSimultaneasSeTurnan() {
        val s = Session()
        val running = AtomicInteger()
        val most = AtomicInteger()
        repeat(3) {
            s.service.results += {
                most.accumulateAndGet(running.incrementAndGet()) { a, b -> maxOf(a, b) }
                delay(60)
                running.decrementAndGet()
                OffersFetch.Fresh(listOf(dto("a")), "W/\"v1\"")
            }
        }

        runBlocking { (1..3).map { async(Dispatchers.Default) { s.repo.refresh() } }.awaitAll() }

        assertEquals("se pisarían al escribir en la caché", 1, most.get())
        s.repo.offers.awaitIds(listOf("a"))
    }

    @Test
    fun elCatalogoGuardadoSobreviveAReabrirLaBaseDeDatos() {
        val name = "offers-${System.nanoTime()}.db"
        try {
            fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val first = Session(open())
            first.serve(dto("a", cents = 2500, slots = listOf("Cualquier hora")), dto("b"), etag = "W/\"v7\"")
            runBlocking { first.repo.refresh() }
            first.repo.offers.awaitIds(listOf("a", "b"))
            first.scope.closeAndJoin()
            first.db.close()

            // Una apertura nueva, sin pedir nada al servidor: sigue el catálogo del servidor, no el de la app.
            val db = open()
            val scope = testScope()
            toClose += { scope.closeAndJoin(); db.close() }
            val repo = OffersRepository(db.offers(), NoOffersService, FakeOfertas, { null }, scope)
            assertEquals(listOf("a", "b"), repo.offers.value.map { it.id })
            assertEquals(2500, repo.offers.value[0].priceFromCents)
            assertEquals(listOf("Cualquier hora"), repo.offers.value[0].times)
            assertEquals("W/\"v7\"", runBlocking { db.offers().meta(OfferDao.OffersCache) }!!.etag)
        } finally {
            context.deleteDatabase(name)
        }
    }
}
