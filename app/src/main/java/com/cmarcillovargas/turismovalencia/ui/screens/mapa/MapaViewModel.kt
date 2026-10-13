package com.cmarcillovargas.turismovalencia.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.ui.components.MapMarker
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.distanceMeters
import kotlin.math.roundToInt
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
    /** Dónde está el usuario (o el punto de ejemplo, ver `LocationRepository`). */
    val user: LatLon = ValenciaCenter,
    /** Metros en línea recta desde [user] hasta la ficha elegida. */
    val selectedMeters: Int = 0,
    /** El permiso de ubicación del sistema está concedido. */
    val hasPermission: Boolean = false,
    /** `user` es la posición real y no el punto de ejemplo. */
    val hasFix: Boolean = false,
    val selected: ContentItem? = null,
    val favorite: Boolean = false,
    val downloaded: Boolean = false,
)

class MapaViewModel(private val container: AppContainer, focusId: String?) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<Category?>(null)
    private val onlySaved = MutableStateFlow(false)
    private val selectedId = MutableStateFlow(focusId ?: DefaultSelected)

    private class UserData(
        val favorites: List<String>,
        val downloads: List<String>,
        val origin: LatLon,
        val hasPermission: Boolean,
        val hasFix: Boolean,
    )

    private val user = combine(
        container.favorites.ids,
        container.downloads.ids,
        container.content.origin,
        container.location.permissionGranted,
        container.location.hasFix,
    ) { favorites, downloads, origin, permission, fix -> UserData(favorites, downloads, origin, permission, fix) }

    val uiState: StateFlow<MapaUiState> = combine(
        query, filter, onlySaved, selectedId, user,
    ) { text, category, saved, selected, data ->
        val favorites = data.favorites
        val downloads = data.downloads
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
            markers = visible.mapNotNull { item -> item.location?.let { MapMarker(item.id, it, item.mapCategory ?: item.category, item.title) } },
            user = data.origin,
            selectedMeters = item?.location?.let { distanceMeters(data.origin, it).roundToInt() } ?: item?.distanceMeters ?: 0,
            hasPermission = data.hasPermission,
            hasFix = data.hasFix,
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

    /** El mapa se ve: empieza a seguir la ubicación del usuario (si hay permiso). */
    fun onVisible() = container.location.start()

    fun onHidden() = container.location.stop()

    /** Resultado del diálogo del sistema que pide la ubicación desde el botón «Centrar en mi ubicación». */
    fun onLocationResult(granted: Boolean) {
        container.settings.setLocation(if (granted) LocationMode.WhileUsing else LocationMode.Denied)
        container.location.refreshPermission()
        if (granted) container.location.start()
    }

    fun setQuery(text: String) {
        query.value = text
    }

    private companion object {
        const val DefaultSelected = "mercado-central"
    }
}
