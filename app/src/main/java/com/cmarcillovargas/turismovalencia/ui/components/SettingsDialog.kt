package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cmarcillovargas.turismovalencia.R

/**
 * Explica que un permiso solo se puede activar en los ajustes del móvil (el sistema ya no vuelve a preguntar) y ofrece abrirlos.
 * [onOpenSettings] abre los ajustes; el diálogo se cierra en cualquier caso.
 */
@Composable
fun OpenSettingsDialog(
    title: String,
    text: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onOpenSettings() }) { Text(stringResource(R.string.common_open_settings)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_not_now)) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
