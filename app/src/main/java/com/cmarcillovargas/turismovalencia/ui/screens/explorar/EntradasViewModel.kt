package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.util.normalizeText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Pestañas de la pantalla, en el orden del diseño. */
enum class OfferTab(val kind: OfferKind) { Entradas(OfferKind.Entrada), Tours(OfferKind.Tour), Experiencias(OfferKind.Experiencia) }

data class EntradasUiState(
    val tab: OfferTab = OfferTab.Experiencias,
    val offers: List<Offer> = emptyList(),
    val searching: Boolean = false,
    val query: String = "",
)

class EntradasViewModel(private val container: AppContainer, initialTab: OfferTab) : ViewModel() {
    private val tab = MutableStateFlow(initialTab)
    private val searching = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<EntradasUiState> = combine(tab, searching, query) { currentTab, isSearching, text ->
        val offers = container.content.offersOf(currentTab.kind).filter { offer ->
            text.isBlank() || normalizeText(offer.title + " " + offer.place + " " + offer.tag).contains(normalizeText(text.trim()))
        }
        EntradasUiState(currentTab, offers, isSearching, text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntradasUiState(initialTab))

    fun onSelectTab(value: OfferTab) {
        tab.value = value
    }

    fun onSearching(value: Boolean) {
        searching.value = value
        if (!value) query.value = ""
    }

    fun onQueryChange(value: String) {
        query.value = value
    }
}
