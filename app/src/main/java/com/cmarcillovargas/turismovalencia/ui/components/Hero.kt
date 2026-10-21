package com.cmarcillovargas.turismovalencia.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.Stat
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.eyebrow

private const val TopShade = 0.45f
private const val BottomShade = 0.85f
private const val ControlAlpha = 0.4f
private val DividerWidth = 1.dp

/**
 * Cabecera con foto a todo el ancho: botones de atrás/compartir/favorito arriba y etiqueta, título y tres cifras abajo.
 * Se usa en las fichas de lugar, de evento y en la portada de turismo sostenible.
 */
@Composable
fun DetailHero(
    @DrawableRes image: Int?,
    category: Category,
    eyebrow: String,
    title: String,
    stats: List<Stat>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 330.dp,
    accent: Color = MaterialTheme.appColors.gold,
    favorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    imageDescription: String? = null,
    /** Foto de la web (ruta dentro de `assets/img/`) si no hay recurso propio. */
    assetPath: String? = null,
) {
    Box(modifier.fillMaxWidth().height(height)) {
        if (image == null && rememberAssetImage(assetPath) == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.appColors.navy))
        } else {
            PlaceImage(image, category, Modifier.fillMaxSize(), imageDescription, assetPath = assetPath)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to MaterialTheme.colorScheme.scrim.copy(alpha = TopShade),
                        0.3f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.scrim.copy(alpha = BottomShade),
                    ),
                ),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (onShare != null) {
                HeroIconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, stringResource(R.string.common_share), tint = Color.White)
                }
                Spacer(Modifier.width(Spacing.s))
            }
            if (favorite != null && onToggleFavorite != null) {
                FavoriteButton(favorite, onToggleFavorite, style = FavoriteStyle.OnPhotoDark, circleSize = Spacing.xxl - Spacing.xs)
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.m + Spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(text = eyebrow.uppercase(), style = MaterialTheme.typography.eyebrow, color = accent)
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            HeroStats(stats, accent)
        }
    }
}

@Composable
private fun HeroIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(Spacing.xxl - Spacing.xs).clip(CircleShape).background(MaterialTheme.colorScheme.scrim.copy(alpha = ControlAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun HeroStats(stats: List<Stat>, accent: Color) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) {
                Box(Modifier.width(DividerWidth).fillMaxHeight().background(Color.White.copy(alpha = 0.3f)))
                Spacer(Modifier.width(Spacing.s))
            }
            Column(Modifier.weight(1f)) {
                Text(text = stat.value, style = MaterialTheme.typography.headlineSmall, color = accent, maxLines = 1)
                Text(
                    text = stat.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                )
            }
        }
    }
}
