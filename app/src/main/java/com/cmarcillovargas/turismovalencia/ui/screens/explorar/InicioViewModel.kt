package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.SortMode
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
)

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

    val uiState: StateFlow<InicioUiState> = combine(loading, selected, user) { isLoading, category, snapshot ->
        InicioUiState(
            loading = isLoading,
            offline = !snapshot.online,
            featured = container.content.featured(),
            selected = category,
            items = itemsFor(category),
            favorites = snapshot.favorites,
            downloaded = snapshot.downloaded,
            hasUnread = snapshot.hasUnread,
            language = snapshot.language,
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
        else -> container.content.sorted(container.content.byCategory(category), SortMode.Distance).take(4)
    }

    fun onSelectCategory(category: Category?) = selected.update { if (it == category) null else category }

    fun onToggleFavorite(id: String) = container.favorites.toggle(id)

    fun onToggleDownload(id: String) = container.downloads.toggle(id)

    fun onRetry() = container.network.retry()

    private companion object {
        const val LoadingMillis = 1_100L
    }
}
