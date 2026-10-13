package com.cmarcillovargas.turismovalencia.ui.screens.valbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ChatMessage
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.WebPageMeta
import com.cmarcillovargas.turismovalencia.data.remote.ChatTurn
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ValBotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val typing: Boolean = false,
    val online: Boolean = true,
    val input: String = "",
    val suggestions: List<String> = emptyList(),
    val items: Map<String, ContentItem> = emptyMap(),
    val pages: Map<String, WebPageMeta> = emptyMap(),
)

class ValBotViewModel(private val container: AppContainer) : ViewModel() {
    private var nextId = 0L
    private val messages = MutableStateFlow(listOf(botMessage(container.valbot.greeting)))
    private val typing = MutableStateFlow(false)
    private val input = MutableStateFlow("")
    private var pending: Job? = null

    val uiState: StateFlow<ValBotUiState> = combine(messages, typing, input, container.network.isOnline) { list, isTyping, text, online ->
        ValBotUiState(
            messages = list,
            typing = isTyping,
            online = online,
            input = text,
            suggestions = container.valbot.suggestions,
            items = container.content.all.associateBy { it.id },
            pages = container.web.index.pages,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ValBotUiState())

    fun onInputChange(value: String) {
        input.value = value.take(MaxInputLength)
    }

    /** Envía la pregunta escrita o una de las sugerencias. */
    fun send(text: String = input.value) {
        val question = text.trim()
        if (question.isEmpty() || typing.value || !container.network.isOnline.value) return
        input.value = ""
        val history = currentHistory()
        messages.update { it + ChatMessage(nextId++, true, question, now()) }
        pending = viewModelScope.launch {
            typing.value = true
            // Aunque el servidor conteste al instante, se ve «escribiendo…» un momento.
            val reply = coroutineScope {
                val answer = async { container.valbot.reply(question, history) }
                delay(MinTypingMillis)
                answer.await()
            }
            typing.value = false
            messages.update { it + ChatMessage(nextId++, false, reply.text, now(), reply.cardIds, reply.pageIds, basic = reply.basic) }
        }
    }

    /** Conversación hasta ahora, sin el saludo de la app, como contexto para el asistente en línea. */
    private fun currentHistory(): List<ChatTurn> =
        messages.value
            .filter { !it.isError && it.text != container.valbot.greeting }
            .map { ChatTurn(it.fromUser, it.text) }

    fun onNewConversation() {
        pending?.cancel()
        messages.value = listOf(botMessage(container.valbot.greeting))
        typing.value = false
        input.value = ""
    }

    fun onRetry() = container.network.retry()

    private fun botMessage(text: String) = ChatMessage(nextId++, false, text, now())

    private fun now(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    private companion object {
        const val MinTypingMillis = 700L

        /** Como el campo de la web. */
        const val MaxInputLength = 300
    }
}
