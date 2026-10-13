package com.cmarcillovargas.turismovalencia.util

import com.cmarcillovargas.turismovalencia.data.model.LatLon
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Plaza del Ayuntamiento: el centro de la ciudad y el punto de partida de ejemplo cuando no hay ubicación real. */
val ValenciaCenter = LatLon(39.4699, -0.3763)

/** Radio alrededor de Valencia dentro del cual una ubicación se considera «estoy en la ciudad». */
const val ValenciaRadiusMeters = 35_000.0

private const val EarthRadiusMeters = 6_371_000.0

/** Distancia en línea recta entre dos puntos, en metros (fórmula del haversine). */
fun distanceMeters(a: LatLon, b: LatLon): Double {
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EarthRadiusMeters * asin(min(1.0, sqrt(h)))
}

fun LatLon.isNearValencia(radiusMeters: Double = ValenciaRadiusMeters): Boolean = distanceMeters(this, ValenciaCenter) <= radiusMeters

/**
 * Punto de partida de las rutas y de «Cerca de ti»: la ubicación real si se conoce y estamos en Valencia; si no (sin permiso,
 * sin señal o lejos de la ciudad, como el emulador en California), el centro de Valencia.
 */
fun resolveOrigin(real: LatLon?): LatLon = real?.takeIf { it.isNearValencia() } ?: ValenciaCenter
