package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// TODO(dark): el Figma no define modo oscuro, así que el tema es solo claro y no hay parámetro darkTheme.
// Sin color dinámico: la marca (naranja) tiene que verse igual en todos los dispositivos.
@Composable
fun TurismoValenciaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides LightAppColors) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
