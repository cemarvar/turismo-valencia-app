package com.cmarcillovargas.turismovalencia.data.fake

import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.distanceMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las coordenadas se escribieron a mano: esto caza latitud y longitud cambiadas, signos perdidos y puntos duplicados. */
class ContentCoordinatesTest {
    private val items = FakeQueVer + FakeQueVerWeb + FakeGastronomia + FakeAlojamientos + FakeEventos + FakeEventosWeb + FakeSostenible
    private val onMap = items.filter { it.onMap }

    @Test
    fun `todas las fichas del mapa tienen coordenadas`() {
        assertEquals(24, onMap.size)
        onMap.forEach { assertNotNull("«${it.id}» sin coordenadas", it.location) }
    }

    @Test
    fun `todas caen en la zona de Valencia y a menos de 20 km del centro`() {
        onMap.forEach { item ->
            val place = item.location!!
            assertTrue("«${item.id}» fuera de Valencia: $place", place.lat in 39.20..39.62 && place.lon in -0.60..-0.10)
            assertTrue("«${item.id}» a ${distanceMeters(ValenciaCenter, place)} m del centro", distanceMeters(ValenciaCenter, place) < 20_000)
        }
    }

    @Test
    fun `dos fichas no comparten el mismo punto exacto`() {
        val repeated = onMap.groupBy { it.location }.filter { it.value.size > 1 }
        assertTrue("Puntos repetidos: ${repeated.values.map { group -> group.map { it.id } }}", repeated.isEmpty())
    }

    @Test
    fun `las fichas que no salen en el mapa no llevan coordenadas`() {
        items.filter { !it.onMap }.forEach { assertTrue("«${it.id}» tiene coordenadas pero no sale en el mapa", it.location == null) }
    }

    @Test
    fun `distancias conocidas desde el centro`() {
        fun meters(id: String) = distanceMeters(ValenciaCenter, items.first { it.id == id }.location!!)
        assertEquals(480.0, meters("mercado-central"), 60.0)
        assertEquals(3_150.0, meters("oceanografic"), 200.0)
        // La Albufera está a más de 10 km del centro.
        assertTrue(meters("albufera") > 10_000)
    }
}
