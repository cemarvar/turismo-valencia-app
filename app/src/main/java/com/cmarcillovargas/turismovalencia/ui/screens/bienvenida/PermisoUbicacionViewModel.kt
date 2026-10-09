package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LocationAccuracy { Precise, Approximate }

data class PermisoUbicacionUiState(
    val accuracy: LocationAccuracy = LocationAccuracy.Precise,
)

class PermisoUbicacionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PermisoUbicacionUiState())
    val uiState: StateFlow<PermisoUbicacionUiState> = _uiState.asStateFlow()

    fun onAccuracyChange(accuracy: LocationAccuracy) = _uiState.update { it.copy(accuracy = accuracy) }
}
