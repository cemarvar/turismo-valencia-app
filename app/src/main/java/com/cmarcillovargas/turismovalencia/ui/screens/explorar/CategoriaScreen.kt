package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.SortMode
import com.cmarcillovargas.turismovalencia.ui.components.AppFilterChip
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.ChipStyle
import com.cmarcillovargas.turismovalencia.ui.components.PlaceGridCard
import com.cmarcillovargas.turismovalencia.ui.components.PlaceListCard
import com.cmarcillovargas.turismovalencia.ui.theme.BottomSheetShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val AccentBarWidth = 48.dp
private val AccentBarHeight = 4.dp

@Composable
fun CategoriaRoute(
    category: Category,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: CategoriaViewModel = viewModel(key = "categoria-${category.key}", factory = containerFactory { CategoriaViewModel(it, category) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriaScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenItem = onOpenItem,
        onOpenMap = onOpenMap,
        onSelectFilter = viewModel::onSelectFilter,
        onSort = viewModel::onSort,
        onToggleGrid = viewModel::onToggleGrid,
        onToggleFavorite = viewModel::onToggleFavorite,
        onToggleDownload = viewModel::onToggleDownload,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaScreen(
    uiState: CategoriaUiState,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenMap: () -> Unit,
    onSelectFilter: (String?) -> Unit,
    onSort: (SortMode) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSort by remember { mutableStateOf(false) }
    val texts = uiState.category.texts()
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(
            title = stringResource(texts.title),
            subtitle = stringResource(R.string.cat_subtitle, stringResource(texts.banner), uiState.total, stringResource(texts.unit)),
            onBack = onBack,
            actions = {
                IconButton(onClick = onOpenMap) {
                    Icon(Icons.Outlined.Map, stringResource(R.string.common_map), tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = { showSort = true }) {
                    Icon(Icons.Outlined.Tune, stringResource(R.string.cat_sort), tint = MaterialTheme.colorScheme.secondary)
                }
            },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.l)) {
            item { Banner(stringResource(texts.banner), stringResource(texts.tagline)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    modifier = Modifier.padding(top = Spacing.m).selectableGroup(),
                ) {
                    item {
                        AppFilterChip(stringResource(R.string.cat_all), uiState.selectedFilter == null, { onSelectFilter(null) }, style = ChipStyle.Navy, height = Spacing.xl, textStyle = MaterialTheme.typography.labelLarge)
                    }
                    items(uiState.filters) { filter ->
                        AppFilterChip(filter, uiState.selectedFilter == filter, { onSelectFilter(filter) }, style = ChipStyle.Navy, height = Spacing.xl, textStyle = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(uiState.sort.label(uiState.category)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onToggleGrid).padding(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (uiState.gridMode) Icons.Outlined.GridView else Icons.Outlined.Menu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(Spacing.l),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = stringResource(if (uiState.gridMode) R.string.cat_view_grid else R.string.cat_view_list),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            if (uiState.gridMode) {
                items(uiState.items.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m).padding(bottom = Spacing.s),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        pair.forEach { item ->
                            PlaceGridCard(
                                item = item,
                                favorite = item.id in uiState.favorites,
                                downloaded = item.id in uiState.downloaded,
                                onClick = { onOpenItem(item.id) },
                                onToggleFavorite = { onToggleFavorite(item.id) },
                                onToggleDownload = { onToggleDownload(item.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(uiState.items, key = { it.id }) { item ->
                    PlaceListCard(
                        item = item,
                        favorite = item.id in uiState.favorites,
                        onClick = { onOpenItem(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        modifier = Modifier.padding(horizontal = Spacing.m).padding(bottom = Spacing.s),
                    )
                }
            }
        }
    }
    if (showSort) {
        ModalBottomSheet(
            onDismissRequest = { showSort = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = BottomSheetShape,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(horizontal = Spacing.m).padding(bottom = Spacing.l).selectableGroup()) {
                Text(stringResource(R.string.cat_sort), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = Spacing.s))
                SortMode.entries.forEach { mode ->
                    val selected = mode == uiState.sort
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.xxl)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSort(mode); showSort = false }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(mode.optionLabel(uiState.category)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.appColors.brand)
                    }
                }
            }
        }
    }
}

@Composable
private fun Banner(title: String, tagline: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.brandSoft)
            .padding(horizontal = Spacing.m, vertical = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = tagline,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.s))
        Box(Modifier.size(AccentBarWidth, AccentBarHeight).clip(CircleShape).background(MaterialTheme.appColors.brand))
    }
}

private class CategoryTexts(val title: Int, val banner: Int, val tagline: Int, val unit: Int)

private fun Category.texts() = when (this) {
    Category.QueVer -> CategoryTexts(R.string.cat_que_ver, R.string.cat_que_ver_banner, R.string.cat_que_ver_tagline, R.string.cat_unit_places)
    Category.Gastronomia -> CategoryTexts(R.string.cat_gastronomia, R.string.cat_gastro_banner, R.string.cat_gastro_tagline, R.string.cat_unit_dishes)
    Category.Alojamientos -> CategoryTexts(R.string.cat_alojamientos, R.string.cat_aloj_banner, R.string.cat_aloj_tagline, R.string.cat_unit_zones)
    Category.Eventos -> CategoryTexts(R.string.cat_eventos, R.string.cat_eventos_banner, R.string.cat_eventos_tagline, R.string.cat_unit_events)
    Category.Sostenible -> CategoryTexts(R.string.cat_sostenible, R.string.cat_sost_banner, R.string.cat_sost_tagline, R.string.cat_unit_topics)
}

private fun SortMode.label(category: Category) = when (this) {
    SortMode.Distance -> if (category == Category.QueVer) R.string.cat_sorted_distance else R.string.cat_sorted_web
    SortMode.Rating -> R.string.cat_sorted_rating
    SortMode.Name -> R.string.cat_sorted_name
}

private fun SortMode.optionLabel(category: Category) = when (this) {
    SortMode.Distance -> if (category == Category.QueVer) R.string.cat_sort_distance else R.string.cat_sort_web
    SortMode.Rating -> R.string.cat_sort_rating
    SortMode.Name -> R.string.cat_sort_name
}

@Preview(name = "B3 · Categoría «Qué ver»", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CategoriaPreview() {
    TurismoValenciaTheme {
        CategoriaScreen(
            uiState = CategoriaUiState(
                category = Category.QueVer,
                filters = listOf("Monumentos", "Museos", "Barrios", "Naturaleza"),
                items = FakeQueVer.take(4),
                total = 13,
                favorites = setOf("mercado-central"),
            ),
            onBack = {}, onOpenItem = {}, onOpenMap = {}, onSelectFilter = {}, onSort = {}, onToggleGrid = {}, onToggleFavorite = {}, onToggleDownload = {},
        )
    }
}
