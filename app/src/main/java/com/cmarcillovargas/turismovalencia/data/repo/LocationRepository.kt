package com.cmarcillovargas.turismovalencia.data.repo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.isNearValencia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Dónde está el usuario. Usa `LocationManager` (sin servicios de Google) y solo mientras el mapa o la ruta están en pantalla
 * ([start] y [stop]). La posición real solo cuenta si hay permiso del sistema, el usuario no eligió «No permitir» en Perfil y
 * está en Valencia; en cualquier otro caso (también el emulador, que está en California) se usa el centro de Valencia como
 * punto de partida de ejemplo, que es con el que están hechas las distancias del diseño.
 */
class LocationRepository(
    private val context: Context,
    settings: SettingsRepository,
    scope: CoroutineScope,
) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val real = MutableStateFlow<LatLon?>(null)
    private val permission = MutableStateFlow(hasPermissionNow())
    private var updating = false

    /** El permiso de ubicación del sistema está concedido (precisa o aproximada). */
    val permissionGranted: StateFlow<Boolean> = permission.asStateFlow()

    private val usable = combine(real, permission, settings.location) { position, granted, mode ->
        position.takeIf { granted && mode != LocationMode.Denied && it?.isNearValencia() == true }
    }

    /** `true` cuando [origin] es la posición real del usuario y no el punto de ejemplo. */
    val hasFix: StateFlow<Boolean> = usable.map { it != null }.stateIn(scope, SharingStarted.Eagerly, false)

    /** Punto de partida de las rutas y de «Cerca de ti». */
    val origin: StateFlow<LatLon> = usable.map { it ?: ValenciaCenter }.stateIn(scope, SharingStarted.Eagerly, ValenciaCenter)

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            real.value = LatLon(location.latitude, location.longitude)
        }

        @Deprecated("Solo existe en Android 10 y anteriores")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    /** Vuelve a mirar el permiso del sistema (el usuario pudo cambiarlo en Ajustes). */
    fun refreshPermission() {
        permission.value = hasPermissionNow()
        if (!permission.value) stop()
    }

    /** Empieza a seguir la posición: primero la última conocida y después los cambios, hasta [stop]. */
    @SuppressLint("MissingPermission")
    fun start() {
        refreshPermission()
        if (!permission.value || updating || manager == null) return
        val providers = providers()
        providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { real.value = LatLon(it.latitude, it.longitude) }
        providers.forEach { provider ->
            runCatching { manager.requestLocationUpdates(provider, UpdateMillis, UpdateMeters, listener, Looper.getMainLooper()) }
        }
        updating = true
    }

    fun stop() {
        if (!updating) return
        runCatching { manager?.removeUpdates(listener) }
        updating = false
    }

    private fun providers(): List<String> {
        val manager = manager ?: return emptyList()
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return buildList {
            if (fine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    }

    private fun hasPermissionNow(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private companion object {
        const val UpdateMillis = 3_000L
        const val UpdateMeters = 5f
    }
}
