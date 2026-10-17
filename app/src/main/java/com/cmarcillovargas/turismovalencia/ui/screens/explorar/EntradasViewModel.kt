package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.data.model.WebPageMeta
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
    /** Guías de la web de la pestaña (Valencia Tourist Card, tours, náuticas…). */
    val guides: List<WebPageMeta> = emptyList(),
)

class EntradasViewModel(private val container: AppContainer, initialTab: OfferTab) : ViewModel() {
    private val tab = MutableStateFlow(initialTab)
    private val searching = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    // El catálogo es el del servidor: la lista se rehace cuando llega una versión nueva.
    val uiState: StateFlow<EntradasUiState> = combine(tab, searching, query, container.content.offersFlow) { currentTab, isSearching, text, catalog ->
        val offers = catalog.filter { it.kind == currentTab.kind }.filter { offer ->
            text.isBlank() || normalizeText(offer.title + " " + offer.place + " " + offer.tag).contains(normalizeText(text.trim()))
        }
        EntradasUiState(currentTab, offers, isSearching, text, if (text.isBlank()) guidesOf(currentTab) else emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntradasUiState(initialTab))

    private fun guidesOf(tab: OfferTab): List<WebPageMeta> = GuideOrder.getValue(tab).mapNotNull(container.web::meta)

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

    private companion object {
        /** Páginas de la web de cada pestaña, en el orden de la web. */
        val GuideOrder = mapOf(
            OfferTab.Entradas to listOf("ValenciaCard", "BusTuristico"),
            OfferTab.Tours to listOf("CentroHistorico", "CienciasArtes", "Bici", "PaseoMaritimo", "PrivadoGrupo"),
            OfferTab.Experiencias to listOf("Excursiones", "ActividadesGastronomicas", "Nauticas"),
        )
    }
}
