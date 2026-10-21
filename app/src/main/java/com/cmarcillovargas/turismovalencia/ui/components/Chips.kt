package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

enum class ChipStyle {
    /** Seleccionado en azul marino (filtros de las listas). */
    Navy,

    /** Seleccionado en naranja (categorías del inicio y del mapa). */
    Brand,
}

private val ChipBorder = 1.dp
private val ChipIconSize = 20.dp
private val SegmentedHeight = 40.dp
private val SegmentedPadding = 4.dp

/** Chip de filtro en píldora. */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Navy,
    leadingIcon: ImageVector? = null,
    dotColor: Color? = null,
    height: Dp = 36.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.appColors
    val container = when {
        !selected -> colors.surface
        style == ChipStyle.Navy -> app.navy
        else -> app.action
    }
    val content = when {
        !selected -> colors.secondary
        style == ChipStyle.Navy -> app.onNavy
        else -> app.onAction
    }
    Surface(
        modifier = modifier.height(height),
        shape = PillShape,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(ChipBorder, colors.outlineVariant),
        shadowElevation = if (selected && style == ChipStyle.Brand) Spacing.xxs else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .padding(horizontal = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (dotColor != null && !selected) {
                Box(Modifier.size(Spacing.s).clip(PillShape).background(dotColor))
                Spacer(Modifier.width(Spacing.xs))
            } else if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(text = label, style = textStyle)
        }
    }
}

enum class StatusTone { Success, Neutral, Warning, Brand, Eco, Danger }

/** Etiqueta pequeña de estado: «Sin conexión», «Confirmada», «Pendiente»… */
@Composable
fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    height: Dp = 24.dp,
    small: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.appColors
    val (container, content) = when (tone) {
        StatusTone.Success -> colors.tertiaryContainer to colors.onTertiaryContainer
        StatusTone.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        StatusTone.Warning -> app.warningContainer to app.warning
        StatusTone.Brand -> app.action to app.onAction
        StatusTone.Eco -> colors.tertiary to colors.onTertiary
        StatusTone.Danger -> colors.errorContainer to colors.onErrorContainer
    }
    Row(
        modifier = modifier
            .height(height)
            .clip(PillShape)
            .background(container)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(Spacing.m))
            Spacer(Modifier.width(Spacing.xxs))
        }
        Text(text = text, style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, color = content)
    }
}

/** Pestañas segmentadas en una píldora gris con la activa en blanco (Entradas/Tours/Experiencias, Próximas/Historial…). */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SegmentedHeight + SegmentedPadding * 2)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(SegmentedPadding)
            .selectableGroup(),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SegmentedHeight)
                    .let { if (selected) it.shadow(Spacing.xxs, PillShape) else it }
                    .clip(PillShape)
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
