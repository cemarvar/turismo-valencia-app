package com.cmarcillovargas.turismovalencia.ui.theme

import androidx.compose.ui.unit.dp

/** Espaciado en múltiplos de 4dp. Las pantallas usan estos tokens y no dp sueltos. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Área táctil mínima (accesibilidad) y alto de los botones del diseño. */
val MinTouchTarget = 48.dp

/** Opacidad del velo negro bajo diálogos y bottom sheets (medida en las capturas de Figma). */
const val ScrimAlpha = 0.6f
