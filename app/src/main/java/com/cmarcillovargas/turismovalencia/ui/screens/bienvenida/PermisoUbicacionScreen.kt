package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.Eyebrow
import com.cmarcillovargas.turismovalencia.ui.components.MapPlaceholder
import com.cmarcillovargas.turismovalencia.ui.components.Scrim
import com.cmarcillovargas.turismovalencia.ui.components.SystemBarIcons
import com.cmarcillovargas.turismovalencia.ui.theme.BottomSheetShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

// Medidas del diseño (A5).
private val LocationIconSize = 28.dp
private val AccuracyCardHeight = 104.dp
private val AccuracyMarkSize = 48.dp
private val SelectedBorder = 2.dp
private val RestingBorder = 1.dp
private const val PreciseFillAlpha = 0.25f

@Composable
fun PermisoUbicacionRoute(onContinue: () -> Unit, viewModel: PermisoUbicacionViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // TODO(permisos): pedir el permiso real según la precisión elegida («mientras se usa» / «solo esta vez» /
    // «no permitir») y recordar la decisión. Hoy las tres opciones solo avanzan.
    PermisoUbicacionScreen(
        uiState = uiState,
        onAccuracyChange = viewModel::onAccuracyChange,
        onWhileUsing = onContinue,
        onOnlyOnce = onContinue,
        onDeny = onContinue,
    )
}

@Composable
fun PermisoUbicacionScreen(
    uiState: PermisoUbicacionUiState,
    onAccuracyChange: (LocationAccuracy) -> Unit,
    onWhileUsing: () -> Unit,
    onOnlyOnce: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarIcons(lightStatusBarIcons = true, lightNavigationBarIcons = true)
    Box(modifier.fillMaxSize()) {
        MapPlaceholder()
        ExplanationPanel(Modifier.align(Alignment.BottomCenter))
        Scrim()
        PermissionDialog(
            uiState = uiState,
            onAccuracyChange = onAccuracyChange,
            onWhileUsing = onWhileUsing,
            onOnlyOnce = onOnlyOnce,
            onDeny = onDeny,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.l),
        )
    }
}

@Composable
private fun ExplanationPanel(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // El diseño deja 32dp bajo el texto; el indicador de gestos cae dentro de ese hueco.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, BottomSheetShape)
            .padding(start = Spacing.l, top = Spacing.l, end = Spacing.l, bottom = maxOf(bottomInset, Spacing.xl)),
    ) {
        Eyebrow(text = stringResource(R.string.permiso_panel_etiqueta))
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.permiso_panel_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.permiso_panel_texto),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionDialog(
    uiState: PermisoUbicacionUiState,
    onAccuracyChange: (LocationAccuracy) -> Unit,
    onWhileUsing: () -> Unit,
    onOnlyOnce: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val appName = stringResource(R.string.app_name)
    val dialogTitle = stringResource(R.string.permiso_dialogo_titulo, appName)
    val title = buildAnnotatedString {
        append(dialogTitle)
        val start = dialogTitle.indexOf(appName)
        if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + appName.length)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface,
        shadowElevation = Spacing.xs,
    ) {
        Column(Modifier.padding(Spacing.l), horizontalAlignment = Alignment.CenterHorizontally) {
            // Decorativo: el título del diálogo ya dice de qué permiso se trata.
            Icon(
                painter = painterResource(R.drawable.ic_my_location),
                contentDescription = null,
                modifier = Modifier.size(LocationIconSize),
                tint = MaterialTheme.appColors.brand,
            )
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = Spacing.xxs),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                AccuracyCard(
                    label = stringResource(R.string.permiso_precisa),
                    accuracy = LocationAccuracy.Precise,
                    selected = uiState.accuracy == LocationAccuracy.Precise,
                    onClick = { onAccuracyChange(LocationAccuracy.Precise) },
                    modifier = Modifier.weight(1f),
                )
                AccuracyCard(
                    label = stringResource(R.string.permiso_aproximada),
                    accuracy = LocationAccuracy.Approximate,
                    selected = uiState.accuracy == LocationAccuracy.Approximate,
                    onClick = { onAccuracyChange(LocationAccuracy.Approximate) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.m))
            AppButton(
                text = stringResource(R.string.permiso_mientras_se_usa),
                onClick = onWhileUsing,
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.Tonal,
            )
            AppButton(
                text = stringResource(R.string.permiso_solo_esta_vez),
                onClick = onOnlyOnce,
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.Plain,
            )
            AppButton(
                text = stringResource(R.string.permiso_no_permitir),
                onClick = onDeny,
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.Plain,
            )
        }
    }
}

@Composable
private fun AccuracyCard(
    label: String,
    accuracy: LocationAccuracy,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val appColors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .height(AccuracyCardHeight)
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(
                width = if (selected) SelectedBorder else RestingBorder,
                color = if (selected) appColors.brand else appColors.controlInactive,
                shape = shape,
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AccuracyMark(accuracy)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
    }
}

/** Ilustración de la opción: círculo azul con aro (precisa) o círculo gris liso (aproximada). */
@Composable
private fun AccuracyMark(accuracy: LocationAccuracy) {
    val location = MaterialTheme.appColors.location
    val mark = Modifier.size(AccuracyMarkSize).clip(CircleShape)
    when (accuracy) {
        LocationAccuracy.Precise -> Box(
            mark
                .background(location.copy(alpha = PreciseFillAlpha))
                .border(SelectedBorder, location, CircleShape),
        )

        LocationAccuracy.Approximate -> Box(mark.background(MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

@Preview(name = "A5 · Permiso (precisa)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermisoPrecisaPreview() = PermisoPreview(LocationAccuracy.Precise)

@Preview(name = "A5 · Permiso (aproximada)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermisoAproximadaPreview() = PermisoPreview(LocationAccuracy.Approximate)

@Composable
private fun PermisoPreview(accuracy: LocationAccuracy) {
    TurismoValenciaTheme {
        PermisoUbicacionScreen(
            uiState = PermisoUbicacionUiState(accuracy),
            onAccuracyChange = {},
            onWhileUsing = {},
            onOnlyOnce = {},
            onDeny = {},
        )
    }
}
