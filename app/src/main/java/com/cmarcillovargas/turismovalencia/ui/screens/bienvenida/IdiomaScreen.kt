package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.Scrim
import com.cmarcillovargas.turismovalencia.ui.components.SystemBarIcons
import com.cmarcillovargas.turismovalencia.ui.components.scrimColor
import com.cmarcillovargas.turismovalencia.ui.theme.BottomSheetShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.ThemeShowcase
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

// Medidas del diseño (A6).
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
private val RowHeight = 64.dp
private val AvatarSize = 40.dp
private val RadioSize = 24.dp
private val RadioStroke = 2.dp
private val RadioDotSize = 12.dp

/** Muestra el selector de idioma como bottom sheet cuando `visible`; lo usan el inicio y los ajustes. */
@Composable
fun IdiomaSheetHost(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return
    val viewModel: IdiomaViewModel = viewModel(factory = containerFactory { IdiomaViewModel(it.settings) })
    LaunchedEffect(Unit) { viewModel.reset() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IdiomaScreen(
        uiState = uiState,
        onSelect = viewModel::onSelect,
        onApply = {
            viewModel.apply()
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

/** El idioma se elige en un bottom sheet modal que, en el diseño, se abre sobre Explorar desde el chip «ES». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdiomaScreen(
    uiState: IdiomaUiState,
    onSelect: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    SystemBarIcons(lightStatusBarIcons = true, lightNavigationBarIcons = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = scrimColor(),
        dragHandle = { DragHandle() },
        contentWindowInsets = { WindowInsets(0.dp) }, // La barra de navegación la gestiona IdiomaSheetContent.
    ) {
        IdiomaSheetContent(uiState = uiState, onSelect = onSelect, onApply = onApply)
    }
}

@Composable
private fun DragHandle() {
    Box(
        Modifier
            .padding(top = Spacing.xs, bottom = Spacing.xs)
            .size(width = HandleWidth, height = HandleHeight)
            .clip(CircleShape)
            .background(MaterialTheme.appColors.controlInactive),
    )
}

@Composable
fun IdiomaSheetContent(
    uiState: IdiomaUiState,
    onSelect: (String) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // El diseño deja 24dp bajo el botón (el indicador de gestos cae dentro de ese hueco).
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.m).padding(bottom = maxOf(bottomInset, Spacing.l))) {
        Text(
            text = stringResource(R.string.idioma_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(R.string.idioma_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.s))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            uiState.options.forEach { option ->
                LanguageRow(
                    option = option,
                    selected = option.code == uiState.selectedCode,
                    onClick = { onSelect(option.code) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        AppButton(
            text = stringResource(R.string.idioma_aplicar),
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LanguageRow(option: IdiomaOption, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(AvatarSize).clip(CircleShape).background(colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.code,
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondary,
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = Spacing.s)) {
            Text(
                text = stringResource(option.name),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(option.detail),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        RadioMark(selected)
    }
}

/** Radio del diseño: aro de 24dp, con punto central cuando está seleccionado. El estado lo comunica la fila. */
@Composable
private fun RadioMark(selected: Boolean) {
    val appColors = MaterialTheme.appColors
    val ringColor = if (selected) appColors.brand else appColors.controlInactive
    Canvas(Modifier.size(RadioSize).clearAndSetSemantics {}) {
        val stroke = RadioStroke.toPx()
        drawCircle(
            color = ringColor,
            radius = (size.minDimension - stroke) / 2,
            center = Offset(size.width / 2, size.height / 2),
            style = Stroke(width = stroke),
        )
        if (selected) drawCircle(color = appColors.brand, radius = RadioDotSize.toPx() / 2)
    }
}

@Preview(name = "A6 · Idioma (ES)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IdiomaEspanolPreview() = IdiomaPreview(selectedCode = "ES")

@Preview(name = "A6 · Idioma (EN)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IdiomaInglesPreview() = IdiomaPreview(selectedCode = "EN")

/** El `ModalBottomSheet` abre su propia ventana y no se ve en el preview: se reproduce el sheet sobre el velo. */
@Composable
private fun IdiomaPreview(selectedCode: String) {
    TurismoValenciaTheme {
        Box(Modifier.fillMaxSize()) {
            ThemeShowcase()
            Scrim()
            Column(Modifier.align(Alignment.BottomCenter).background(MaterialTheme.colorScheme.surface, BottomSheetShape)) {
                DragHandle()
                IdiomaSheetContent(
                    uiState = IdiomaUiState(options = IdiomaOptions, selectedCode = selectedCode),
                    onSelect = {},
                    onApply = {},
                )
            }
        }
    }
}
