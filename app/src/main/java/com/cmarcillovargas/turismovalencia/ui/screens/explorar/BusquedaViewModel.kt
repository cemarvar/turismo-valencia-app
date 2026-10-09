package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.SearchSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BusquedaUiState(
    val query: String = "",
    val suggestions: List<SearchSuggestion> = emptyList(),
    val recent: List<String> = emptyList(),
    val trends: List<String> = emptyList(),
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val noResults: Boolean get() = hasQuery && suggestions.isEmpty()
}

class BusquedaViewModel(private val container: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<BusquedaUiState> = combine(query, container.searchHistory.recent) { q, recent ->
        BusquedaUiState(
            query = q,
            suggestions = container.content.search(q),
            recent = recent,
            trends = Trends,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusquedaUiState(trends = Trends))

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Guarda la búsqueda en recientes al abrir un resultado. */
    fun remember(text: String) = container.searchHistory.add(text)

    private companion object {
        val Trends = listOf("Mascletà", "Oceanogràfic", "Albufera")
    }
}
