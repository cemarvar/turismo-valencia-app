package com.cmarcillovargas.turismovalencia.data.remote

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ChatApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ChatApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = ChatApi(OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build(), server.url("/api/chat").toString())
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun failure(block: suspend () -> Unit): ChatException {
        try {
            runBlocking { block() }
        } catch (e: ChatException) {
            return e
        }
        fail("Debía lanzar ChatException")
        error("inalcanzable")
    }

    @Test
    fun `envia el mensaje y el historial como la web y devuelve la respuesta`() {
        server.enqueue(MockResponse().setBody("""{"reply":"  ¡Hola! Prueba la paella.  "}"""))
        val history = listOf(ChatTurn(true, "Hola"), ChatTurn(false, "¡Hola!"))

        val reply = runBlocking { api.ask("¿Dónde como paella?", history) }

        assertEquals("¡Hola! Prueba la paella.", reply)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/chat", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("¿Dónde como paella?", body.getString("message"))
        val turns = body.getJSONArray("history")
        assertEquals(2, turns.length())
        assertEquals("user", turns.getJSONObject(0).getString("role"))
        assertEquals("Hola", turns.getJSONObject(0).getString("text"))
        assertEquals("assistant", turns.getJSONObject(1).getString("role"))
        // Sin idioma pedido, el de la app por defecto (español).
        assertEquals("es", body.getString("lang"))
    }

    @Test
    fun `manda el idioma de la app para que el servidor conteste en el`() {
        server.enqueue(MockResponse().setBody("""{"reply":"Hello!"}"""))
        runBlocking { api.ask("hello", emptyList(), LanguageCode.VA) }
        assertEquals("ca-ES-valencia", JSONObject(server.takeRequest().body.readUtf8()).getString("lang"))
    }

    @Test
    fun `error del servidor con mensaje de la funcion`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"modelo no disponible"}"""))
        val error = failure { api.ask("Hola", emptyList()) }
        assertTrue(error.message!!.contains("404"))
        assertTrue(error.message!!.contains("modelo no disponible"))
    }

    @Test
    fun `error 502 sin cuerpo JSON`() {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"))
        val error = failure { api.ask("Hola", emptyList()) }
        assertTrue(error.message!!.contains("502"))
    }

    @Test
    fun `respuesta 200 sin reply, vacia o que no es JSON`() {
        server.enqueue(MockResponse().setBody("""{"reply":"   "}"""))
        server.enqueue(MockResponse().setBody("""{}"""))
        server.enqueue(MockResponse().setBody("no es json"))
        repeat(3) { failure { api.ask("Hola", emptyList()) } }
    }

    @Test
    fun `servidor que no responde agota el tiempo`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        failure { api.ask("Hola", emptyList()) }
    }

    @Test
    fun `sin servidor`() {
        server.shutdown()
        failure { api.ask("Hola", emptyList()) }
    }
}
