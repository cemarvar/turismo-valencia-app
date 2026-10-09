package com.cmarcillovargas.turismovalencia.ui.screens.valbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ChatMessage
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
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
)

class ValBotViewModel(private val container: AppContainer) : ViewModel() {
    private var nextId = 0L
    private val messages = MutableStateFlow(listOf(botMessage(container.valbot.greeting)))
    private val typing = MutableStateFlow(false)
    private val input = MutableStateFlow("")

    val uiState: StateFlow<ValBotUiState> = combine(messages, typing, input, container.network.isOnline) { list, isTyping, text, online ->
        ValBotUiState(
            messages = list,
            typing = isTyping,
            online = online,
            input = text,
            suggestions = container.valbot.suggestions,
            items = container.content.all.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ValBotUiState())

    fun onInputChange(value: String) {
        input.value = value
    }

    /** Envía la pregunta escrita o una de las sugerencias. */
    fun send(text: String = input.value) {
        val question = text.trim()
        if (question.isEmpty() || typing.value || !container.network.isOnline.value) return
        input.value = ""
        messages.update { it + ChatMessage(nextId++, true, question, now()) }
        viewModelScope.launch {
            typing.value = true
            delay(ReplyMillis)
            // TODO(api): llamar al backend (Gemini) en lugar de las respuestas de ejemplo.
            val reply = container.valbot.reply(question)
            typing.value = false
            messages.update { it + ChatMessage(nextId++, false, reply.text, now(), reply.cardIds) }
        }
    }

    fun onNewConversation() {
        messages.value = listOf(botMessage(container.valbot.greeting))
        typing.value = false
        input.value = ""
    }

    fun onRetry() = container.network.retry()

    private fun botMessage(text: String) = ChatMessage(nextId++, false, text, now())

    private fun now(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    private companion object {
        const val ReplyMillis = 1_100L
    }
}
