package com.cmarcillovargas.turismovalencia.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Las reservas por HTTP contra un servidor falso, con los ejemplos de `backend/contract/examples/` como respuestas y como cuerpo. */
class ReservationsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var service: ReservationsService
    private val device = "5b1e0c3a-6c1d-4f0b-9a52-0c2c8d6a7e11"
    private val key = "9d2f5b1a-3c64-4e2b-8a77-1f0a5d9c2b34"
    private val request = Json.decodeFromString<ReservationRequest>(contractExample("reservation-request.json"))

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
        RetrofitReservationsService(TurismoApiClient.create(baseUrl, OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build()))

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
    fun `crea la reserva con las cabeceras y el cuerpo del contrato`() {
        server.enqueue(MockResponse().setResponseCode(201).setBody(contractExample("reservation.json")))

        val created = runBlocking { service.create(device, key, request) }

        val sent = server.takeRequest()
        assertEquals("POST", sent.method)
        assertEquals("/reservations", sent.path)
        assertEquals(device, sent.getHeader("X-Device-Id"))
        assertEquals(key, sent.getHeader("Idempotency-Key"))
        assertTrue(sent.getHeader("Content-Type").orEmpty().startsWith("application/json"))
        // El cuerpo es exactamente el del ejemplo del contrato (mismas claves y valores).
        assertEquals(Json.parseToJsonElement(contractExample("reservation-request.json")) as JsonObject, Json.parseToJsonElement(sent.body.readUtf8()) as JsonObject)
        assertFalse(created.replayed)
        with(created.reservation) {
            assertEquals("TV-2027-1100", locator)
            assertEquals("albufera-barca", offerId)
            assertEquals("2027-02-11", date)
            assertEquals(3600, totalCents)
            assertEquals("confirmed", status)
            assertEquals("2026-10-14T17:04:06Z", createdAt)
        }
    }

    @Test
    fun `una clave ya usada devuelve la misma reserva y lo dice`() {
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Idempotent-Replayed", "true").setBody(contractExample("reservation.json")))

        val created = runBlocking { service.create(device, key, request) }

        assertTrue(created.replayed)
        assertEquals("TV-2027-1100", created.reservation.locator)
    }

    @Test
    fun `lee las reservas del dispositivo`() {
        server.enqueue(MockResponse().setBody(contractExample("reservations-list.json")))

        val list = runBlocking { service.list(device) }

        val sent = server.takeRequest()
        assertEquals("GET", sent.method)
        assertEquals("/reservations", sent.path)
        assertEquals(device, sent.getHeader("X-Device-Id"))
        assertEquals(listOf("TV-2027-1100"), list.map { it.locator })
    }

    @Test
    fun `sin plazas es un 409 con su codigo`() {
        server.enqueue(MockResponse().setResponseCode(409).setBody(contractExample("error-sold-out.json")))

        val error = failure { service.create(device, key, request) }

        assertEquals(409, error.status)
        assertEquals("sold_out", error.code)
        assertNull(error.retryAfterSeconds)
    }

    @Test
    fun `demasiadas peticiones trae el tiempo de espera`() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "120").setBody(contractExample("error-rate-limited.json")))

        val error = failure { service.create(device, key, request) }

        assertEquals(429, error.status)
        assertEquals("rate_limited", error.code)
        assertEquals(120, error.retryAfterSeconds)
    }

    @Test
    fun `los datos incorrectos son un 400 con su codigo`() {
        server.enqueue(MockResponse().setResponseCode(400).setBody(contractExample("error-invalid-request.json")))

        val error = failure { service.create(device, key, request) }

        assertEquals(400, error.status)
        assertEquals("invalid_request", error.code)
    }

    @Test
    fun `un error sin cuerpo del contrato conserva el estado y no inventa el codigo`() {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"))
        val html = failure { service.create(device, key, request) }
        assertEquals(502, html.status)
        assertNull(html.code)

        server.enqueue(MockResponse().setResponseCode(500))
        val empty = failure { service.list(device) }
        assertEquals(500, empty.status)
        assertNull(empty.code)
    }

    @Test
    fun `sin servidor no hay estado HTTP`() {
        val address = server.url("/").toString()
        server.shutdown()

        val error = failure { serviceFor(address).create(device, key, request) }

        assertNull("sin respuesta no hay estado", error.status)
        assertNull(error.code)
    }

    @Test
    fun `una respuesta de exito que no es la reserva del contrato es un fallo sin estado`() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("esto no es JSON"))
        assertNull(failure { service.create(device, key, request) }.status)

        // Falta un dato obligatorio (el localizador): se rechaza en vez de inventarlo.
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"x","offerId":"albufera-barca","date":"2027-02-11","time":"18:30","adults":2,"totalCents":3600}"""))
        assertNull(failure { service.create(device, key, request) }.status)
    }

    @Test
    fun `cancela la reserva con el movil en la cabecera y el correo en el cuerpo del contrato`() {
        server.enqueue(MockResponse().setBody(contractExample("reservation-cancelled.json")))

        val cancelled = runBlocking { service.cancel(device, "TV-2027-1100", "laura.garcia@email.com") }

        val sent = server.takeRequest()
        assertEquals("POST", sent.method)
        assertEquals("/reservations/TV-2027-1100/cancel", sent.path)
        assertEquals(device, sent.getHeader("X-Device-Id"))
        assertTrue(sent.getHeader("Content-Type").orEmpty().startsWith("application/json"))
        assertEquals(Json.parseToJsonElement(contractExample("cancel-request.json")) as JsonObject, Json.parseToJsonElement(sent.body.readUtf8()) as JsonObject)
        assertEquals("cancelled", cancelled.status)
        assertEquals("TV-2027-1100", cancelled.locator)
    }

    @Test
    fun `un localizador raro no se cuela en la ruta`() {
        server.enqueue(MockResponse().setBody(contractExample("reservation-cancelled.json")))

        runBlocking { service.cancel(device, "a/b c", "x@example.com") }

        assertEquals("/reservations/a%2Fb%20c/cancel", server.takeRequest().path)
    }

    @Test
    fun `cancelar una reserva que no es del movil ni del correo es un 404 con su codigo`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody(contractExample("error-reservation-not-found.json")))

        val error = failure { service.cancel(device, "TV-2027-1100", "otro@example.com") }

        assertEquals(404, error.status)
        assertEquals("reservation_not_found", error.code)
    }

    @Test
    fun `cancelar una actividad que ya empezo es un 409 con su codigo`() {
        server.enqueue(MockResponse().setResponseCode(409).setBody(contractExample("error-reservation-past.json")))

        val error = failure { service.cancel(device, "TV-2027-1100", "laura.garcia@email.com") }

        assertEquals(409, error.status)
        assertEquals("reservation_past", error.code)
    }

    @Test
    fun `demasiadas cancelaciones trae el tiempo de espera`() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "300").setBody(contractExample("error-rate-limited.json")))

        val error = failure { service.cancel(device, "TV-2027-1100", "laura.garcia@email.com") }

        assertEquals(429, error.status)
        assertEquals(300, error.retryAfterSeconds)
    }

    @Test
    fun `recupera una reserva con el localizador en la ruta y el correo en la consulta`() {
        server.enqueue(MockResponse().setBody(contractExample("reservation.json")))

        val found = runBlocking { service.find("TV-2027-1100", "laura.garcia@email.com") }

        val sent = server.takeRequest()
        assertEquals("GET", sent.method)
        assertEquals(listOf("reservations", "TV-2027-1100"), sent.requestUrl!!.pathSegments)
        assertEquals("laura.garcia@email.com", sent.requestUrl!!.queryParameter("email"))
        assertNull("recuperar no manda el identificador del móvil", sent.getHeader("X-Device-Id"))
        assertEquals("TV-2027-1100", found.locator)
        assertEquals("confirmed", found.status)
    }

    @Test
    fun `un correo con un mas se manda codificado para que el servidor no lo lea como un espacio`() {
        server.enqueue(MockResponse().setBody(contractExample("reservation.json")))

        runBlocking { service.find("TV-2027-1100", "ana+valencia@example.com") }

        val sent = server.takeRequest()
        assertTrue(sent.path.orEmpty(), sent.path.orEmpty().contains("ana%2Bvalencia%40example.com"))
        assertEquals("ana+valencia@example.com", sent.requestUrl!!.queryParameter("email"))
    }

    @Test
    fun `recuperar con un localizador y un correo que no coinciden es un 404 con su codigo`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody(contractExample("error-reservation-not-found.json")))

        val error = failure { service.find("TV-2027-1100", "otro@example.com") }

        assertEquals(404, error.status)
        assertEquals("reservation_not_found", error.code)
    }

    @Test
    fun `sin servidor no se puede cancelar ni recuperar`() {
        val address = server.url("/").toString()
        server.shutdown()

        assertNull(failure { serviceFor(address).cancel(device, "TV-2027-1100", "x@example.com") }.status)
        assertNull(failure { serviceFor(address).find("TV-2027-1100", "x@example.com") }.status)
    }

    @Test
    fun `ignora los campos que no conoce de la reserva`() {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":"x","locator":"TV-2027-1","offerId":"o","date":"2027-02-11","time":"10:00","adults":1,"totalCents":100,"campoNuevo":[1,2]}""",
            ),
        )

        val created = runBlocking { service.create(device, key, request) }

        assertEquals("TV-2027-1", created.reservation.locator)
        assertEquals("confirmed", created.reservation.status)
        assertEquals(0, created.reservation.children)
    }
}
