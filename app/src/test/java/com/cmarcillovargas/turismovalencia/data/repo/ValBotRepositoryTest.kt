package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.remote.ChatException
import com.cmarcillovargas.turismovalencia.data.remote.ChatService
import com.cmarcillovargas.turismovalencia.data.remote.ChatTurn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRules : ValBotRules {
    override val greeting = "Hola"
    override val suggestions = listOf("¿Qué ver?")
    override fun reply(question: String) = BotReply("regla: $question", listOf("a"))
    override fun decorate(text: String) = BotReply(text, listOf("mercado-central"))
}

private class FakeChat(private val answer: () -> String) : ChatService {
    var received: Pair<String, List<ChatTurn>>? = null
    var language: LanguageCode? = null
    override suspend fun ask(message: String, history: List<ChatTurn>, language: LanguageCode): String {
        received = message to history
        this.language = language
        return answer()
    }
}

class ValBotRepositoryTest {
    @Test
    fun `usa la respuesta en linea y le anade las fichas`() = runBlocking {
        val repo = ValBotRepository(FakeChat { "Visita el Mercado Central" }, FakeRules())
        val reply = repo.reply("¿Qué ver?", emptyList())
        assertEquals("Visita el Mercado Central", reply.text)
        assertEquals(listOf("mercado-central"), reply.cardIds)
        assertFalse(reply.basic)
    }

    @Test
    fun `si el servidor falla contesta con las reglas y lo marca como basica`() = runBlocking {
        val repo = ValBotRepository(FakeChat { throw ChatException("404") }, FakeRules())
        val reply = repo.reply("¿Qué ver?", emptyList())
        assertEquals("regla: ¿Qué ver?", reply.text)
        assertEquals(listOf("a"), reply.cardIds)
        assertTrue(reply.basic)
    }

    @Test
    fun `solo envia los ultimos 10 turnos`() = runBlocking {
        val chat = FakeChat { "ok" }
        val history = (1..14).map { ChatTurn(it % 2 == 1, "m$it") }
        ValBotRepository(chat, FakeRules()).reply("pregunta", history)
        val (message, sent) = chat.received!!
        assertEquals("pregunta", message)
        assertEquals(10, sent.size)
        assertEquals("m5", sent.first().text)
        assertEquals("m14", sent.last().text)
    }

    @Test
    fun `le pide la respuesta en el idioma de la app`() = runBlocking {
        val chat = FakeChat { "ok" }
        var language = LanguageCode.EN
        val repo = ValBotRepository(chat, FakeRules(), language = { language })
        repo.reply("hello", emptyList())
        assertEquals(LanguageCode.EN, chat.language)
        language = LanguageCode.VA
        repo.reply("hola", emptyList())
        assertEquals(LanguageCode.VA, chat.language)
    }

    @Test
    fun `se puede cambiar el asistente en linea`() = runBlocking {
        val repo = ValBotRepository(FakeChat { throw ChatException("x") }, FakeRules())
        repo.useChat(FakeChat { "ahora sí" })
        assertEquals("ahora sí", repo.reply("hola", emptyList()).text)
    }
}

class FindMentionsTest {
    private val names = listOf(
        "mercado-central" to "Mercado Central",
        "lonja" to "Lonja de la Seda",
        "marina" to "La Marina",
        "oceanografic" to "L'Oceanogràfic",
        "corto" to "Moda",
        "paella" to "Paella valenciana",
    )

    @Test
    fun `devuelve los ids en el orden en que aparecen`() {
        val text = "Empieza por la Lonja de la Seda y acaba en el mercado central."
        assertEquals(listOf("lonja", "mercado-central"), findMentions(text, names, 5))
    }

    @Test
    fun `ignora acentos, mayusculas y el articulo del titulo`() {
        assertEquals(listOf("marina", "oceanografic"), findMentions("PASEA POR MARINA y visita el oceanografic", names, 5))
    }

    @Test
    fun `respeta el limite y descarta titulos cortos`() {
        val text = "Moda, Mercado Central, Lonja de la Seda y paella valenciana"
        assertEquals(listOf("mercado-central", "lonja"), findMentions(text, names, 2))
    }

    @Test
    fun `sin menciones no devuelve nada`() {
        assertTrue(findMentions("Hace buen tiempo", names, 2).isEmpty())
    }
}
