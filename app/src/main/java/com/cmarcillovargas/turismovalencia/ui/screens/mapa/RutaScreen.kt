package com.cmarcillovargas.turismovalencia.ui.screens.mapa

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Subway
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.RouteStep
import com.cmarcillovargas.turismovalencia.data.model.StepKind
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.MapAttribution
import com.cmarcillovargas.turismovalencia.ui.components.MapMarker
import com.cmarcillovargas.turismovalencia.ui.components.OnScreenVisible
import com.cmarcillovargas.turismovalencia.ui.components.OsmMap
import com.cmarcillovargas.turismovalencia.ui.components.rememberMapCamera
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.theme.BottomSheetShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.formatDistance

// Márgenes (px) al encuadrar la ruta: arriba cabe el marcador seleccionado (círculo de 64 dp), abajo el punto de ubicación con su halo.
private const val FitTopMarginPx = 170
private const val FitBottomMarginPx = 110
private const val FitSideMarginPx = 110
private val FieldShape = RoundedCornerShape(16.dp)
private val StepIconSize = Spacing.xl
private val ModeShape = RoundedCornerShape(16.dp)

@Composable
fun RutaRoute(
    id: String,
    onBack: () -> Unit,
    viewModel: RutaViewModel = viewModel(key = "ruta-$id", factory = containerFactory { RutaViewModel(it, id) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    OnScreenVisible(onStart = viewModel::onVisible, onStop = viewModel::onHidden)
    RutaScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectMode = viewModel::onSelectMode,
        onToggleSaved = viewModel::onToggleSaved,
        onStart = { uiState.item?.let { startNavigation(context, it, uiState.mode) } },
    )
}

@Composable
fun RutaScreen(
    uiState: RutaUiState,
    onBack: () -> Unit,
    onSelectMode: (TravelMode) -> Unit,
    onToggleSaved: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = uiState.item ?: return
    // Encuadra la ruta (origen y destino) cuando ya se conoce.
    val camera = rememberMapCamera()
    var headerPx by remember { mutableIntStateOf(0) }
    var sheetPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.route, headerPx, sheetPx) {
        if (uiState.route.size >= 2 && headerPx > 0 && sheetPx > 0) {
            camera.fit(uiState.route, topPx = headerPx + FitTopMarginPx, bottomPx = sheetPx + FitBottomMarginPx, sidePx = FitSideMarginPx)
        }
    }
    Box(modifier.fillMaxSize()) {
        val marker = item.location?.let { MapMarker(item.id, it, item.mapCategory ?: item.category, item.title) }
        OsmMap(
            modifier = Modifier.fillMaxSize(),
            markers = listOfNotNull(marker),
            selectedId = item.id,
            user = uiState.origin,
            route = uiState.route,
            camera = camera,
            showSelectedLabel = false,
        )
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().onSizeChanged { headerPx = it.height },
            shape = RoundedCornerShape(bottomStart = Spacing.l, bottomEnd = Spacing.l),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Spacing.xs,
        ) {
            Column(Modifier.statusBarsPadding().padding(bottom = Spacing.s)) {
                Row(Modifier.padding(end = Spacing.m, top = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back), tint = MaterialTheme.colorScheme.secondary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        RouteField(stringResource(R.string.route_your_location)) {
                            Box(Modifier.size(Spacing.s).clip(CircleShape).background(MaterialTheme.appColors.location))
                        }
                        RouteField(item.title) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l - Spacing.xxs))
                        }
                    }
                }
                ModeTabs(uiState, onSelectMode, Modifier.padding(horizontal = Spacing.m).padding(top = Spacing.s))
            }
        }
        MapAttribution(Modifier.align(Alignment.BottomStart).padding(start = Spacing.xs, bottom = with(LocalDensity.current) { sheetPx.toDp() } + Spacing.xxs))
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { sheetPx = it.height },
            shape = BottomSheetShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = Spacing.s,
        ) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.m).padding(top = Spacing.xs, bottom = Spacing.m)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(32.dp, 4.dp).clip(CircleShape).background(MaterialTheme.appColors.controlInactive))
                Spacer(Modifier.height(Spacing.s))
                val minutes = uiState.minutes[uiState.mode]
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (minutes != null) stringResource(R.string.route_minutes, minutes) else stringResource(R.string.route_mode_metro),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.route_summary, formatDistance(uiState.meters), uiState.arrival),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        when (uiState.status) {
                            RouteStatus.Loading -> Text(stringResource(R.string.route_loading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            RouteStatus.Estimated -> Text(stringResource(R.string.route_estimated), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            RouteStatus.Ready -> Unit
                        }
                    }
                    StatusChip(
                        text = stringResource(if (uiState.saved) R.string.route_saved else R.string.route_save),
                        tone = if (uiState.saved) StatusTone.Success else StatusTone.Neutral,
                        icon = Icons.Outlined.Download,
                        onClick = onToggleSaved,
                        height = Spacing.l,
                        small = true,
                    )
                }
                Spacer(Modifier.height(Spacing.s))
                uiState.steps.forEach { step -> StepRow(step, item) }
                Spacer(Modifier.height(Spacing.xs))
                AppButton(stringResource(R.string.route_start), onStart, Modifier.fillMaxWidth(), leadingIcon = Icons.Outlined.NearMe)
            }
        }
    }
}

@Composable
private fun RouteField(text: String, leading: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.xxl - Spacing.xs)
            .clip(FieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun ModeTabs(uiState: RutaUiState, onSelectMode: (TravelMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup()) {
        TravelMode.entries.forEach { mode ->
            val selected = mode == uiState.mode
            val minutes = uiState.minutes[mode]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(ModeShape)
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelectMode(mode) })
                    .padding(vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    mode.icon(),
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.l),
                )
                Text(
                    text = if (minutes != null) stringResource(R.string.route_minutes, minutes) else stringResource(R.string.route_mode_metro),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepRow(step: RouteStep, item: ContentItem) {
    val last = step.kind == StepKind.Arrive
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(StepIconSize)
                .clip(CircleShape)
                .background(if (last) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (step.kind) {
                    StepKind.Straight -> Icons.AutoMirrored.Outlined.ArrowForward
                    StepKind.Left -> Icons.Outlined.ChevronLeft
                    StepKind.Right -> Icons.Outlined.ChevronRight
                    StepKind.Arrive -> Icons.Outlined.LocationOn
                },
                contentDescription = null,
                tint = if (last) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.l - Spacing.xxs),
            )
        }
        Spacer(Modifier.width(Spacing.s))
        Column {
            Text(
                text = when (step.kind) {
                    StepKind.Straight -> step.street?.let { stringResource(R.string.route_step_straight, it) } ?: stringResource(R.string.route_step_straight_plain)
                    StepKind.Left -> step.street?.let { stringResource(R.string.route_step_left, it) } ?: stringResource(R.string.route_step_left_plain)
                    StepKind.Right -> step.street?.let { stringResource(R.string.route_step_right, it) } ?: stringResource(R.string.route_step_right_plain)
                    StepKind.Arrive -> stringResource(R.string.route_arrived, item.title)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = step.meters?.let { formatDistance(it) } ?: stringResource(R.string.route_entrance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun TravelMode.icon(): ImageVector = when (this) {
    TravelMode.Walk -> Icons.AutoMirrored.Outlined.DirectionsWalk
    TravelMode.Bike -> Icons.Outlined.PedalBike
    TravelMode.Bus -> Icons.Outlined.DirectionsBus
    TravelMode.Metro -> Icons.Outlined.Subway
}

/** Abre la app de mapas del móvil con la navegación hasta las coordenadas del lugar. */
private fun startNavigation(context: Context, item: ContentItem, mode: TravelMode) {
    val letter = when (mode) {
        TravelMode.Walk -> "w"
        TravelMode.Bike -> "b"
        else -> "d"
    }
    val destination = item.location?.let { "${it.lat},${it.lon}" } ?: Uri.encode(item.title + ", Valencia")
    val uri = Uri.parse("google.navigation:q=$destination&mode=$letter")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.route_no_maps, Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "C2 · Cómo llegar", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RutaPreview() {
    TurismoValenciaTheme {
        val item = FakeQueVer.first { it.id == "mercado-central" }
        RutaScreen(
            uiState = RutaUiState(
                item = item,
                minutes = mapOf(TravelMode.Walk to 6, TravelMode.Bike to 3, TravelMode.Bus to 9, TravelMode.Metro to null),
                arrival = "10:42",
                meters = 484,
                status = RouteStatus.Ready,
                steps = listOf(
                    RouteStep(StepKind.Straight, "Av. María Cristina", 300),
                    RouteStep(StepKind.Left, "Pl. Ciudad de Brujas", 150),
                    RouteStep(StepKind.Arrive, item.title, null),
                ),
                route = listOf(ValenciaCenter, LatLon(39.4735, -0.3789)),
                saved = true,
            ),
            onBack = {}, onSelectMode = {}, onToggleSaved = {}, onStart = {},
        )
    }
}
