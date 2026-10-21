package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val FieldShape = RoundedCornerShape(16.dp)
private val FieldBorder = 2.dp

/**
 * Campo de texto del diseño (el del correo de F2): borde de 2dp en el naranja de marca, icono decorativo, etiqueta pequeña y el texto
 * debajo. [capitalization] y [autoCorrect] sirven para códigos como el localizador (mayúsculas y sin corrector); [onDone] es lo que
 * hace la tecla «Hecho» del teclado.
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ChatBubbleOutline,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    autoCorrect: Boolean = true,
    onDone: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val brand = MaterialTheme.appColors.brand
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .border(BorderStroke(FieldBorder, brand), FieldShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = brand)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(brand),
                keyboardOptions = KeyboardOptions(
                    capitalization = capitalization,
                    autoCorrectEnabled = autoCorrect,
                    keyboardType = keyboardType,
                    imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            )
        }
    }
}

@Preview(name = "Campo de texto", showBackground = true, widthDp = 390)
@Composable
private fun LabeledTextFieldPreview() {
    TurismoValenciaTheme {
        Column(Modifier.padding(Spacing.m)) {
            LabeledTextField("Correo para la confirmación", "laura.garcia@email.com", {}, keyboardType = KeyboardType.Email)
        }
    }
}
