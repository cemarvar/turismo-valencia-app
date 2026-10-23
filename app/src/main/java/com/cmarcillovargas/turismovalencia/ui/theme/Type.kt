package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cmarcillovargas.turismovalencia.R

// Source Sans 3 va empaquetada en res/font para que funcione sin conexión.
val SourceSans3 = FontFamily(
    Font(R.font.source_sans_3_regular, FontWeight.Normal),
    Font(R.font.source_sans_3_italic, FontWeight.Normal, FontStyle.Italic),
    // Ningún estilo del tema lo usa (el diseño solo trae Regular, SemiBold, Bold, ExtraBold e Italic), pero está para que un
    // `FontWeight.Medium` no caiga a Regular.
    Font(R.font.source_sans_3_medium, FontWeight.Medium),
    Font(R.font.source_sans_3_semibold, FontWeight.SemiBold),
    Font(R.font.source_sans_3_bold, FontWeight.Bold),
    Font(R.font.source_sans_3_extrabold, FontWeight.ExtraBold),
)

// Escala PROVISIONAL: tamaños estimados midiendo la altura de mayúscula en las capturas de Figma
// (Explorar y G1) y redondeados; el tracking negativo de los títulos es un valor propio.
// Verificar contra docs/guia-estilo.md (ver «Decisiones» en docs/progreso.md).
private val TitleTracking = (-0.02).em

private fun style(weight: FontWeight, size: Int, lineHeight: Int, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = SourceSans3,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking,
)

val Typography = Typography(
    // Títulos: ExtraBold con tracking negativo.
    displayLarge = style(FontWeight.ExtraBold, 57, 64, TitleTracking),
    displayMedium = style(FontWeight.ExtraBold, 45, 52, TitleTracking),
    displaySmall = style(FontWeight.ExtraBold, 36, 44, TitleTracking),
    headlineLarge = style(FontWeight.ExtraBold, 33, 40, TitleTracking), // «Valencia» del splash
    headlineMedium = style(FontWeight.ExtraBold, 26, 32, TitleTracking), // Título del onboarding
    headlineSmall = style(FontWeight.ExtraBold, 21, 26, TitleTracking), // «Cerca de ti», «Idioma de la app»
    titleLarge = style(FontWeight.ExtraBold, 20, 26, TitleTracking),    // Nombre en el perfil
    // Tarjetas y barras superiores: SemiBold.
    titleMedium = style(FontWeight.SemiBold, 17, 22),                   // «Mercado Central», «Perfil y ajustes»
    titleSmall = style(FontWeight.SemiBold, 15, 20),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 15, 22),                      // Subtítulo del sheet de idioma
    bodySmall = style(FontWeight.Normal, 13, 18),                       // Metadatos: «Ciutat Vella · 450 m»
    labelLarge = style(FontWeight.SemiBold, 14, 20),                    // Botones
    labelMedium = style(FontWeight.SemiBold, 13, 16),                   // Chips
    labelSmall = style(FontWeight.SemiBold, 11, 16),                    // Barra de navegación
)

/**
 * Eyebrow: 11sp con +14 % de tracking. Va en mayúsculas, pero un TextStyle no puede forzarlo:
 * pasar el texto con `.uppercase()` al componer.
 */
val Typography.eyebrow: TextStyle
    get() = labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.14.em)

/** Título de la barra superior de las pantallas interiores («Tickets & Tours», «Descargas sin conexión»). */
val Typography.screenTitle: TextStyle
    get() = titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = TitleTracking)

/** Cabecera grande de las pestañas («Favoritos», «Mis reservas»). */
val Typography.tabTitle: TextStyle
    get() = headlineMedium
