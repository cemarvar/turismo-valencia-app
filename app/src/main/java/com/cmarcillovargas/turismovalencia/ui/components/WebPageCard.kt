package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.WebPageMeta
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing

// Medidas del diseño: miniatura cuadrada de las tarjetas de guía.
private val ThumbSize = 72.dp
private val ThumbShape = RoundedCornerShape(12.dp)

/** Tarjeta horizontal de una página de la web (guías de entradas, tours y experiencias, resultados…). */
@Composable
fun WebPageCard(page: WebPageMeta, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(CardShape).clickable(role = Role.Button, onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.xxs / 2,
    ) {
        Row(Modifier.padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            PlaceImage(null, Category.QueVer, Modifier.size(ThumbSize).clip(ThumbShape), page.title, assetPath = page.thumb ?: page.hero)
            Spacer(Modifier.width(Spacing.s))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs / 2)) {
                page.eyebrow?.let { Eyebrow(it, maxLines = 1) }
                Text(page.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.m))
        }
    }
}
