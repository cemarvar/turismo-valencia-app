package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta «Web (referencia)».
// PROVISIONAL: valores medidos de las capturas de Figma (Explorar y G1) porque docs/guia-estilo.md
// aún no está en el repo. Verificar contra la guía (ver «Decisiones» en docs/progreso.md).
// Las pantallas nunca usan Color(0x…): solo MaterialTheme.colorScheme.* o MaterialTheme.appColors.*.

private val Brand = Color(0xFFEB5A00)          // Solo iconos y gráficos: 3,5:1 sobre blanco, no vale para texto.
private val PrimaryStrong = Color(0xFFC44D00)  // Texto y botones: 4,76:1 sobre blanco (AA).
private val BrandTint = Color(0xFFFDEBDD)      // Fondo del elemento seleccionado (píldora de la barra inferior).
private val Navy = Color(0xFF2C3E50)           // Avatar, iconos de ajustes, «Mejor esta vida».
private val Eco = Color(0xFF2E7D32)            // Etiqueta «Eco» (con texto blanco).
private val Success = Color(0xFF1B7A43)        // Texto de «Sin conexión» / «reservas».
private val SuccessTint = Color(0xFFE3F4EA)    // Fondo de esas etiquetas.
private val Rating = Color(0xFFF2A900)         // Estrella de valoración.
private val Favorite = Color(0xFFE0453A)       // Corazón de favorito activo.

private val Ink = Color(0xFF1A1A1A)            // Texto principal.
private val InkMuted = Color(0xFF555555)       // Texto secundario e iconos inactivos.
private val InkSubtle = Color(0xFF6E6E6E)      // Placeholders, chevrons, eyebrows.
private val Neutral50 = Color(0xFFF9F9F9)      // Fondo de pantalla.
private val Neutral100 = Color(0xFFF0F0F0)     // Chips neutros.
private val Neutral200 = Color(0xFFE5E5E5)     // Divisores.
private val White = Color(0xFFFFFFFF)          // Tarjetas, barras.

// TODO(dark): el Figma no define modo oscuro; solo existe el esquema claro.
internal val LightColorScheme = lightColorScheme(
    primary = PrimaryStrong,
    onPrimary = White,
    primaryContainer = BrandTint,
    onPrimaryContainer = PrimaryStrong, // 4,11:1: solo iconos/gráficos sobre la píldora, no texto pequeño.
    secondary = Navy,
    onSecondary = White,
    secondaryContainer = Neutral100,
    onSecondaryContainer = InkMuted,
    tertiary = Eco,
    onTertiary = White,
    tertiaryContainer = SuccessTint,
    onTertiaryContainer = Success,
    background = Neutral50,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Neutral100,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = Neutral100,
    surfaceContainerHighest = Neutral100,
    outline = InkSubtle,
    outlineVariant = Neutral200,
    // error, scrim y roles inversos: valores por defecto de Material 3 (no aparecen en las capturas).
)

/** Tokens que Material 3 no tiene como rol: marca para iconos/gráficos, valoración y favorito. */
@Immutable
data class AppColors(
    val brand: Color,
    val rating: Color,
    val favorite: Color,
)

internal val LightAppColors = AppColors(
    brand = Brand,
    rating = Rating,
    favorite = Favorite,
)

internal val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
