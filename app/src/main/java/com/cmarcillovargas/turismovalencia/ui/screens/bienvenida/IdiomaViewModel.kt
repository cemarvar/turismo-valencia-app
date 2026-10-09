package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class IdiomaOption(
    val code: String,
    @param:StringRes val name: Int,
    @param:StringRes val detail: Int,
)

data class IdiomaUiState(
    val options: List<IdiomaOption>,
    val selectedCode: String,
)

// Idiomas ofrecidos por el diseño.
internal val IdiomaOptions = listOf(
    IdiomaOption("ES", R.string.idioma_es_nombre, R.string.idioma_es_detalle),
    IdiomaOption("VA", R.string.idioma_va_nombre, R.string.idioma_va_detalle),
    IdiomaOption("EN", R.string.idioma_en_nombre, R.string.idioma_en_detalle),
    IdiomaOption("FR", R.string.idioma_fr_nombre, R.string.idioma_fr_detalle),
)

class IdiomaViewModel(private val settings: SettingsRepository) : ViewModel() {
    private val pending = MutableStateFlow(settings.language.value.code)

    val uiState: StateFlow<IdiomaUiState> = pending
        .map { IdiomaUiState(options = IdiomaOptions, selectedCode = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, IdiomaUiState(IdiomaOptions, pending.value))

    /** Al abrir el selector, parte del idioma que hay aplicado. */
    fun reset() {
        pending.value = settings.language.value.code
    }

    fun onSelect(code: String) {
        pending.value = code
    }

    // TODO(i18n): aplicar el idioma al contenido, a ValBot y al locale de la app.
    fun apply() {
        LanguageCode.entries.firstOrNull { it.code == pending.value }?.let(settings::setLanguage)
    }
}
