package com.cmarcillovargas.turismovalencia.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OffersApiTest {
    private lateinit var server: MockWebServer
    private lateinit var service: OffersService

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        service = serviceFor(server.url("/").toString())
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun serviceFor(baseUrl: String) =
        RetrofitOffersService(TurismoApiClient.create(baseUrl, OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build()))

    private fun failure(block: suspend () -> Unit): ApiException {
        try {
            runBlocking { block() }
        } catch (e: ApiException) {
            return e
        }
        fail("Debía lanzar ApiException")
        error("inalcanzable")
    }

    @Test
    fun `lee el catalogo del contrato y devuelve su ETag`() {
        server.enqueue(MockResponse().setBody(contractExample("offers-list.json")).setHeader("ETag", "W/\"1792228538411749-16\""))

        val fetched = runBlocking { service.fetch(null) } as OffersFetch.Fresh

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/offers", request.path)
        assertNull("sin ETag guardado no se pide nada condicional", request.getHeader("If-None-Match"))
        assertEquals("W/\"1792228538411749-16\"", fetched.etag)
        assertEquals(listOf("albufera-barca", "tourist-card-72", "entrada-oceanografic"), fetched.offers.map { it.id })
        val barca = fetched.offers.first { it.id == "albufera-barca" }
        assertEquals("experiencia", barca.kind)
        assertEquals("Paseo en barca por la Albufera", barca.title)
        assertEquals(1800, barca.priceFromCents)
        assertEquals("albufera-barca", barca.placeId)
        assertEquals(listOf("16:30", "17:30", "18:30", "19:30"), barca.slots)
        // Una oferta sin lugar enlazado llega con placeId nulo.
        assertNull(fetched.offers.first { it.id == "tourist-card-72" }.placeId)
        // Las tildes y la eñe llegan bien (UTF-8).
        assertTrue(fetched.offers.any { it.tag == "Excursión" && it.extra == "español/inglés" })
    }

    @Test
    fun `envia If-None-Match tal como lo recibio y entiende el 304`() {
        server.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "W/\"abc\""))

        val fetched = runBlocking { service.fetch("W/\"abc\"") }

        assertEquals(OffersFetch.NotModified, fetched)
        assertEquals("W/\"abc\"", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun `ignora los campos que no conoce y rellena los que faltan`() {
        server.enqueue(
            MockResponse().setBody(
                """{"offers":[{"id":"nueva","kind":"tour","title":"Tour nuevo","priceFromCents":1250,"campoNuevo":{"a":1}}],"version":"x"}""",
            ),
        )

        val fetched = runBlocking { service.fetch(null) } as OffersFetch.Fresh

        val offer = fetched.offers.single()
        assertEquals("nueva", offer.id)
        assertEquals(1250, offer.priceFromCents)
        assertEquals("", offer.place)
        assertEquals(emptyList<String>(), offer.slots)
        assertNull(offer.placeId)
        assertNull(fetched.etag)
    }

    @Test
    fun `un error del servidor es una ApiException`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":{"code":"internal","message":"boom"}}"""))

        val error = failure { service.fetch(null) }

        assertTrue(error.message.orEmpty().contains("500"))
    }

    @Test
    fun `una respuesta que no es JSON del contrato es una ApiException`() {
        server.enqueue(MockResponse().setBody("<html>Bad gateway</html>"))
        failure { service.fetch(null) }

        server.enqueue(MockResponse().setBody("""{"offers":"no es una lista"}"""))
        failure { service.fetch(null) }

        // Falta un dato obligatorio (el precio): se rechaza en vez de inventarlo.
        server.enqueue(MockResponse().setBody("""{"offers":[{"id":"x","kind":"tour","title":"Sin precio"}]}"""))
        failure { service.fetch(null) }
    }

    @Test
    fun `sin servidor es una ApiException`() {
        val address = server.url("/").toString()
        server.shutdown()

        failure { serviceFor(address).fetch(null) }
    }

    @Test
    fun `la direccion base se normaliza para Retrofit`() {
        server.enqueue(MockResponse().setBody("""{"offers":[]}"""))
        val withoutSlash = serviceFor(server.url("/functions/v1/api-v1").toString().trimEnd('/'))

        runBlocking { withoutSlash.fetch(null) }

        assertEquals("/functions/v1/api-v1/offers", server.takeRequest().path)
    }
}
