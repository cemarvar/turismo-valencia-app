package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoriaUiState(
    val category: Category = Category.QueVer,
    /** Subcategorías disponibles; el filtro «Todos» lo añade la pantalla. */
    val filters: List<String> = emptyList(),
    /** Subcategoría activa o `null` para «Todos». */
    val selectedFilter: String? = null,
    val sort: SortMode = SortMode.Distance,
    val gridMode: Boolean = false,
    val items: List<ContentItem> = emptyList(),
    val total: Int = 0,
    val favorites: Set<String> = emptySet(),
    val downloaded: Set<String> = emptySet(),
)

class CategoriaViewModel(private val container: AppContainer, private val category: Category) : ViewModel() {
    private val filter = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(SortMode.Distance)
    private val grid = MutableStateFlow(false)

    private val all = container.content.byCategory(category)

    val uiState: StateFlow<CategoriaUiState> = combine(
        filter, sort, grid, container.favorites.ids, container.downloads.ids,
    ) { selected, mode, isGrid, favorites, downloaded ->
        val filtered = all.filter { selected == null || it.subcategory == selected }
        CategoriaUiState(
            category = category,
            filters = all.map { it.subcategory }.distinct(),
            selectedFilter = selected,
            sort = mode,
            gridMode = isGrid,
            items = container.content.sorted(filtered, mode),
            total = all.size,
            favorites = favorites.toSet(),
            downloaded = downloaded.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriaUiState(category = category))

    fun onSelectFilter(value: String?) {
        filter.value = value
    }

    fun onSort(mode: SortMode) {
        sort.value = mode
    }

    fun onToggleGrid() {
        grid.value = !grid.value
    }

    fun onToggleFavorite(id: String) = container.favorites.toggle(id)

    fun onToggleDownload(id: String) = container.downloads.toggle(id)
}
