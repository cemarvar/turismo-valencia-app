package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Los gestos del mapa (arrastrar con un dedo y pellizcar con dos) llegan al mapa de osmdroid a través de Compose. */
class OsmMapGesturesTest {
    @get:Rule
    val rule = createComposeRule()

    private fun abrirMapa(): MapCamera {
        val camera = MapCamera()
        rule.setContent { TurismoValenciaTheme { OsmMap(Modifier.fillMaxSize(), camera = camera) } }
        rule.waitUntil(10_000) { camera.view != null }
        rule.waitForIdle()
        return camera
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

    @Test
    fun pellizcarConDosDedosAcercaElMapa() {
        val camera = abrirMapa()
        val antes = zoom(camera)
        rule.onRoot().performTouchInput {
            pinch(
                start0 = center + Offset(-40f, 0f), end0 = center + Offset(-300f, 0f),
                start1 = center + Offset(40f, 0f), end1 = center + Offset(300f, 0f),
                durationMillis = 600,
            )
        }
        rule.waitForIdle()
        val despues = zoom(camera)
        assertTrue("el zoom debería subir: $antes → $despues", despues > antes + 0.5)
    }

    @Test
    fun pellizcarConDosDedosAlejaElMapa() {
        val camera = abrirMapa()
        val antes = zoom(camera)
        rule.onRoot().performTouchInput {
            pinch(
                start0 = center + Offset(-300f, 0f), end0 = center + Offset(-40f, 0f),
                start1 = center + Offset(300f, 0f), end1 = center + Offset(40f, 0f),
                durationMillis = 600,
            )
        }
        rule.waitForIdle()
        val despues = zoom(camera)
        assertTrue("el zoom debería bajar: $antes → $despues", despues < antes - 0.5)
    }

    @Test
    fun arrastrarConUnDedoMueveElMapa() {
        val camera = abrirMapa()
        val (_, lonAntes) = centro(camera)
        rule.onRoot().performTouchInput {
            swipe(start = center + Offset(200f, 0f), end = center + Offset(-200f, 0f), durationMillis = 500)
        }
        rule.waitForIdle()
        val (_, lonDespues) = centro(camera)
        assertTrue("el mapa debería moverse hacia el este: $lonAntes → $lonDespues", lonDespues > lonAntes)
    }
}
