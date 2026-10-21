package com.cmarcillovargas.turismovalencia.ui.screens.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import com.cmarcillovargas.turismovalencia.data.model.ThemeMode
import com.cmarcillovargas.turismovalencia.data.repo.DownloadsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PerfilUiState(
    val offlineMb: Int = 0,
    val cardsMb: Int = 0,
    val reservations: Int = 0,
    val language: LanguageCode = LanguageCode.ES,
    /** Las notificaciones llegarán: el ajuste está activo y el sistema las permite. */
    val notifications: Boolean = true,
    val location: LocationMode = LocationMode.WhileUsing,
    val textScale: TextScale = TextScale.System,
    val themeMode: ThemeMode = ThemeMode.System,
)

class PerfilViewModel(private val container: AppContainer) : ViewModel() {
    private val settings = combine(
        container.settings.language,
        container.reminders.active,
        container.settings.location,
        container.settings.textScale,
        container.settings.themeMode,
    ) { language, notifications, location, scale, theme -> listOf(language, notifications, location, scale, theme) }

    val uiState: StateFlow<PerfilUiState> = combine(
        settings, container.downloads.ids, container.downloads.mapDownloaded, container.reservations.items,
    ) { values, downloads, map, reservations ->
        PerfilUiState(
            offlineMb = container.downloads.cardsMb(downloads) + if (map) DownloadsRepository.MAP_MB else 0,
            cardsMb = container.downloads.cardsMb(downloads),
            reservations = reservations.size,
            language = values[0] as LanguageCode,
            notifications = values[1] as Boolean,
            location = values[2] as LocationMode,
            textScale = values[3] as TextScale,
            themeMode = values[4] as ThemeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState())

    /** Enciende o apaga el ajuste general de Notificaciones; al encenderlo la pantalla pide además el permiso del sistema. */
    fun onNotifications(value: Boolean) = container.settings.setNotificationsEnabled(value)

    fun onLocation(value: LocationMode) = container.settings.setLocation(value)

    /** ¿Tiene la app el permiso de ubicación del sistema? (se vuelve a mirar: pudo cambiarse en Ajustes). */
    fun locationPermissionGranted(): Boolean {
        container.location.refreshPermission()
        return container.location.permissionGranted.value
    }

    fun onTextScale(value: TextScale) = container.settings.setTextScale(value)

    /** Claro, oscuro o el del móvil: Android recrea la actividad con el modo nuevo (la pantalla vuelve a pintarse sola). */
    fun onThemeMode(value: ThemeMode) = container.settings.setThemeMode(value)
}
