package com.cmarcillovargas.turismovalencia.util

import com.cmarcillovargas.turismovalencia.data.model.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    private val mercadoCentral = LatLon(39.4735, -0.3789)
    private val oceanografic = LatLon(39.4530, -0.3472)
    private val mountainView = LatLon(37.4220, -122.0841)
    private val alicante = LatLon(38.3452, -0.4810)

    @Test
    fun `la distancia de un punto a si mismo es cero`() {
        assertEquals(0.0, distanceMeters(ValenciaCenter, ValenciaCenter), 0.001)
    }

    @Test
    fun `la distancia es la misma en los dos sentidos`() {
        assertEquals(distanceMeters(ValenciaCenter, oceanografic), distanceMeters(oceanografic, ValenciaCenter), 0.001)
    }

    @Test
    fun `de la Plaza del Ayuntamiento al Mercado Central hay unos 480 metros`() {
        assertEquals(480.0, distanceMeters(ValenciaCenter, mercadoCentral), 40.0)
    }

    @Test
    fun `del centro al Oceanografic hay unos 3 kilometros`() {
        assertEquals(3_150.0, distanceMeters(ValenciaCenter, oceanografic), 150.0)
    }

    @Test
    fun `un grado de latitud son unos 111 kilometros`() {
        assertEquals(111_195.0, distanceMeters(LatLon(39.0, -0.4), LatLon(40.0, -0.4)), 200.0)
    }

    @Test
    fun `Valencia y su area cuentan como cerca de Valencia, California y Alicante no`() {
        assertTrue(ValenciaCenter.isNearValencia())
        assertTrue(oceanografic.isNearValencia())
        assertTrue(LatLon(39.68, -0.28).isNearValencia()) // Sagunto
        assertFalse(alicante.isNearValencia())
        assertFalse(mountainView.isNearValencia())
    }

    @Test
    fun `el punto de partida es la ubicacion real solo si esta en Valencia`() {
        assertEquals(mercadoCentral, resolveOrigin(mercadoCentral))
        assertEquals(ValenciaCenter, resolveOrigin(mountainView))
        assertEquals(ValenciaCenter, resolveOrigin(null))
    }
}
