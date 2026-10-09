package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

// Solo desarrollo: vista previa para comprobar la fidelidad del tema contra Figma.
// Los textos de muestra no van a strings.xml porque nunca se muestran al usuario.
@Preview(name = "Theme", showBackground = true, widthDp = 390, heightDp = 1700)
@Composable
private fun ThemePreview() {
    TurismoValenciaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PaletteSection()
            TypographySection()
            ShapesSection()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.eyebrow,
            color = MaterialTheme.colorScheme.outline,
        )
        content()
    }
}

@Composable
private fun PaletteSection() {
    val c = MaterialTheme.colorScheme
    val app = MaterialTheme.appColors
    val swatches = listOf(
        "brand (iconos)" to app.brand,
        "primary (texto, botón)" to c.primary,
        "primaryContainer" to c.primaryContainer,
        "secondary (navy)" to c.secondary,
        "tertiary (eco)" to c.tertiary,
        "tertiaryContainer" to c.tertiaryContainer,
        "onTertiaryContainer" to c.onTertiaryContainer,
        "rating" to app.rating,
        "favorite" to app.favorite,
        "background" to c.background,
        "surface" to c.surface,
        "surfaceContainerHigh" to c.surfaceContainerHigh,
        "onSurface" to c.onSurface,
        "onSurfaceVariant" to c.onSurfaceVariant,
        "outline" to c.outline,
        "outlineVariant" to c.outlineVariant,
    )
    Section("Paleta") {
        swatches.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (name, color) -> Swatch(name, color, Modifier.weight(1f)) }
                if (pair.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(color, CardShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape),
        )
        Column {
            Text(name, style = MaterialTheme.typography.labelMedium)
            Text(
                text = hex(color),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun hex(color: Color) = String.format(Locale.ROOT, "#%06X", color.toArgb() and 0xFFFFFF)

@Composable
private fun TypographySection() {
    val t = MaterialTheme.typography
    val styles = listOf(
        "displayLarge" to t.displayLarge,
        "displayMedium" to t.displayMedium,
        "displaySmall" to t.displaySmall,
        "headlineLarge" to t.headlineLarge,
        "headlineMedium" to t.headlineMedium,
        "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge,
        "titleMedium" to t.titleMedium,
        "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge,
        "bodyMedium" to t.bodyMedium,
        "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge,
        "labelMedium" to t.labelMedium,
        "labelSmall" to t.labelSmall,
        "eyebrow" to t.eyebrow,
    )
    Section("Tipografía · Source Sans 3") {
        styles.forEach { (name, style) ->
            Column {
                Text(
                    text = "$name · ${style.fontSize.value.toInt()}sp · ${style.fontWeight?.weight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                val sample = when {
                    name == "eyebrow" -> "Sin conexión y accesibilidad".uppercase()
                    name.startsWith("display") -> "Valencia"
                    else -> "Valencia, mejor esta vida"
                }
                Text(text = sample, style = style)
            }
        }
    }
}

@Composable
private fun ShapesSection() {
    Section("Formas") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShapeSample("Tarjeta 16dp", CardShape, Modifier.weight(1f))
            ShapeSample("Sheet 24dp", BottomSheetShape, Modifier.weight(1f))
            ShapeSample("Píldora", PillShape, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShapeSample(label: String, shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
