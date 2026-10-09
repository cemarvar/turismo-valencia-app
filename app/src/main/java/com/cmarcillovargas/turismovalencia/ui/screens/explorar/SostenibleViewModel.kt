package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SostenibleUiState(
    /** Tema principal de la portada («Ecoturismo en Valencia»): da la foto, el título y las cifras. */
    val cover: ContentItem? = null,
    val topics: List<ContentItem> = emptyList(),
    val coverFavorite: Boolean = false,
)

class SostenibleViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<SostenibleUiState> = container.favorites.ids.map { favorites ->
        SostenibleUiState(
            cover = container.content.item("ecoturismo"),
            topics = container.content.sustainableTopics(),
            coverFavorite = "ecoturismo" in favorites,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SostenibleUiState())

    fun onToggleCoverFavorite() = container.favorites.toggle("ecoturismo")
}
