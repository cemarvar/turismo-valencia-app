package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.SectionTitle
import com.cmarcillovargas.turismovalencia.ui.components.SegmentedTabs
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.components.WebPageCard
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.formatPrice

private val BannerHeight = 132.dp
private val OfferShape = RoundedCornerShape(20.dp)

@Composable
fun EntradasRoute(
    initialTab: OfferTab,
    onBack: () -> Unit,
    onReserve: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    viewModel: EntradasViewModel = viewModel(key = "entradas", factory = containerFactory { EntradasViewModel(it, initialTab) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EntradasScreen(uiState, onBack, onReserve, viewModel::onSelectTab, viewModel::onSearching, viewModel::onQueryChange, onOpenPage)
}

@Composable
fun EntradasScreen(
    uiState: EntradasUiState,
    onBack: () -> Unit,
    onReserve: (String) -> Unit,
    onSelectTab: (OfferTab) -> Unit,
    onSearching: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.searching) {
            AppTopBar(title = "", onBack = { onSearching(false) }) {
                BasicTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.appColors.brand),
                    modifier = Modifier.weight(1f).padding(end = Spacing.m),
                    decorationBox = { inner ->
                        Box {
                            if (uiState.query.isEmpty()) Text(stringResource(R.string.tickets_search_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            inner()
                        }
                    },
                )
            }
        } else {
            AppTopBar(title = stringResource(R.string.cat_tickets), onBack = onBack) {
                IconButton(onClick = { onSearching(true) }) {
                    Icon(Icons.Outlined.Search, stringResource(R.string.common_search), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item {
                SegmentedTabs(
                    labels = listOf(R.string.tickets_tab_tickets, R.string.tickets_tab_tours, R.string.tickets_tab_experiences).map { stringResource(it) },
                    selectedIndex = uiState.tab.ordinal,
                    onSelect = { onSelectTab(OfferTab.entries[it]) },
                )
            }
            items(uiState.offers, key = { it.id }) { offer -> OfferCard(offer, onReserve = { onReserve(offer.id) }) }
            if (uiState.guides.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.tickets_guides), Modifier.padding(top = Spacing.s)) }
                items(uiState.guides, key = { it.id }) { guide -> WebPageCard(guide, { onOpenPage(guide.id) }) }
            }
            if (uiState.offers.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.tickets_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.l),
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: Offer, onReserve: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = OfferShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column {
            Box(Modifier.fillMaxWidth().height(BannerHeight)) {
                PlaceImage(offer.image, offer.category(), Modifier.fillMaxSize(), offer.title, assetPath = offer.assetImage)
                StatusChip(
                    text = offer.tag,
                    tone = StatusTone.Brand,
                    modifier = Modifier.align(Alignment.TopStart).padding(Spacing.s),
                    height = Spacing.l,
                    small = true,
                )
            }
            Column(Modifier.padding(start = Spacing.m, end = Spacing.m, top = Spacing.xs + Spacing.xxs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = offer.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.m + Spacing.xxs))
                            Spacer(Modifier.width(Spacing.xxs))
                            Text(
                                // El servidor puede dejar algún dato vacío (p. ej. «extra»): no se pinta un separador de más.
                                text = listOf(offer.duration, offer.place, offer.extra).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(Spacing.s))
                    AppButton(stringResource(R.string.tickets_reserve), onReserve, compact = true)
                }
                // «desde» va centrado con el precio (más cifras que letras), no pegado a su línea base.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tickets_from), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(Spacing.xxs))
                    Text(formatPrice(offer.priceFromCents), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Sección a la que pertenece la oferta, para elegir el icono del marcador de imagen. */
private fun Offer.category(): Category = when {
    tag == "Gastronómica" -> Category.Gastronomia
    kind == OfferKind.Experiencia -> Category.Sostenible
    else -> Category.QueVer
}

@Preview(name = "B6 · Tickets & Tours", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EntradasPreview() {
    TurismoValenciaTheme {
        EntradasScreen(
            uiState = EntradasUiState(OfferTab.Experiencias, FakeOfertas.filter { it.kind == OfferKind.Experiencia }.take(3)),
            onBack = {}, onReserve = {}, onSelectTab = {}, onSearching = {}, onQueryChange = {}, onOpenPage = {},
        )
    }
}
