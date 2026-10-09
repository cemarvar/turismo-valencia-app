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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.MapCanvas
import com.cmarcillovargas.turismovalencia.ui.components.MapMarker
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.theme.BottomSheetShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.formatDistance

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
    Box(modifier.fillMaxSize()) {
        MapCanvas(
            modifier = Modifier.fillMaxSize(),
            markers = listOf(MapMarker(item.id, item.mapPoint, item.mapCategory ?: item.category, item.title)),
            selectedId = item.id,
            showUser = true,
            route = uiState.route,
            showLabels = true,
            showSelectedLabel = false,
        )
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
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
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
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
                            text = stringResource(R.string.route_summary, formatDistance(item.distanceMeters), uiState.arrival),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                    StepKind.Straight -> stringResource(R.string.route_step_straight, step.street ?: stringResource(R.string.route_street_generic))
                    StepKind.Left -> stringResource(R.string.route_step_left, step.street ?: item.area)
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

/** Abre la app de mapas del móvil con la ruta al lugar (consulta por nombre; el mapa dibujado no tiene coordenadas reales). */
private fun startNavigation(context: Context, item: ContentItem, mode: TravelMode) {
    val letter = when (mode) {
        TravelMode.Walk -> "w"
        TravelMode.Bike -> "b"
        else -> "d"
    }
    val uri = Uri.parse("google.navigation:q=${Uri.encode(item.title + ", Valencia")}&mode=$letter")
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
                steps = listOf(
                    RouteStep(StepKind.Straight, "Av. María Cristina", 300),
                    RouteStep(StepKind.Left, "Pl. Ciudad de Brujas", 150),
                    RouteStep(StepKind.Arrive, item.title, null),
                ),
                route = listOf(
                    com.cmarcillovargas.turismovalencia.data.model.MapPoint(142f, 392f),
                    com.cmarcillovargas.turismovalencia.data.model.MapPoint(146f, 352f),
                    com.cmarcillovargas.turismovalencia.data.model.MapPoint(188f, 345f),
                    com.cmarcillovargas.turismovalencia.data.model.MapPoint(193f, 339f),
                ),
                saved = true,
            ),
            onBack = {}, onSelectMode = {}, onToggleSaved = {}, onStart = {},
        )
    }
}
