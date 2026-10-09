package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.MapPoint
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import kotlin.math.min
import kotlin.math.roundToInt

// El mapa se dibuja sobre un lienzo de 700×1100 «dp de mapa»; a zoom 1 una pantalla de 390 dp enseña el centro.
private const val WorldWidth = 700f
private const val WorldHeight = 1100f
private const val MinZoom = 0.7f
private const val MaxZoom = 2.4f
private const val CellWidth = 52f
private const val CellHeight = 50f
private const val RiverStart = 345f
private const val RiverEnd = 395f

/** Posición del usuario en el mapa dibujado. */
val UserLocation = MapPoint(142f, 392f)

data class MapMarker(val id: String, val point: MapPoint, val category: Category, val title: String)

@Stable
class MapCamera {
    var zoom by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var viewport by mutableStateOf(Size.Zero)

    fun transform(centroid: Offset, pan: Offset, zoomChange: Float, density: Float) {
        val newZoom = (zoom * zoomChange).coerceIn(MinZoom, MaxZoom)
        val ratio = newZoom / zoom
        zoom = newZoom
        offset = clamp(centroid - (centroid - offset) * ratio + pan, density)
    }

    fun zoomBy(factor: Float, density: Float) {
        transform(Offset(viewport.width / 2, viewport.height / 2), Offset.Zero, factor, density)
    }

    fun centerOn(point: MapPoint, density: Float) {
        val s = zoom * density
        offset = clamp(Offset(viewport.width / 2 - point.x * s, viewport.height / 2 - point.y * s), density)
    }

    private fun clamp(o: Offset, density: Float): Offset {
        val width = WorldWidth * zoom * density
        val height = WorldHeight * zoom * density
        return Offset(
            o.x.coerceIn(min(0f, viewport.width - width), 0f),
            o.y.coerceIn(min(0f, viewport.height - height), 0f),
        )
    }
}

@Composable
fun rememberMapCamera(): MapCamera = remember { MapCamera() }

/**
 * Mapa de la ciudad dibujado con manzanas, calles, parque y río, con zoom y arrastre, marcadores por categoría,
 * la posición del usuario y una ruta punteada. Es un dibujo, no un mapa real.
 */
// TODO(mapa): sustituir por el mapa real (osmdroid) cuando llegue su fase; la API de este componente se mantiene.
@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    selectedId: String? = null,
    onMarkerClick: (String) -> Unit = {},
    showUser: Boolean = false,
    route: List<MapPoint> = emptyList(),
    camera: MapCamera = rememberMapCamera(),
    interactive: Boolean = true,
    showLabels: Boolean = true,
    showSelectedLabel: Boolean = true,
) {
    val density = LocalDensity.current.density
    val colors = MaterialTheme.appColors
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.outline
    val parkLabel = MaterialTheme.colorScheme.tertiary
    val userColor = colors.location
    val routeColor = colors.brand

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { camera.viewport = Size(it.width.toFloat(), it.height.toFloat()) }
            .let {
                if (interactive) {
                    it.pointerInput(density) {
                        detectTransformGestures { centroid, pan, zoom, _ -> camera.transform(centroid, pan, zoom, density) }
                    }
                } else {
                    it
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val s = camera.zoom * density
            withTransform({
                translate(camera.offset.x, camera.offset.y)
                scale(s, s, pivot = Offset.Zero)
            }) {
                drawWorld(colors.mapLand, colors.mapBuilding, colors.mapRoad, colors.mapRoadMajor, colors.mapRoadWarm, colors.mapPark, colors.mapWater)
                if (route.size >= 2) drawRoute(route, routeColor)
                if (showUser) drawUser(UserLocation, userColor)
            }
            if (showLabels) drawLabels(measurer, camera, s, labelColor, parkLabel)
        }
        val s = camera.zoom * density
        markers.filter { it.id != selectedId }.forEach { marker ->
            MapPin(marker, false, onMarkerClick, s, camera.offset)
        }
        markers.firstOrNull { it.id == selectedId }?.let { marker -> MapPin(marker, true, onMarkerClick, s, camera.offset, showSelectedLabel) }
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

private fun DrawScope.drawRoute(points: List<MapPoint>, color: Color) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(0.1f, 9f))))
}

private fun DrawScope.drawUser(point: MapPoint, color: Color) {
    val center = Offset(point.x, point.y)
    drawCircle(color.copy(alpha = 0.15f), radius = 43f, center = center)
    drawCircle(color.copy(alpha = 0.45f), radius = 43f, center = center, style = Stroke(width = 1f))
    drawCircle(Color.White, radius = 7f, center = center)
    drawCircle(color, radius = 5f, center = center)
}

private fun DrawScope.drawLabels(measurer: TextMeasurer, camera: MapCamera, scale: Float, color: Color, parkColor: Color) {
    fun label(text: String, x: Float, y: Float, tint: Color, angle: Float = 0f) {
        val style = TextStyle(color = tint, fontSize = (13f * camera.zoom).sp, fontWeight = FontWeight.SemiBold)
        val layout = measurer.measure(text, style)
        val origin = Offset(x * scale + camera.offset.x, y * scale + camera.offset.y)
        if (origin.x < -400f || origin.y < -100f || origin.x > size.width + 400f || origin.y > size.height + 100f) return
        rotate(angle, origin) { drawText(layout, topLeft = origin) }
    }
    label("Ciutat Vella", 150f, 250f, color)
    label("El Carme", 200f, 362f, color)
    label("Jardí del Túria", 22f, 245f, parkColor, 30f)
}

/** Marcador: círculo de color con el icono de su categoría; el seleccionado crece y lleva su nombre debajo. */
@Composable
private fun MapPin(marker: MapMarker, selected: Boolean, onClick: (String) -> Unit, scale: Float, offset: Offset, showLabel: Boolean = true) {
    val density = LocalDensity.current.density
    val x = (marker.point.x * scale + offset.x) / density
    val y = (marker.point.y * scale + offset.y) / density
    if (x < -80f || y < -100f) return
    val color = marker.category.pinColor()
    if (selected) {
        Box(
            modifier = Modifier
                .offset { IntOffset(((x - SelectedBoxWidth / 2) * density).roundToInt(), ((y - SelectedCircle / 2) * density).roundToInt()) }
                .size(SelectedBoxWidth.dp, (SelectedCircle + 44).dp)
                .clickable { onClick(marker.id) },
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(SelectedCircle.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(marker.category.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.size(Spacing.xl + Spacing.xxs))
                }
                if (showLabel) Surface(
                    modifier = Modifier.padding(top = Spacing.xxs),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.s),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = Spacing.xxs,
                ) {
                    Text(
                        text = marker.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xxs),
                        maxLines = 1,
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .offset { IntOffset(((x - PinWidth / 2) * density).roundToInt(), ((y - PinHeight) * density).roundToInt()) }
                .width(PinWidth.dp)
                .height(PinHeight.dp)
                .clickable { onClick(marker.id) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(PinCircle.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(marker.category.icon(), contentDescription = marker.title, tint = Color.White, modifier = Modifier.size(Spacing.l - Spacing.xxs))
            }
            Canvas(Modifier.size(12.dp, 6.dp)) {
                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, color)
            }
        }
    }
}

private const val PinCircle = 36
private const val PinWidth = 48
private const val PinHeight = 44
private const val SelectedCircle = 64
private const val SelectedBoxWidth = 180

@Composable
private fun Category.pinColor(): Color = when (this) {
    Category.QueVer -> MaterialTheme.colorScheme.secondary
    Category.Gastronomia, Category.Eventos -> MaterialTheme.appColors.brand
    Category.Alojamientos -> MaterialTheme.appColors.lodging
    Category.Sostenible -> MaterialTheme.colorScheme.tertiary
}

/** Color de la categoría en los puntos de los chips del mapa. */
@Composable
fun Category.dotColor(): Color = pinColor()

/** Fondo de mapa sin interacción (por ejemplo, tras el permiso de ubicación). */
@Composable
fun MapPlaceholder(modifier: Modifier = Modifier) {
    MapCanvas(modifier.fillMaxSize(), interactive = false, showLabels = false)
}
