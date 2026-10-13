package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.HomeTile
import com.cmarcillovargas.turismovalencia.data.model.SortMode
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InicioUiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val featured: List<ContentItem> = emptyList(),
    val selected: Category? = Category.QueVer,
    val items: List<ContentItem> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val downloaded: Set<String> = emptySet(),
    val hasUnread: Boolean = false,
    val language: String = "ES",
    /** Título y texto del listado cuando la sección no es «Qué ver» (en «Qué ver» se usa «Cerca de ti»). */
    val sectionTitle: String? = null,
    val sectionSubtitle: String? = null,
    val esencial: List<HomeTile> = emptyList(),
    val planes: List<HomeTile> = emptyList(),
    val sostenible: List<HomeTile> = emptyList(),
)

/** A dónde lleva una tarjeta de las rejillas de la portada. */
sealed interface HomeTarget {
    data object Tickets : HomeTarget

    data class Item(val id: String) : HomeTarget

    data class Page(val id: String) : HomeTarget
}

private data class UserSnapshot(
    val favorites: Set<String>,
    val downloaded: Set<String>,
    val online: Boolean,
    val hasUnread: Boolean,
    val language: String,
)

class InicioViewModel(private val container: AppContainer) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val selected = MutableStateFlow<Category?>(Category.QueVer)

    private val user = combine(
        container.favorites.ids,
        container.downloads.ids,
        container.network.isOnline,
        container.notifications.items,
        container.settings.language,
    ) { favorites, downloaded, online, notifications, language ->
        UserSnapshot(favorites.toSet(), downloaded.toSet(), online, notifications.any { it.unread }, language.code)
    }

    // `content.origin` hace que las distancias («Cerca de ti») se recalculen cuando cambia la ubicación.
    val uiState: StateFlow<InicioUiState> = combine(loading, selected, user, container.content.origin) { isLoading, category, snapshot, _ ->
        InicioUiState(
            loading = isLoading,
            offline = !snapshot.online,
            featured = container.content.featuredFor(category),
            selected = category,
            items = itemsFor(category),
            sectionTitle = sectionOf(category)?.title,
            sectionSubtitle = sectionOf(category)?.subtitle,
            favorites = snapshot.favorites,
            downloaded = snapshot.downloaded,
            hasUnread = snapshot.hasUnread,
            language = snapshot.language,
            esencial = container.web.index.esencial,
            planes = container.web.index.planes,
            sostenible = container.web.index.sostenible,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    init {
        // TODO(api): sustituir el retardo simulado por la carga real del contenido.
        viewModelScope.launch {
            delay(LoadingMillis)
            loading.value = false
        }
    }

    private fun itemsFor(category: Category?): List<ContentItem> = when (category) {
        null, Category.QueVer -> container.content.nearby()
        else -> container.content.sorted(container.content.byCategory(category), SortMode.Distance).take(SectionItems)
    }

    private fun sectionOf(category: Category?) = category?.takeIf { it != Category.QueVer }?.let(container.content::sectionIndex)

    /** Elegir una sección cambia el carrusel y el listado; volver a pulsarla no la quita. */
    fun onSelectCategory(category: Category?) = selected.update { category ?: Category.QueVer }

    fun onToggleFavorite(id: String) = container.favorites.toggle(id)

    fun onToggleDownload(id: String) = container.downloads.toggle(id)

    fun onRetry() = container.network.retry()

    /** «Visitas y tours» abre las entradas; las páginas con ficha propia abren la ficha; el resto, su página. */
    fun targetOf(route: String): HomeTarget = when {
        route == "Entradas" -> HomeTarget.Tickets
        container.web.meta(route)?.family in PageFamilies -> HomeTarget.Page(route)
        else -> container.web.contentIdFor(route)?.let { HomeTarget.Item(it) } ?: HomeTarget.Page(route)
    }

    private companion object {
        const val LoadingMillis = 1_100L
        const val SectionItems = 6

        /** Las tarjetas de estas familias abren su página de la web; el resto abre la ficha de la app. */
        val PageFamilies = setOf(WebFamily.Esencial, WebFamily.Planes)
    }
}
