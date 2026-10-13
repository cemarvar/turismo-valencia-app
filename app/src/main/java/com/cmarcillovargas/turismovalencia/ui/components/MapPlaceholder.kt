package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

// El fondo se dibuja sobre un lienzo de 700×1100 «dp de mapa»; a escala 1 una pantalla de 390 dp enseña el centro.
private const val WorldWidth = 700f
private const val WorldHeight = 1100f
private const val CellWidth = 52f
private const val CellHeight = 50f
private const val RiverStart = 345f
private const val RiverEnd = 395f

/**
 * Fondo con aspecto de plano (manzanas, calles, parque y río) para las pantallas que solo necesitan «un mapa» de adorno y no
 * deben pedir red: el permiso de ubicación y Contacto. El mapa de verdad, con teselas de OpenStreetMap, es [OsmMap].
 */
@Composable
fun MapPlaceholder(modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val colors = MaterialTheme.appColors
    Canvas(modifier.fillMaxSize().clipToBounds().clearAndSetSemantics {}) {
        withTransform({ scale(density, density, pivot = Offset.Zero) }) {
            drawWorld(colors.mapLand, colors.mapBuilding, colors.mapRoad, colors.mapRoadMajor, colors.mapRoadWarm, colors.mapPark, colors.mapWater)
        }
    }
}

private fun DrawScope.drawWorld(
    land: Color,
    building: Color,
    road: Color,
    majorRoad: Color,
    warmRoad: Color,
    park: Color,
    water: Color,
) {
    drawRect(land, size = Size(WorldWidth, WorldHeight))
    val cols = (WorldWidth / CellWidth).toInt() + 1
    val rows = (WorldHeight / CellHeight).toInt() + 1
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if ((r * 7 + c * 3) % 5 == 0) continue
            drawRoundRect(building, Offset(c * CellWidth + 8f, r * CellHeight + 8f), Size(CellWidth - 16f, CellHeight - 16f), CornerRadius(3f))
        }
    }
    // Calles blancas: verticales principales y dos diagonales.
    listOf(35f, 100f, 210f, 270f, 335f, 440f, 520f, 600f).forEach { x ->
        drawLine(road, Offset(x, 0f), Offset(x, WorldHeight), strokeWidth = if (x == 210f) 12f else 7f)
    }
    drawLine(road, Offset(0f, 330f), Offset(RiverStart, 270f), strokeWidth = 13f)
    drawLine(road, Offset(0f, 447f), Offset(RiverStart, 386f), strokeWidth = 10f)
    drawLine(road, Offset(RiverEnd, 270f), Offset(WorldWidth, 330f), strokeWidth = 13f)
    drawLine(road, Offset(RiverEnd, 386f), Offset(WorldWidth, 447f), strokeWidth = 10f)
    // Avenidas principales.
    drawLine(road, Offset(0f, 120f), Offset(WorldWidth, 262f), strokeWidth = 18f)
    drawLine(majorRoad, Offset(0f, 120f), Offset(WorldWidth, 262f), strokeWidth = 13f)
    drawLine(road, Offset(0f, 520f), Offset(WorldWidth, 380f), strokeWidth = 16f)
    drawLine(warmRoad, Offset(0f, 520f), Offset(WorldWidth, 380f), strokeWidth = 11f)
    // Parque diagonal del antiguo cauce.
    drawPath(
        Path().apply {
            moveTo(0f, 160f)
            lineTo(70f, 185f)
            lineTo(RiverStart, 400f)
            lineTo(RiverStart, 570f)
            lineTo(250f, 560f)
            lineTo(0f, 340f)
            close()
        },
        park,
    )
    drawPath(
        Path().apply {
            moveTo(RiverEnd, 400f); lineTo(RiverEnd, 570f); lineTo(520f, 640f); lineTo(700f, 640f); lineTo(700f, 420f); close()
        },
        park,
    )
    // Río con sus orillas.
    drawRect(water, Offset(RiverStart, 0f), Size(RiverEnd - RiverStart, WorldHeight))
    drawRect(majorRoad, Offset(RiverStart - 4f, 0f), Size(4f, WorldHeight))
    drawRect(majorRoad, Offset(RiverEnd, 0f), Size(4f, WorldHeight))
}
