package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.remote.ChatException
import com.cmarcillovargas.turismovalencia.data.remote.ChatService
import com.cmarcillovargas.turismovalencia.data.remote.ChatTurn

/**
 * ValBot: pregunta primero al asistente en línea (función `chat` de la web, Gemini) y, si no responde
 * (sin red, tiempo agotado o error del servidor), contesta con las reglas de la app.
 */
class ValBotRepository(private var chat: ChatService, private val rules: ValBotRules) {
    val greeting: String get() = rules.greeting
    val suggestions: List<String> get() = rules.suggestions

    /** Solo depuración: cambia el asistente en línea (por ejemplo, por uno de ejemplo). */
    fun useChat(service: ChatService) {
        chat = service
    }

    /** [history] son los turnos anteriores a [question]; se envían como máximo los últimos [HistoryTurns]. */
    suspend fun reply(question: String, history: List<ChatTurn>): BotReply =
        try {
            rules.decorate(chat.ask(question, history.takeLast(HistoryTurns)))
        } catch (_: ChatException) {
            rules.reply(question).copy(basic = true)
        }

    companion object {
        /** Los mismos 10 mensajes de contexto que envía la web. */
        const val HistoryTurns = 10
    }
}
