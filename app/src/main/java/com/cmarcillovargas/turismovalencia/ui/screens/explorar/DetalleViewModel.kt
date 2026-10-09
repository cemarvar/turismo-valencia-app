package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DetalleUiState(
    val item: ContentItem? = null,
    val favorite: Boolean = false,
    val downloaded: Boolean = false,
    val reminderOn: Boolean = true,
)

class DetalleViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private val reminder = MutableStateFlow(true)

    val uiState: StateFlow<DetalleUiState> = combine(
        container.favorites.ids, container.downloads.ids, reminder,
    ) { favorites, downloads, reminderOn ->
        DetalleUiState(
            item = container.content.item(id),
            favorite = id in favorites,
            downloaded = id in downloads,
            reminderOn = reminderOn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState(item = container.content.item(id)))

    fun onToggleFavorite() = container.favorites.toggle(id)

    fun onToggleDownload() = container.downloads.toggle(id)

    // TODO(permisos): programar la notificación local (mascletà 30 min antes) cuando exista el canal de notificaciones.
    fun onReminderChange(value: Boolean) {
        reminder.value = value
    }
}
