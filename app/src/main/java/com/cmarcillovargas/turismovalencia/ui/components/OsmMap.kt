package com.cmarcillovargas.turismovalencia.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.isDark
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.openUrl
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

private const val MinZoom = 11.0
private const val MaxZoom = 19.0
private const val DefaultZoom = 15.0
private const val CenterAnimationMillis = 600L
private const val ZoomAnimationMillis = 250L
private const val MapSettleMillis = 120L
private const val PinTouchDp = 48f
private const val SelectedTouchDp = 64f
private const val RouteDotDp = 5f
private const val RouteGapDp = 9f

// Norte, este, sur y oeste: el mapa no deja alejarse de la zona de Valencia.
private val ValenciaArea = BoundingBox(39.62, -0.10, 39.20, -0.60)

private const val CopyrightUrl = "https://www.openstreetmap.org/copyright"

/** Marcador del mapa: un lugar con su categoría, que da el color y el icono. */
data class MapMarker(val id: String, val point: LatLon, val category: Category, val title: String)

/** Dónde mira el mapa. Se puede mover desde fuera (`centerOn`, `zoomBy`, `fit`) aunque el mapa aún no esté en pantalla. */
@Stable
class MapCamera(private var center: LatLon = ValenciaCenter, private var zoom: Double = DefaultZoom) {
    /** El mapa de osmdroid, cuando ya existe; es estado para que lo que depende de él (la capa de accesibilidad) se componga después. */
    internal var view: MapView? by mutableStateOf(null)
        private set

    /** Sube cada vez que el mapa se mueve o cambia de zoom (y al tener tamaño), para recolocar lo que va sobre él. */
    internal var version: Int by mutableIntStateOf(0)
        private set

    internal fun moved() {
        version++
    }

    internal val initialCenter: LatLon get() = center
    internal val initialZoom: Double get() = zoom

    internal fun attach(view: MapView) {
        this.view = view
    }

    internal fun detach() {
        view?.let {
            val current = it.mapCenter
            center = LatLon(current.latitude, current.longitude)
            zoom = it.zoomLevelDouble
        }
        view = null
    }

    /** Centra el mapa en [point] (con animación, salvo que [animate] sea `false`); con [zoom] también cambia el nivel. */
    fun centerOn(point: LatLon, zoom: Double? = null, animate: Boolean = true) {
        center = point
        if (zoom != null) this.zoom = zoom
        val map = view ?: return
        if (animate) {
            map.controller.animateTo(point.toGeoPoint(), zoom ?: map.zoomLevelDouble, CenterAnimationMillis)
        } else {
            if (zoom != null) map.controller.setZoom(zoom)
            map.controller.setCenter(point.toGeoPoint())
        }
    }

    /** Acerca (positivo) o aleja (negativo) el mapa esos niveles de zoom. */
    fun zoomBy(delta: Double) {
        val map = view ?: return
        map.controller.zoomTo((map.zoomLevelDouble + delta).coerceIn(MinZoom, MaxZoom), ZoomAnimationMillis)
    }

    /**
     * Ajusta el mapa para que se vean todos los puntos en el hueco que dejan las capas de encima (cabecera arriba y hoja abajo,
     * en píxeles) y con [sidePx] a los lados, centrándolos en ese hueco.
     */
    fun fit(points: List<LatLon>, topPx: Int, bottomPx: Int, sidePx: Int) {
        if (points.isEmpty()) return
        val map = view ?: return
        val box = BoundingBox.fromGeoPointsSafe(points.map { it.toGeoPoint() })
        center = LatLon(box.centerLatitude, box.centerLongitude)
        map.post {
            val width = map.width
            val height = map.height
            val freeWidth = width - 2 * sidePx
            val freeHeight = height - topPx - bottomPx
            if (freeWidth <= 0 || freeHeight <= 0) return@post
            // Con el centro de la caja en el centro del mapa y su zoom ya puesto, se desplaza el centro del mapa lo justo para que
            // la caja quede en el centro del hueco libre.
            val zoom = MapView.getTileSystem().getBoundingBoxZoom(box, freeWidth, freeHeight).coerceIn(MinZoom, MaxZoom - 1.0)
            map.controller.setZoom(zoom)
            map.controller.setCenter(GeoPoint(box.centerLatitude, box.centerLongitude))
            val freeCenterY = topPx + freeHeight / 2
            map.controller.setCenter(map.projection.fromPixels(width / 2, height - freeCenterY))
        }
    }
}

@Composable
fun rememberMapCamera(center: LatLon = ValenciaCenter, zoom: Double = DefaultZoom): MapCamera = remember { MapCamera(center, zoom) }

/**
 * Mapa de OpenStreetMap (osmdroid): teselas, zoom y arrastre, marcadores por categoría, el punto del usuario y una ruta punteada.
 * Las teselas que se ven quedan en la caché del móvil y siguen viéndose sin conexión.
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    selectedId: String? = null,
    onMarkerClick: (String) -> Unit = {},
    user: LatLon? = null,
    route: List<LatLon> = emptyList(),
    camera: MapCamera = rememberMapCamera(),
    interactive: Boolean = true,
    showSelectedLabel: Boolean = true,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val colors = MaterialTheme.appColors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val categoryColors: Map<Category, Color> = Category.entries.associateWith { it.pinColor() }
    val renderer = remember(density, measurer, labelStyle, scheme.surface, scheme.onSurface, colors.location) {
        PinRenderer(density, measurer, PinStyle(labelStyle, scheme.onSurface, scheme.surface, colors.location))
    }
    val routeColor = colors.brand.toArgb()
    val dark = MaterialTheme.isDark
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)

    DisposableEffect(lifecycle, camera) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> camera.view?.onResume()
                Lifecycle.Event.ON_PAUSE -> camera.view?.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                OsmSetup.ensure(ctx) // antes de crear el primer mapa
                MapView(ctx).also { configure(it, camera, dark) }
            },
            update = { view ->
                view.setOnTouchListener(if (interactive) null else BlockTouches)
                view.render(markers, selectedId, user, route, renderer, categoryColors, routeColor, showSelectedLabel) { currentOnMarkerClick(it) }
            },
            onRelease = {
                camera.detach()
                it.onDetach()
            },
        )
        if (interactive) PinSemantics(camera, markers, selectedId) { currentOnMarkerClick(it) }
    }
}

/**
 * osmdroid pinta los marcadores como imágenes, que los lectores de pantalla no ven. Esta capa, invisible y sin recoger toques
 * (los toques llegan al mapa), pone sobre cada marcador un botón con el nombre del lugar para accesibilidad y para las pruebas.
 */
@Composable
private fun PinSemantics(camera: MapCamera, markers: List<MapMarker>, selectedId: String?, onSelect: (String) -> Unit) {
    val view = camera.view ?: return
    camera.version // se lee para recomponer cuando el mapa se mueve
    if (view.width == 0 || view.height == 0) return
    val projection = view.projection
    val density = LocalDensity.current.density
    Box(Modifier.fillMaxSize()) {
        markers.forEach { marker ->
            val point = projection.toPixels(marker.point.toGeoPoint(), null)
            val selected = marker.id == selectedId
            val width = ((if (selected) SelectedTouchDp else PinTouchDp) * density).toInt()
            val height = width
            // El normal tiene la punta en el punto del lugar; el seleccionado, el centro del círculo.
            val top = if (selected) point.y - height / 2 else point.y - height
            Box(
                Modifier
                    .offset { IntOffset(point.x - width / 2, top) }
                    .size(width.toDp(density), height.toDp(density))
                    .semantics(mergeDescendants = true) {
                        contentDescription = marker.title
                        role = Role.Button
                        this.selected = selected
                        onClick(label = null) {
                            onSelect(marker.id)
                            true
                        }
                    },
            )
        }
    }
}

private fun Int.toDp(density: Float) = (this / density).dp

private val BlockTouches = View.OnTouchListener { _, _ -> true }

/**
 * Teselas de OpenStreetMap en oscuro: invierte la luminosidad (el fondo beige pasa a oscuro y las calles blancas a casi negras con su
 * borde claro), gira el tono 180° para que el agua siga siendo azulada y los parques verdosos (es el truco `invert() hue-rotate(180deg)`
 * de CSS) y por último lo enfría y lo oscurece hacia el azul del tema oscuro (rojo ×0,55, verde ×0,80, azul ×1,05), que es el tono del mapa
 * de las imágenes de diseño con las calles aún legibles. La atribución de OpenStreetMap se mantiene.
 */
private val DarkTilesFilter: ColorMatrixColorFilter by lazy {
    val invert = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    val hue = ColorMatrix(
        floatArrayOf(
            -0.574f, 1.430f, 0.144f, 0f, 0f,
            0.426f, 0.430f, 0.144f, 0f, 0f,
            0.426f, 1.430f, -0.856f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    val cool = ColorMatrix(
        floatArrayOf(
            0.55f, 0f, 0f, 0f, 0f,
            0f, 0.80f, 0f, 0f, 0f,
            0f, 0f, 1.05f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    invert.postConcat(hue)
    invert.postConcat(cool)
    ColorMatrixColorFilter(invert)
}

private fun configure(view: MapView, camera: MapCamera, dark: Boolean) {
    view.setTileSource(TileSourceFactory.MAPNIK)
    // El tema oscuro cambia las teselas con un filtro de color (el servidor de OpenStreetMap solo sirve su estilo estándar, claro).
    if (dark) view.overlayManager.tilesOverlay.setColorFilter(DarkTilesFilter)
    view.setMultiTouchControls(true)
    view.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    view.minZoomLevel = MinZoom
    view.maxZoomLevel = MaxZoom
    view.isTilesScaledToDpi = true
    view.isHorizontalMapRepetitionEnabled = false
    view.isVerticalMapRepetitionEnabled = false
    view.setScrollableAreaLimitDouble(ValenciaArea)
    view.controller.setZoom(camera.initialZoom)
    view.controller.setCenter(camera.initialCenter.toGeoPoint())
    // El mapa cambia de tamaño después de componerse (aparece la barra inferior): lo que va sobre él se recoloca entonces.
    view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) camera.moved()
    }
    view.addMapListener(
        DelayedMapListener(
            object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    camera.moved()
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    camera.moved()
                    return false
                }
            },
            MapSettleMillis,
        ),
    )
    camera.attach(view)
}

private fun MapView.render(
    markers: List<MapMarker>,
    selectedId: String?,
    user: LatLon?,
    route: List<LatLon>,
    renderer: PinRenderer,
    categoryColors: Map<Category, Color>,
    routeColor: Int,
    showSelectedLabel: Boolean,
    onMarkerClick: (String) -> Unit,
) {
    overlays.clear()
    if (route.size >= 2) overlays.add(routeLine(route, routeColor))
    if (user != null) overlays.add(bitmapMarker(user, renderer.user(), PinAnchor(0.5f, 0.5f), PinHit.None, null))
    markers.filter { it.id != selectedId }.forEach { marker ->
        val bitmap = renderer.pin(categoryColors.getValue(marker.category), marker.category.icon())
        overlays.add(bitmapMarker(marker.point, bitmap, renderer.pinAnchor, renderer.pinHit) { onMarkerClick(marker.id) })
    }
    // El seleccionado va el último para que quede encima y reciba antes el toque.
    markers.firstOrNull { it.id == selectedId }?.let { marker ->
        val pin = renderer.selected(categoryColors.getValue(marker.category), marker.category.icon(), marker.title.takeIf { showSelectedLabel })
        overlays.add(bitmapMarker(marker.point, pin.bitmap, pin.anchor, pin.hit) { onMarkerClick(marker.id) })
    }
    invalidate()
}

private fun MapView.bitmapMarker(point: LatLon, bitmap: Bitmap, anchor: PinAnchor, hit: PinHit, onClick: (() -> Unit)?): Marker =
    PinMarker(this, hit).apply {
        position = point.toGeoPoint()
        icon = BitmapDrawable(resources, bitmap)
        setAnchor(anchor.x, anchor.y)
        setInfoWindow(null)
        setOnMarkerClickListener { _, _ ->
            onClick?.invoke()
            true
        }
    }

/**
 * Marcador que solo recoge el toque dentro de su forma ([PinHit]); fuera de ella deja pasar el toque a lo que haya debajo
 * (otros marcadores o el propio mapa), en vez de quedárselo por tener una imagen más grande que lo dibujado.
 */
private class PinMarker(map: MapView, private val hit: PinHit) : Marker(map) {
    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView): Boolean {
        val anchor = mapView.projection.toPixels(position, null)
        return hit.contains(event.x - anchor.x, event.y - anchor.y) && super.onSingleTapConfirmed(event, mapView)
    }
}

private fun MapView.routeLine(route: List<LatLon>, color: Int): Polyline {
    val density = resources.displayMetrics.density
    return Polyline(this).apply {
        setPoints(route.map { it.toGeoPoint() })
        outlinePaint.apply {
            this.color = color
            strokeWidth = RouteDotDp * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            pathEffect = DashPathEffect(floatArrayOf(0.1f, RouteGapDp * density), 0f)
        }
        setOnClickListener { _, _, _ -> true }
    }
}

private fun LatLon.toGeoPoint() = GeoPoint(lat, lon)

/** Configuración común de osmdroid, que hay que hacer antes de crear el primer mapa. */
private object OsmSetup {
    private const val UserAgent = "TurismoValencia/1.0 (Android; TFG DAM; https://github.com/cemarvar/turismo-valencia-app)"
    private const val CacheMaxBytes = 100L * 1024 * 1024
    private const val CacheTrimBytes = 80L * 1024 * 1024
    private var ready = false

    @Synchronized
    fun ensure(context: Context) {
        if (ready) return
        val app = context.applicationContext
        val config = Configuration.getInstance()
        config.load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        // OpenStreetMap exige identificar la app; sin esto sus servidores no devuelven teselas.
        config.userAgentValue = UserAgent
        // Caché en el almacenamiento privado de la app: no hace falta ningún permiso de almacenamiento.
        val base = File(app.filesDir, "osmdroid").apply { mkdirs() }
        config.osmdroidBasePath = base
        config.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
        config.tileFileSystemCacheMaxBytes = CacheMaxBytes
        config.tileFileSystemCacheTrimBytes = CacheTrimBytes
        ready = true
    }
}

/** Atribución obligatoria de los datos y las teselas de OpenStreetMap; abre la página de su licencia. */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable(role = Role.Button) { context.openUrl(CopyrightUrl) },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    ) {
        Text(
            text = stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
    }
}

@Composable
private fun Category.pinColor(): Color = when (this) {
    Category.QueVer -> MaterialTheme.appColors.navy
    Category.Gastronomia, Category.Eventos -> MaterialTheme.appColors.brand
    Category.Alojamientos -> MaterialTheme.appColors.lodging
    Category.Sostenible -> MaterialTheme.appColors.eco
}

/**
 * Color de la categoría en los puntos de los chips del mapa. «Qué ver» usa la pizarra de primer plano (`secondary`, igual que el
 * relleno del marcador en claro): en oscuro, la del marcador (`navy`) casi no se distinguiría sobre el chip.
 */
@Composable
fun Category.dotColor(): Color = if (this == Category.QueVer) MaterialTheme.colorScheme.secondary else pinColor()
