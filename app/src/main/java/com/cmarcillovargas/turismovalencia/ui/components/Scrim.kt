package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.cmarcillovargas.turismovalencia.ui.theme.ScrimAlpha

/** Color del velo que oscurece el fondo bajo diálogos y bottom sheets. */
@Composable
fun scrimColor(): Color = MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha)

/** Velo a pantalla completa para diálogos dibujados a mano (los `ModalBottomSheet` usan `scrimColor()`). */
@Composable
fun Scrim(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(scrimColor()).clearAndSetSemantics {})
}
