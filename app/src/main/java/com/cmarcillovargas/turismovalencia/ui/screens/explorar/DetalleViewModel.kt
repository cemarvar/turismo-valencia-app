package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.data.notify.EventReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleUiState(
    val item: ContentItem? = null,
    val favorite: Boolean = false,
    val downloaded: Boolean = false,
    /** Esta ficha ofrece un aviso (la mascletà de las Fallas). */
    val reminderAvailable: Boolean = false,
    /** El aviso llegará: el usuario lo quiere, el ajuste de Notificaciones está activo y el sistema las permite. */
    val reminderOn: Boolean = false,
    /** Guía completa de la web de esta ficha (secciones, datos y consejos), cuando ya se ha leído. */
    val page: WebPage? = null,
)

class DetalleViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private val page = MutableStateFlow<WebPage?>(null)
    private val reminderAvailable = EventReminders.of(id) != null

    val uiState: StateFlow<DetalleUiState> = combine(
        combine(container.favorites.ids, container.downloads.ids, page, container.content.origin) { favorites, downloads, webPage, _ ->
            Triple(favorites, downloads, webPage)
        },
        container.reminders.ids,
        container.reminders.active,
    ) { (favorites, downloads, webPage), reminders, active ->
        DetalleUiState(
            item = container.content.item(id),
            favorite = id in favorites,
            downloaded = id in downloads,
            reminderAvailable = reminderAvailable,
            reminderOn = reminderAvailable && active && id in reminders,
            page = webPage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState(item = container.content.item(id)))

    init {
        container.content.item(id)?.pageId?.let { pageId -> viewModelScope.launch { page.value = container.web.page(pageId) } }
    }

    fun onToggleFavorite() = container.favorites.toggle(id)

    fun onToggleDownload() = container.downloads.toggle(id)

    /**
     * El usuario enciende o apaga el aviso. Encenderlo guarda la petición y activa el ajuste general de Notificaciones; que
     * llegue de verdad depende además del permiso del sistema, que pide la pantalla (ver `rememberNotificationAccessRequest`).
     */
    fun onReminderChange(value: Boolean) {
        if (!reminderAvailable) return
        container.reminders.set(id, value)
        if (value) container.settings.setNotificationsEnabled(true)
    }
}
