package com.cmarcillovargas.turismovalencia.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.MapPoint
import com.cmarcillovargas.turismovalencia.ui.components.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TravelMode { Walk, Bike, Bus, Metro }

enum class StepKind { Straight, Left, Arrive }

data class RouteStep(val kind: StepKind, val street: String?, val meters: Int?)

data class RutaUiState(
    val item: ContentItem? = null,
    val mode: TravelMode = TravelMode.Walk,
    /** Minutos por modo; `null` cuando el modo se muestra solo con su nombre (metro). */
    val minutes: Map<TravelMode, Int?> = emptyMap(),
    val arrival: String = "",
    val steps: List<RouteStep> = emptyList(),
    val route: List<MapPoint> = emptyList(),
    val saved: Boolean = false,
)

// TODO(mapa): las rutas son de ejemplo (distancia en línea recta y calles inventadas); el cálculo real vendrá con el mapa.
class RutaViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private val mode = MutableStateFlow(TravelMode.Walk)

    val uiState: StateFlow<RutaUiState> = combine(mode, container.downloads.ids) { selected, downloads ->
        val item = container.content.item(id) ?: return@combine RutaUiState()
        val meters = item.distanceMeters.coerceAtLeast(MinMeters)
        val minutes = mapOf(
            TravelMode.Walk to meters / 75,
            TravelMode.Bike to meters / 150,
            TravelMode.Bus to 6 + meters / 150,
            TravelMode.Metro to null,
        )
        val duration = minutes[selected] ?: (4 + meters / 200)
        val hints = streets[id]
        RutaUiState(
            item = item,
            mode = selected,
            minutes = minutes,
            arrival = LocalTime.now().plusMinutes(duration.toLong()).format(DateTimeFormatter.ofPattern("HH:mm")),
            steps = listOf(
                RouteStep(StepKind.Straight, hints?.first, meters * 2 / 3),
                RouteStep(StepKind.Left, hints?.second ?: item.area, meters / 3),
                RouteStep(StepKind.Arrive, item.title, null),
            ),
            route = elbow(UserLocation, item.mapPoint),
            saved = id in downloads,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RutaUiState())

    fun onSelectMode(value: TravelMode) {
        mode.value = value
    }

    fun onToggleSaved() = container.downloads.toggle(id)

    private fun elbow(from: MapPoint, to: MapPoint): List<MapPoint> {
        val midY = (from.y + to.y) / 2 + 20f
        return listOf(from, MapPoint(from.x + 4f, midY - 14f), MapPoint(to.x - 5f, midY - 22f), MapPoint(to.x, to.y + 34f))
    }

    private companion object {
        const val MinMeters = 150
        val streets = mapOf("mercado-central" to ("Av. María Cristina" to "Pl. Ciudad de Brujas"))
    }
}
