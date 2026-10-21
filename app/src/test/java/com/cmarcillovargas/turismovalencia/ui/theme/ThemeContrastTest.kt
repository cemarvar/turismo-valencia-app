package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Contraste WCAG 2.1 de cada pareja de texto (o icono) y fondo que usan las pantallas, en el tema claro y en el oscuro. Si alguien
 * toca un color de `Color.kt`, esto avisa de la pareja que deja de cumplir. Texto normal: 4,5:1 (AA); iconos y gráficos: 3:1.
 */
class ThemeContrastTest {
    private class Check(val name: String, val foreground: Color, val background: Color, val minimum: Double)

    private fun luminance(color: Color): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val argb = color.toArgb()
        return 0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun pairs(c: ColorScheme, a: AppColors): List<Check> = listOf(
        Check("texto principal sobre el fondo", c.onBackground, c.background, 4.5),
        Check("texto principal sobre una tarjeta", c.onSurface, c.surface, 4.5),
        Check("texto principal sobre un chip o campo", c.onSurface, c.surfaceContainerHigh, 4.5),
        Check("texto secundario sobre el fondo", c.onSurfaceVariant, c.background, 4.5),
        Check("texto secundario sobre una tarjeta", c.onSurfaceVariant, c.surface, 4.5),
        Check("texto secundario sobre un chip o campo", c.onSurfaceVariant, c.surfaceContainerHigh, 4.5),
        Check("placeholder y eyebrow sobre una tarjeta", c.outline, c.surface, 4.5),
        Check("naranja de texto sobre el fondo", c.primary, c.background, 4.5),
        Check("naranja de texto sobre una tarjeta", c.primary, c.surface, 4.5),
        Check("texto del botón sobre su relleno", a.onAction, a.action, 4.5),
        Check("texto sobre el contenedor naranja", c.onPrimaryContainer, c.primaryContainer, 4.5),
        Check("iconos de pizarra sobre una tarjeta", c.secondary, c.surface, 4.5),
        Check("iconos de pizarra sobre el fondo", c.secondary, c.background, 4.5),
        Check("texto sobre el avatar y el aviso sin conexión", a.onNavy, a.navy, 4.5),
        Check("verde de texto sobre una tarjeta", c.tertiary, c.surface, 4.5),
        Check("texto de la etiqueta «Eco»", c.onTertiary, c.tertiary, 4.5),
        Check("texto de la etiqueta de éxito", c.onTertiaryContainer, c.tertiaryContainer, 4.5),
        Check("rojo de texto sobre una tarjeta", c.error, c.surface, 4.5),
        Check("texto del aviso de error", c.onErrorContainer, c.errorContainer, 4.5),
        Check("texto del snackbar", c.inverseOnSurface, c.inverseSurface, 4.5),
        Check("acción del snackbar", c.inversePrimary, c.inverseSurface, 4.5),
        Check("texto de la etiqueta «Pendiente»", a.warning, a.warningContainer, 4.5),
        Check("texto blanco de ValBot sobre su cabecera", Color.White, a.bot, 4.5),
        Check("tinta del QR sobre su papel", a.onPaper, a.paper, 7.0),
        Check("color de ValBot como texto e icono sobre una tarjeta", a.botAccent, c.surface, 4.5),
        Check("icono blanco sobre el marcador «Sostenible»", Color.White, a.eco, 4.5),
        Check("icono blanco sobre el marcador «Qué ver»", Color.White, a.navy, 4.5),
        Check("naranja de marca como gráfico sobre una tarjeta", a.brand, c.surface, 3.0),
        Check("pulgar del interruptor sobre su track", a.switchThumb, a.brand, 3.0),
    )

    private fun comprobar(tema: String, pares: List<Check>) {
        val fallos = pares.filter { contrast(it.foreground, it.background) < it.minimum }
        assertTrue(
            "$tema: ${fallos.size} parejas por debajo del mínimo: " +
                fallos.joinToString("; ") { "${it.name} = ${"%.2f".format(contrast(it.foreground, it.background))} (mínimo ${it.minimum})" },
            fallos.isEmpty(),
        )
    }

    @Test
    fun `el tema claro cumple el contraste AA en todas sus parejas`() = comprobar("claro", pairs(LightColorScheme, LightAppColors))

    @Test
    fun `el tema oscuro cumple el contraste AA en todas sus parejas`() = comprobar("oscuro", pairs(DarkColorScheme, DarkAppColors))

    @Test
    fun `en oscuro las superficies se aclaran con la elevacion y el texto es claro`() {
        val c = DarkColorScheme
        assertTrue(luminance(c.background) < luminance(c.surface))
        assertTrue(luminance(c.surface) < luminance(c.surfaceContainerHigh))
        assertTrue(luminance(c.onSurface) > 0.5)
        assertTrue(luminance(c.background) < 0.01)
    }

    @Test
    fun `el codigo QR es siempre tinta oscura sobre papel blanco`() {
        for (a in listOf(LightAppColors, DarkAppColors)) {
            assertTrue(luminance(a.paper) > 0.95)
            assertTrue(luminance(a.onPaper) < 0.05)
        }
    }

    @Test
    fun `los colores de relleno de accion no cambian de papel entre temas`() {
        // El naranja de marca de los gráficos es el mismo en los dos temas: solo cambian fondos y textos.
        assertTrue(LightAppColors.brand == DarkAppColors.brand)
        assertTrue(LightAppColors.onAction == DarkAppColors.onAction)
    }
}
