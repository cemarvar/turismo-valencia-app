package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// La app sigue el modo claro u oscuro del sistema (`darkTheme`); no hay selector propio porque el diseño no lo tiene.
// Sin color dinámico: la marca (naranja) tiene que verse igual en todos los móviles.
/** `fontScaleFactor` multiplica el tamaño de texto del sistema (ajuste «Tamaño del texto»). */
@Composable
fun TurismoValenciaTheme(
    fontScaleFactor: Float = 1f,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors,
        LocalDarkTheme provides darkTheme,
        LocalDensity provides Density(density.density, density.fontScale * fontScaleFactor),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
