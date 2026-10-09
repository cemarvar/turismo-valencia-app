package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import kotlin.math.abs

private val DotSize = Spacing.xs
private val ActiveWidth = Spacing.l
private const val NearAlpha = 0.6f
private const val FarAlpha = 0.3f

/**
 * Indicador de páginas. Por defecto, la página activa es una píldora naranja y las demás puntos grises
 * (onboarding). Sobre fondos de color: `activeColor`/`inactiveColor` blancos, `stretchActive = false`
 * y `fadeInactive = true` (splash). Es decorativo: la posición ya la dice el texto «Paso x de y».
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.appColors.brand,
    inactiveColor: Color = MaterialTheme.appColors.controlInactive,
    stretchActive: Boolean = true,
    fadeInactive: Boolean = false,
) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected && stretchActive) ActiveWidth else DotSize,
                label = "indicatorWidth",
            )
            val target = when {
                selected -> activeColor
                fadeInactive -> inactiveColor.copy(alpha = if (abs(index - currentPage) == 1) NearAlpha else FarAlpha)
                else -> inactiveColor
            }
            val color by animateColorAsState(target, label = "indicatorColor")
            Box(
                Modifier
                    .size(width = width, height = DotSize)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
