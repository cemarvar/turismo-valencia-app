package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.lifecycle.ViewModel
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LocationAccuracy { Precise, Approximate }

data class PermisoUbicacionUiState(
    val accuracy: LocationAccuracy = LocationAccuracy.Precise,
)

class PermisoUbicacionViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(PermisoUbicacionUiState())
    val uiState: StateFlow<PermisoUbicacionUiState> = _uiState.asStateFlow()

    fun onAccuracyChange(accuracy: LocationAccuracy) = _uiState.update { it.copy(accuracy = accuracy) }

    /** El usuario eligió en la pantalla; [granted] es lo que respondió después el diálogo del sistema. */
    fun onDecision(mode: LocationMode, granted: Boolean) {
        container.settings.setLocation(if (granted) mode else LocationMode.Denied)
        container.location.refreshPermission()
    }
}
