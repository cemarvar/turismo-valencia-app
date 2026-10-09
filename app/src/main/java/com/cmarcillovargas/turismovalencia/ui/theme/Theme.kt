package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// TODO(dark): el Figma no define modo oscuro, así que el tema es solo claro y no hay parámetro darkTheme.
// Sin color dinámico: la marca (naranja) tiene que verse igual en todos los móviles.
/** `fontScaleFactor` multiplica el tamaño de texto del sistema (ajuste «Tamaño del texto»). */
@Composable
fun TurismoValenciaTheme(fontScaleFactor: Float = 1f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppColors provides LightAppColors,
        LocalDensity provides Density(density.density, density.fontScale * fontScaleFactor),
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
