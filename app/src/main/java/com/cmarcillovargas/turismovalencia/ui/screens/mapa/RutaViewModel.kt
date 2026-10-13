package com.cmarcillovargas.turismovalencia.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.RoutePlan
import com.cmarcillovargas.turismovalencia.data.model.RouteProfile
import com.cmarcillovargas.turismovalencia.data.model.RouteStep
import com.cmarcillovargas.turismovalencia.data.model.StepKind
import com.cmarcillovargas.turismovalencia.data.remote.RouteException
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.distanceMeters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TravelMode { Walk, Bike, Bus, Metro }

/** Cómo va el cálculo de la ruta: en marcha, hecho con el servicio de rutas, o aproximado porque el servicio no respondió. */
enum class RouteStatus { Loading, Ready, Estimated }

data class RutaUiState(
    val item: ContentItem? = null,
    val mode: TravelMode = TravelMode.Walk,
    /** Minutos por modo; `null` cuando el modo se muestra solo con su nombre (metro). */
    val minutes: Map<TravelMode, Int?> = emptyMap(),
    /** Metros de la ruta del modo elegido. */
    val meters: Int = 0,
    val arrival: String = "",
    val steps: List<RouteStep> = emptyList(),
    /** Punto de partida (tu ubicación o el centro de Valencia). */
    val origin: LatLon = ValenciaCenter,
    val route: List<LatLon> = emptyList(),
    val status: RouteStatus = RouteStatus.Loading,
    val saved: Boolean = false,
)

/** Rutas calculadas para un origen: `null` en el estado mientras se calculan. */
private data class Plans(val byProfile: Map<RouteProfile, RoutePlan>)

/**
 * Ruta a pie y en bici desde el punto de partida hasta el lugar, con el servicio de rutas de OpenStreetMap. Si no responde
 * (sin conexión, por ejemplo), se dibuja la línea recta y los tiempos son una estimación. Autobús y metro son siempre estimaciones.
 */
class RutaViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private val mode = MutableStateFlow(TravelMode.Walk)
    private val plans = MutableStateFlow<Plans?>(null)

    init {
        val destination = container.content.item(id)?.location
        viewModelScope.launch {
            if (destination == null) {
                plans.value = Plans(emptyMap())
                return@launch
            }
            // Si la ubicación cambia (el usuario se mueve), se recalcula desde el nuevo punto.
            container.location.origin.collectLatest { origin ->
                plans.value = null
                plans.value = Plans(load(origin, destination))
            }
        }
    }

    val uiState: StateFlow<RutaUiState> = combine(mode, container.downloads.ids, container.location.origin, plans) { selected, downloads, origin, planSet ->
        val item = container.content.item(id) ?: return@combine RutaUiState()
        val straight = item.location?.let { distanceMeters(origin, it) } ?: item.distanceMeters.toDouble()
        // Por las calles hay algo más que en línea recta.
        val estimated = (straight * StreetFactor).toInt().coerceAtLeast(MinMeters)
        val foot = planSet?.byProfile?.get(RouteProfile.Foot)
        val bike = planSet?.byProfile?.get(RouteProfile.Bike)
        val minutes = mapOf(
            TravelMode.Walk to (foot?.minutes ?: estimated / WalkMetersPerMinute),
            TravelMode.Bike to (bike?.minutes ?: estimated / BikeMetersPerMinute),
            TravelMode.Bus to BusWaitMinutes + (foot?.meters ?: estimated) / BusMetersPerMinute,
            TravelMode.Metro to null,
        )
        val active = if (selected == TravelMode.Bike) bike ?: foot else foot ?: bike
        val meters = active?.meters ?: estimated
        val duration = minutes[selected] ?: (MetroWaitMinutes + meters / MetroMetersPerMinute)
        RutaUiState(
            item = item,
            mode = selected,
            minutes = minutes,
            meters = meters,
            arrival = LocalTime.now().plusMinutes(duration.toLong()).format(DateTimeFormatter.ofPattern("HH:mm")),
            steps = active?.steps ?: fallbackSteps(item, meters),
            origin = origin,
            route = active?.points ?: listOfNotNull(origin, item.location),
            status = when {
                planSet == null -> RouteStatus.Loading
                active == null -> RouteStatus.Estimated
                else -> RouteStatus.Ready
            },
            saved = id in downloads,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RutaUiState())

    fun onSelectMode(value: TravelMode) {
        mode.value = value
    }

    fun onToggleSaved() = container.downloads.toggle(id)

    /** La ruta se ve: sigue la ubicación del usuario (si hay permiso) para que el punto de partida sea el real. */
    fun onVisible() = container.location.start()

    fun onHidden() = container.location.stop()

    /** Pide a la vez la ruta a pie y la de bici; la que falle se queda fuera y su modo se estima. */
    private suspend fun load(origin: LatLon, destination: LatLon): Map<RouteProfile, RoutePlan> = coroutineScope {
        RouteProfile.entries
            .map { profile ->
                async {
                    try {
                        profile to container.routes.route(profile, origin, destination)
                    } catch (_: RouteException) {
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .toMap()
    }

    /** Pasos de ejemplo cuando no hay ruta real: recto, un giro y la llegada. */
    private fun fallbackSteps(item: ContentItem, meters: Int): List<RouteStep> {
        val hints = streets[id]
        return listOf(
            RouteStep(StepKind.Straight, hints?.first, meters * 2 / 3),
            RouteStep(StepKind.Left, hints?.second ?: item.area, meters / 3),
            RouteStep(StepKind.Arrive, item.title, null),
        )
    }

    private companion object {
        const val MinMeters = 150
        const val StreetFactor = 1.25
        const val WalkMetersPerMinute = 75
        const val BikeMetersPerMinute = 150
        const val BusMetersPerMinute = 150
        const val BusWaitMinutes = 6
        const val MetroMetersPerMinute = 200
        const val MetroWaitMinutes = 4
        val streets = mapOf("mercado-central" to ("Av. María Cristina" to "Pl. Ciudad de Brujas"))
    }
}
