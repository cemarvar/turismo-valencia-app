package com.cmarcillovargas.turismovalencia.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.ui.components.MapMarker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapaUiState(
    val query: String = "",
    /** `null` equivale al chip «Todo». */
    val filter: Category? = null,
    val onlySaved: Boolean = false,
    val markers: List<MapMarker> = emptyList(),
    val selected: ContentItem? = null,
    val favorite: Boolean = false,
    val downloaded: Boolean = false,
)

class MapaViewModel(private val container: AppContainer, focusId: String?) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<Category?>(null)
    private val onlySaved = MutableStateFlow(false)
    private val selectedId = MutableStateFlow(focusId ?: DefaultSelected)

    val uiState: StateFlow<MapaUiState> = combine(
        query, filter, onlySaved, selectedId, combine(container.favorites.ids, container.downloads.ids) { f, d -> f to d },
    ) { text, category, saved, selected, (favorites, downloads) ->
        val visible = container.content.all.filter { item ->
            item.onMap &&
                (category == null || (item.mapCategory ?: item.category) == category) &&
                (!saved || item.id in downloads) &&
                (text.isBlank() || container.content.matches(item, text))
        }
        val item = container.content.item(selected)
        MapaUiState(
            query = text,
            filter = category,
            onlySaved = saved,
            markers = visible.map { MapMarker(it.id, it.mapPoint, it.mapCategory ?: it.category, it.title) },
            selected = item?.takeIf { it.id in visible.map(ContentItem::id) },
            favorite = selected in favorites,
            downloaded = selected in downloads,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapaUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilter(value: Category?) {
        filter.value = value
    }

    fun onToggleSaved() {
        onlySaved.value = !onlySaved.value
    }

    fun onClearFilters() {
        query.value = ""
        filter.value = null
        onlySaved.value = false
    }

    fun onSelect(id: String) {
        selectedId.value = id
    }

    fun onToggleFavorite() = container.favorites.toggle(selectedId.value)

    fun setQuery(text: String) {
        query.value = text
    }

    private companion object {
        const val DefaultSelected = "mercado-central"
    }
}
