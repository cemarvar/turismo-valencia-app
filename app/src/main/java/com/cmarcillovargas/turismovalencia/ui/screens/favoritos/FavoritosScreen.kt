package com.cmarcillovargas.turismovalencia.ui.screens.favoritos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.ui.components.EmptyState
import com.cmarcillovargas.turismovalencia.ui.components.OfflineChip
import com.cmarcillovargas.turismovalencia.ui.components.PlaceListCard
import com.cmarcillovargas.turismovalencia.ui.components.SegmentedTabs
import com.cmarcillovargas.turismovalencia.ui.components.UserAvatar
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.tabTitle

private val SummaryShape = RoundedCornerShape(20.dp)

@Composable
fun FavoritosRoute(
    onOpenItem: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDownloads: () -> Unit,
    onExplore: () -> Unit,
    viewModel: FavoritosViewModel = viewModel(factory = containerFactory { FavoritosViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritosScreen(
        uiState = uiState,
        onOpenItem = onOpenItem,
        onOpenProfile = onOpenProfile,
        onOpenDownloads = onOpenDownloads,
        onExplore = onExplore,
        onSelectTab = viewModel::onSelectTab,
        onToggleFavorite = viewModel::onToggleFavorite,
        onToggleDownload = viewModel::onToggleDownload,
    )
}

@Composable
fun FavoritosScreen(
    uiState: FavoritosUiState,
    onOpenItem: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDownloads: () -> Unit,
    onExplore: () -> Unit,
    onSelectTab: (FavoritesTab) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabHeader(
            title = stringResource(R.string.nav_favoritos),
            subtitle = if (uiState.totalSaved == 0) stringResource(R.string.fav_none_saved) else pluralStringResource(R.plurals.fav_saved_count, uiState.totalSaved, uiState.totalSaved),
            onOpenProfile = onOpenProfile,
        )
        if (uiState.totalSaved == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = stringResource(R.string.fav_empty_title),
                    text = stringResource(R.string.fav_empty_text),
                    actionText = stringResource(R.string.fav_explore),
                    actionIcon = Icons.Outlined.Search,
                    onAction = onExplore,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                item { OfflineSummary(uiState, onOpenDownloads) }
                item {
                    SegmentedTabs(
                        labels = listOf(R.string.fav_tab_all, R.string.fav_tab_places, R.string.fav_tab_events).map { stringResource(it) },
                        selectedIndex = uiState.tab.ordinal,
                        onSelect = { onSelectTab(FavoritesTab.entries[it]) },
                        modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                }
                items(uiState.items, key = { it.id }) { item ->
                    PlaceListCard(
                        item = item,
                        favorite = true,
                        onClick = { onOpenItem(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        eyebrow = stringResource(item.category.singularLabel()),
                        showRating = false,
                        footer = { OfflineChip(downloaded = item.id in uiState.downloaded, onClick = { onToggleDownload(item.id) }) },
                    )
                }
            }
        }
    }
}

/** Cabecera blanca de las pestañas con título grande, subtítulo y el avatar del usuario. */
@Composable
fun TabHeader(title: String, subtitle: String, onOpenProfile: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs) {
        Row(
            modifier = Modifier.statusBarsPadding().padding(start = Spacing.m, end = Spacing.m, top = Spacing.s, bottom = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.tabTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            UserAvatar(initials = stringResource(R.string.profile_initials), onClick = onOpenProfile)
        }
    }
}

@Composable
private fun OfflineSummary(uiState: FavoritosUiState, onOpenDownloads: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SummaryShape)
            .background(MaterialTheme.appColors.brandSoft)
            .padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Spacing.xl + Spacing.xs).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l))
        }
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.fav_offline_summary, uiState.availableOffline, uiState.totalSaved),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.fav_offline_mb, uiState.offlineMb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onOpenDownloads,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                stringResource(R.string.fav_manage),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            )
        }
    }
}

/** Etiqueta en singular de la sección, para las tarjetas de lista de favoritos y descargas. */
fun Category.singularLabel(): Int = when (this) {
    Category.QueVer -> R.string.cat_que_ver
    Category.Gastronomia -> R.string.cat_gastronomia
    Category.Alojamientos -> R.string.fav_label_lodging
    Category.Eventos -> R.string.fav_label_event
    Category.Sostenible -> R.string.cat_sostenible
}

@Preview(name = "E1 · Favoritos", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FavoritosPreview() {
    TurismoValenciaTheme {
        FavoritosScreen(
            FavoritosUiState(items = FakeQueVer.take(3), totalSaved = 6, availableOffline = 4, offlineMb = 48, downloaded = setOf("lonja-seda")),
            {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "H3 · Favoritos vacío", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FavoritosEmptyPreview() {
    TurismoValenciaTheme { FavoritosScreen(FavoritosUiState(), {}, {}, {}, {}, {}, {}, {}) }
}
