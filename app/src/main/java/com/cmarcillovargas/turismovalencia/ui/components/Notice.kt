package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme

/**
 * Aviso de un fallo en su sitio de la pantalla (por ejemplo, sobre la barra de acciones): icono y texto sobre el color de error.
 * Es una región «en vivo»: el lector de pantalla lo lee en cuanto aparece.
 */
@Composable
fun ErrorNotice(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = CardShape,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(Modifier.padding(Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(Spacing.l))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        }
    }
}

/** Aviso informativo, neutro (no es un fallo): lo que se va a ver no está en el idioma de la app, por ejemplo. */
@Composable
fun InfoNotice(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.Translate) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.padding(Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.l))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(name = "Aviso de error", showBackground = true, widthDp = 390)
@Composable
private fun ErrorNoticePreview() {
    TurismoValenciaTheme {
        ErrorNotice("Ya no quedan plazas para ese día y horario. Prueba con otro.", Modifier.padding(Spacing.m))
    }
}

@Preview(name = "Aviso informativo", showBackground = true, widthDp = 390)
@Composable
private fun InfoNoticePreview() {
    TurismoValenciaTheme {
        InfoNotice("This guide is only available in Spanish.", Modifier.padding(Spacing.m))
    }
}
