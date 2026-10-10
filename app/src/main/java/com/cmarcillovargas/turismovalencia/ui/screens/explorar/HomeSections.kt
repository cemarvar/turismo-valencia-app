package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.HomeTile
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.SectionTitle
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing

// Medidas del diseño: rejillas de la portada de la web («Lo esencial», «Planes», «Turismo sostenible»).
private val TileHeight = 128.dp
private val TileShape = RoundedCornerShape(16.dp)
private val SectionPadding = 20.dp
private const val TileShadeAlpha = 0.72f

/** Sección de la portada: título con barrita, texto y una rejilla de dos columnas con fotos y su nombre encima. */
@Composable
fun HomeTileSection(
    title: String,
    text: String,
    tiles: List<HomeTile>,
    category: Category,
    onOpenTile: (HomeTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = SectionPadding)) {
        SectionTitle(title)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.m),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    pair.forEach { tile -> HomeTileCard(tile, category, { onOpenTile(tile) }, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeTileCard(tile: HomeTile, category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.height(TileHeight).clip(TileShape).clickable(role = Role.Button, onClick = onClick)) {
        PlaceImage(null, category, Modifier.fillMaxSize(), tile.title, assetPath = tile.image)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(colorStops = arrayOf(0.35f to Color.Transparent, 1f to MaterialTheme.colorScheme.scrim.copy(alpha = TileShadeAlpha))),
            ),
        )
        Text(
            text = tile.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.s),
        )
    }
}
