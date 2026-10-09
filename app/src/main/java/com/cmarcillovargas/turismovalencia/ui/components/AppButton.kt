package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.ui.theme.MinTouchTarget
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing

enum class AppButtonStyle {
    /** Relleno primary con resplandor naranja: acción principal («Siguiente», «Aplicar»). */
    Primary,

    /** Relleno primaryContainer con texto onPrimaryContainer: acción secundaria destacada. */
    Tonal,

    /** Solo texto en primary: acciones de baja prioridad. */
    Plain,

    /** Borde de 2dp y texto en primary: acción secundaria junto a un botón principal («Guardar», «Calendario»). */
    Outlined,

    /** Borde y texto en rojo: acciones destructivas («Borrar todo»). */
    Danger,
}

private const val GlowAlpha = 1f
private val IconSize = 20.dp
private val CompactHeight = 40.dp
private val OutlineWidth = 2.dp

/** Botón en píldora de 48dp (alto del diseño). El ancho lo decide quien lo usa (`fillMaxWidth`, `weight`…). */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val sized = modifier.height(if (compact) CompactHeight else MinTouchTarget)
    val padding = if (compact) PaddingValues(horizontal = Spacing.m) else ButtonDefaults.ContentPadding
    // Los botones rellenos van en 15sp y el resto (tonal, texto y con borde) en 16sp, como en el diseño.
    val textStyle = if (style == AppButtonStyle.Primary) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
    }
    val content: @Composable RowScope.() -> Unit = {
        if (leadingIcon != null) {
            // Decorativo: el significado ya está en el texto del botón.
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(IconSize))
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text = text, style = textStyle)
        if (trailingIcon != null) {
            Spacer(Modifier.width(Spacing.xs))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(IconSize))
        }
    }
    when (style) {
        AppButtonStyle.Primary -> {
            val glow = colors.primary.copy(alpha = GlowAlpha)
            Button(
                onClick = onClick,
                modifier = sized.shadow(Spacing.xxs, PillShape, ambientColor = glow, spotColor = glow),
                enabled = enabled,
                shape = PillShape,
                contentPadding = padding,
                content = content,
            )
        }

        AppButtonStyle.Tonal -> FilledTonalButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = PillShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ),
            content = content,
        )

        AppButtonStyle.Plain -> TextButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = PillShape,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
            content = content,
        )

        AppButtonStyle.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = PillShape,
            border = BorderStroke(OutlineWidth, colors.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
            content = content,
        )

        AppButtonStyle.Danger -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = PillShape,
            border = BorderStroke(OutlineWidth, colors.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            content = content,
        )
    }
}
