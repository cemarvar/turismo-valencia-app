package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val EmptyCircleSize = 112.dp
private val EmptyIconSize = 44.dp
private val SwitchIconSize = 16.dp

/** Estado vacío: círculo suave con icono, título, texto y una acción opcional. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(EmptyCircleSize)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.appColors.brandSoft.copy(alpha = 0.7f), MaterialTheme.appColors.brandSoft)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(EmptyIconSize))
        }
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(Spacing.m))
            AppButton(text = actionText, onClick = onAction, leadingIcon = actionIcon)
        }
    }
}

/** Bloque gris que late suavemente mientras carga el contenido. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

/** Interruptor naranja con una marca en el pulgar cuando está activo. */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val brand = MaterialTheme.appColors.brand
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = if (checked) {
            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(SwitchIconSize)) }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.surface,
            checkedTrackColor = brand,
            checkedBorderColor = brand,
            checkedIconColor = brand,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.appColors.controlInactive,
        ),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Eyebrow(
        text = text,
        modifier = modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
        color = MaterialTheme.colorScheme.outline,
    )
}
