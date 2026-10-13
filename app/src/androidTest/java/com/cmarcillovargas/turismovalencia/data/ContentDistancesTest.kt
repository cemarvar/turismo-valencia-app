package com.cmarcillovargas.turismovalencia.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Las distancias de las fichas salen de sus coordenadas y del punto de partida, y se recalculan al moverse el usuario. */
@RunWith(AndroidJUnit4::class)
class ContentDistancesTest {
    private lateinit var content: ContentRepository

    @Before
    fun setUp() {
        content = ContentRepository(WebContentRepository(ApplicationProvider.getApplicationContext()))
    }

    private fun meters(id: String) = content.item(id)!!.distanceMeters

    @Test
    fun porDefectoSeMidenDesdeElCentroDeValencia() {
        assertEquals(ValenciaCenter, content.origin.value)
        assertEquals(480.0, meters("mercado-central").toDouble(), 60.0)
        assertEquals(3_150.0, meters("oceanografic").toDouble(), 200.0)
        assertTrue(meters("albufera") > 10_000)
    }

    @Test
    fun alMoverseElUsuarioSeRecalculan() {
        val oceanografic = content.item("oceanografic")!!.location!!
        content.updateOrigin(oceanografic)

        assertEquals(oceanografic, content.origin.value)
        assertEquals(1, meters("oceanografic")) // al lado: nunca 0, que significa «sin distancia»
        assertTrue(meters("mercado-central") in 3_300..3_800)
        // `all`, `item` e `items` ven lo mismo.
        assertEquals(meters("mercado-central"), content.all.first { it.id == "mercado-central" }.distanceMeters)
        assertEquals(meters("mercado-central"), content.items(listOf("mercado-central")).single().distanceMeters)
    }

    @Test
    fun loQueNoLlevabaDistanciaSigueSinElla() {
        assertEquals(0, meters("paella"))
        assertEquals(0, meters("fallas"))
        assertEquals(0, meters("horchata"))
        // Sin coordenadas: conserva el valor del diseño.
        assertEquals(1_500, meters("aloj-gran-via"))
        content.updateOrigin(content.item("oceanografic")!!.location!!)
        assertEquals(0, meters("paella"))
        assertEquals(1_500, meters("aloj-gran-via"))
    }

    @Test
    fun cercaDeTiOrdenaPorLaDistanciaReal() {
        content.updateOrigin(content.item("oceanografic")!!.location!!)
        val sorted = content.sorted(content.byCategory(com.cmarcillovargas.turismovalencia.data.model.Category.QueVer), com.cmarcillovargas.turismovalencia.data.model.SortMode.Distance)
        assertEquals("oceanografic", sorted.first().id)
    }
}
