package com.cmarcillovargas.turismovalencia.ui.screens.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FavoritesTab { All, Places, Events }

data class FavoritosUiState(
    val tab: FavoritesTab = FavoritesTab.All,
    val items: List<ContentItem> = emptyList(),
    val totalSaved: Int = 0,
    val availableOffline: Int = 0,
    val offlineMb: Int = 0,
    val downloaded: Set<String> = emptySet(),
)

class FavoritosViewModel(private val container: AppContainer) : ViewModel() {
    private val tab = MutableStateFlow(FavoritesTab.All)

    val uiState: StateFlow<FavoritosUiState> = combine(tab, container.favorites.ids, container.downloads.ids, container.content.origin) { current, favorites, downloads, _ ->
        val all = container.content.items(favorites)
        val offline = all.filter { it.id in downloads }
        FavoritosUiState(
            tab = current,
            items = when (current) {
                FavoritesTab.All -> all
                FavoritesTab.Places -> all.filterNot { it.isEvent }
                FavoritesTab.Events -> all.filter { it.isEvent }
            },
            totalSaved = all.size,
            availableOffline = offline.size,
            offlineMb = offline.sumOf { it.offlineMb },
            downloaded = downloads.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritosUiState())

    fun onSelectTab(value: FavoritesTab) {
        tab.value = value
    }

    fun onToggleFavorite(id: String) = container.favorites.toggle(id)

    fun onToggleDownload(id: String) = container.downloads.toggle(id)
}
