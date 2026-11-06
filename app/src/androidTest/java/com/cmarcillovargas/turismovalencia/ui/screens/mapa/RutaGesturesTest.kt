package com.cmarcillovargas.turismovalencia.ui.screens.mapa

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.RouteStep
import com.cmarcillovargas.turismovalencia.data.model.StepKind
import com.cmarcillovargas.turismovalencia.ui.components.MapCamera
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.concurrent.thread

/**
 * Con el GPS del móvil la posición cambia cada pocos segundos y la ruta se recalcula: el mapa de la ruta no tiene que volver a su
 * encuadre mientras el usuario lo arrastra o lo amplía con los dedos (en el emulador la posición no se mueve y esto no se notaba).
 */
class RutaGesturesTest {
    @get:Rule
    val rule = createComposeRule()

    private val item: ContentItem = FakeQueVer.first { it.id == "mercado-central" }

    private fun estado(jitter: Double) = RutaUiState(
        item = item,
        minutes = mapOf(TravelMode.Walk to 6, TravelMode.Bike to 3, TravelMode.Bus to 9, TravelMode.Metro to null),
        arrival = "10:42",
        meters = 484,
        status = RouteStatus.Ready,
        steps = listOf(RouteStep(StepKind.Straight, "Av. María Cristina", 300), RouteStep(StepKind.Arrive, item.title, null)),
        origin = LatLon(39.4690 + jitter, -0.3765),
        route = listOf(LatLon(39.4690 + jitter, -0.3765), item.location!!),
    )

    private fun abrirRuta(): Pair<MapCamera, androidx.compose.runtime.MutableState<RutaUiState>> {
        val camera = MapCamera()
        val state = mutableStateOf(estado(0.0))
        rule.setContent { TurismoValenciaTheme { RutaScreen(state.value, {}, {}, {}, {}, camera = camera) } }
        rule.waitUntil(10_000) { camera.view != null }
        rule.waitForIdle()
        Thread.sleep(800) // el primer encuadre de la ruta
        rule.waitForIdle()
        return camera to state
    }

    private fun zoom(camera: MapCamera): Double {
        var value = 0.0
        rule.runOnUiThread { value = camera.view!!.zoomLevelDouble }
        return value
    }

    private fun centro(camera: MapCamera): Pair<Double, Double> {
        var value = 0.0 to 0.0
        rule.runOnUiThread { camera.view!!.mapCenter.let { value = it.latitude to it.longitude } }
        return value
    }

    /** Posiciones nuevas cada 200 ms durante 2,4 s, como un GPS: cada una cambia el origen y, con él, la ruta. */
    private fun gps(state: androidx.compose.runtime.MutableState<RutaUiState>) = thread {
        repeat(12) { i ->
            state.value = estado((i + 1) * 0.00005)
            Thread.sleep(200)
        }
    }

    @Test
    fun ampliarConDosDedosSeMantieneAunqueLlegueUnaPosicionNueva() {
        val (camera, state) = abrirRuta()
        val antes = zoom(camera)
        val gps = gps(state)
        rule.onRoot().performTouchInput {
            pinch(
                start0 = Offset(width / 2f - 40f, height * 0.40f), end0 = Offset(width / 2f - 280f, height * 0.40f),
                start1 = Offset(width / 2f + 40f, height * 0.40f), end1 = Offset(width / 2f + 280f, height * 0.40f),
                durationMillis = 600,
            )
        }
        gps.join()
        rule.waitForIdle()
        Thread.sleep(600)
        rule.waitForIdle()
        val despues = zoom(camera)
        assertTrue("el zoom debería quedarse más cerca: $antes → $despues", despues > antes + 0.5)
    }

    @Test
    fun arrastrarElMapaSeMantieneAunqueLlegueUnaPosicionNueva() {
        val (camera, state) = abrirRuta()
        val (_, lonAntes) = centro(camera)
        val gps = gps(state)
        rule.onRoot().performTouchInput {
            swipe(start = Offset(width / 2f + 200f, height * 0.40f), end = Offset(width / 2f - 200f, height * 0.40f), durationMillis = 500)
        }
        gps.join()
        rule.waitForIdle()
        Thread.sleep(600)
        rule.waitForIdle()
        val (_, lonDespues) = centro(camera)
        assertTrue("el mapa debería seguir más al este: $lonAntes → $lonDespues", lonDespues > lonAntes + 0.0005)
    }

    @Test
    fun alAbrirLaRutaSeEncuadraUnaVez() {
        val (camera, _) = abrirRuta()
        // Origen y destino quedan a la vista: el zoom no es el de partida (15) sino el que los encuadra.
        assertTrue("debería haber encuadrado la ruta, zoom ${zoom(camera)}", zoom(camera) != 15.0)
        assertEquals(true, camera.view != null)
    }
}
