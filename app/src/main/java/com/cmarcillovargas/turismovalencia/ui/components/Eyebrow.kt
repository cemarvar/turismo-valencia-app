package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.cmarcillovargas.turismovalencia.ui.theme.eyebrow

/** Etiqueta superior en mayúsculas con tracking amplio («PASO 1 DE 3», «PERMISO DE UBICACIÓN»). */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.eyebrow,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
