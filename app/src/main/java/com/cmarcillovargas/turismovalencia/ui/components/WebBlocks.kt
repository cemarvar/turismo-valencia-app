package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebFilter
import com.cmarcillovargas.turismovalencia.data.model.WebItem
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

// Medidas del diseño de las tarjetas de contenido de la web.
private val ItemImageHeight = 176.dp
private val FactsShape = RoundedCornerShape(12.dp)
private const val FactLabelWeight = 0.38f
private const val SimpleBodyMax = 260
private const val SimpleFactsMax = 3

/**
 * Pinta los bloques de una página de la web (secciones numeradas, datos, consejos…) dentro de una `LazyColumn`.
 * `filters` guarda el filtro elegido en cada bloque; `legal` pinta los textos legales como texto corrido, sin tarjetas.
 */
fun LazyListScope.webBlocks(
    blocks: List<WebBlock>,
    filters: SnapshotStateMap<Int, String>,
    onOpenUrl: (String) -> Unit,
    legal: Boolean = false,
) {
    blocks.forEachIndexed { index, block ->
        when (block) {
            is WebBlock.Items -> {
                if (block.title.isNotBlank()) item(key = "title-$index") { BlockTitle(block.title) }
                val usable = block.filters.filter { filter -> block.items.any { it.category == filter.id } }
                if (usable.isNotEmpty()) item(key = "filters-$index") { FilterRow(usable, filters[index]) { id -> if (id == null) filters.remove(index) else filters[index] = id } }
                val selected = filters[index]
                val shown = if (selected == null) block.items else block.items.filter { it.category == selected }
                when {
                    legal -> shown.forEachIndexed { i, entry -> item(key = "legal-$index-$i") { LegalSection(entry) } }
                    shown.size > 1 && shown.all { it.isSimple() } -> item(key = "rows-$index") { SimpleRows(shown) }
                    else -> shown.forEachIndexed { i, entry -> item(key = "item-$index-$i") { WebItemCard(entry, onOpenUrl, Modifier.padding(horizontal = Spacing.m)) } }
                }
            }
            is WebBlock.Facts -> {
                if (block.title.isNotBlank()) item(key = "title-$index") { BlockTitle(block.title) }
                item(key = "facts-$index") { FactsCard(block.rows) }
            }
            is WebBlock.Bullets -> item(key = "bullets-$index") { TipsCard(block.title, block.items) }
        }
    }
}

@Composable
private fun BlockTitle(text: String) {
    SectionTitle(text, Modifier.padding(horizontal = Spacing.m).padding(top = Spacing.m))
}

@Composable
private fun FilterRow(options: List<WebFilter>, selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        AppFilterChip(stringResource(R.string.cat_all), selected == null, { onSelect(null) }, style = ChipStyle.Navy, height = Spacing.xl, textStyle = MaterialTheme.typography.labelLarge)
        options.forEach { filter ->
            AppFilterChip(filter.label, selected == filter.id, { onSelect(filter.id) }, style = ChipStyle.Navy, height = Spacing.xl, textStyle = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun WebItem.isSimple(): Boolean =
    image == null && tags.isEmpty() && url == null && bullets.isEmpty() && notes.isEmpty() && facts.size <= SimpleFactsMax && (body?.length ?: 0) <= SimpleBodyMax

/** Tarjeta de un elemento: foto, etiqueta, título, texto, datos, puntos, notas, etiquetas y enlace. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebItemCard(item: WebItem, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column {
            if (item.image != null) {
                PlaceImage(null, Category.QueVer, Modifier.fillMaxWidth().height(ItemImageHeight), item.title, assetPath = item.image)
            }
            Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                val kicker = listOfNotNull(item.num, item.kicker).joinToString(" · ")
                if (kicker.isNotBlank()) Eyebrow(kicker)
                if (!item.title.isNullOrBlank()) Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (!item.subtitle.isNullOrBlank()) {
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!item.body.isNullOrBlank()) Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.facts.isNotEmpty()) FactRows(item.facts, Modifier.padding(top = Spacing.xxs))
                if (item.bullets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        if (!item.bulletsTitle.isNullOrBlank()) Text(item.bulletsTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        item.bullets.forEach { Bullet(it) }
                    }
                }
                item.notes.forEach { (label, text) ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs / 2)) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (item.tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(top = Spacing.xxs)) {
                        item.tags.forEach { tag ->
                            StatusChip(tag.label, if (tag.free) StatusTone.Success else StatusTone.Neutral, height = Spacing.l, small = true)
                        }
                    }
                }
                if (item.url != null) {
                    AppButton(
                        text = stringResource(R.string.web_more_info),
                        onClick = { onOpenUrl(item.url) },
                        style = AppButtonStyle.Outlined,
                        trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                        compact = true,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.padding(top = Spacing.xxs).size(Spacing.m))
        Spacer(Modifier.width(Spacing.xs))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Filas «etiqueta · valor» sobre un fondo suave. */
@Composable
private fun FactRows(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clip(FactsShape).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = Spacing.s)) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.Top) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(FactLabelWeight))
                Spacer(Modifier.width(Spacing.s))
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f - FactLabelWeight))
            }
        }
    }
}

@Composable
private fun FactsCard(rows: List<Pair<String, String>>) {
    Surface(Modifier.padding(horizontal = Spacing.m).fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(vertical = Spacing.s), verticalAlignment = Alignment.Top) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(FactLabelWeight))
                    Spacer(Modifier.width(Spacing.s))
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f - FactLabelWeight))
                }
            }
        }
    }
}

/** Elementos cortos (pasos, preguntas, tarifas) en una sola tarjeta con separadores. */
@Composable
private fun SimpleRows(items: List<WebItem>) {
    Surface(Modifier.padding(horizontal = Spacing.m).fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.padding(vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xxs / 2)) {
                    val kicker = listOfNotNull(item.num, item.kicker).joinToString(" · ")
                    if (kicker.isNotBlank()) Eyebrow(kicker)
                    if (!item.title.isNullOrBlank()) Text(item.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (!item.subtitle.isNullOrBlank()) Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!item.body.isNullOrBlank()) Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    item.facts.forEach { (label, value) ->
                        Text("$label: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

/** Consejos o puntos clave sobre fondo naranja suave. */
@Composable
private fun TipsCard(title: String, items: List<String>) {
    Column(
        Modifier
            .padding(horizontal = Spacing.m)
            .padding(top = Spacing.m)
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.appColors.brandSoft)
            .padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        items.forEach { Bullet(it) }
    }
}

/** Sección de un texto legal: título y párrafos, sin tarjeta. */
@Composable
private fun LegalSection(item: WebItem) {
    Column(Modifier.padding(horizontal = Spacing.m).padding(top = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (!item.title.isNullOrBlank()) Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        item.body?.split("\n\n")?.filter { it.isNotBlank() }?.forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item.bullets.forEach { Bullet(it) }
    }
}
