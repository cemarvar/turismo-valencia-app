package com.cmarcillovargas.turismovalencia.ui.screens.mapa

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppFilterChip
import com.cmarcillovargas.turismovalencia.ui.components.ChipStyle
import com.cmarcillovargas.turismovalencia.ui.components.Eyebrow
import com.cmarcillovargas.turismovalencia.ui.components.FavoriteButton
import com.cmarcillovargas.turismovalencia.ui.components.FavoriteStyle
import com.cmarcillovargas.turismovalencia.ui.components.MapCamera
import com.cmarcillovargas.turismovalencia.ui.components.MapCanvas
import com.cmarcillovargas.turismovalencia.ui.components.MapMarker
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.RatingRow
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.components.dotColor
import com.cmarcillovargas.turismovalencia.ui.components.rememberMapCamera
import com.cmarcillovargas.turismovalencia.ui.theme.BottomSheetShape
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.formatDistance
import com.cmarcillovargas.turismovalencia.util.walkMinutes

private val SearchHeight = 48.dp
private val ControlSize = 44.dp
private val ThumbShape = RoundedCornerShape(16.dp)
private val ZoomShape = RoundedCornerShape(12.dp)
private const val ZoomStep = 1.35f

@Composable
fun MapaRoute(
    focus: String?,
    onOpenItem: (String) -> Unit,
    onOpenRoute: (String) -> Unit,
    initialQuery: String? = null,
    viewModel: MapaViewModel = viewModel(key = "mapa-$focus", factory = containerFactory { MapaViewModel(it, focus) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val camera = rememberMapCamera()
    val density = LocalDensity.current.density
    LaunchedEffect(initialQuery) { if (!initialQuery.isNullOrBlank()) viewModel.setQuery(initialQuery) }
    // Si llegamos desde una ficha, el mapa se abre centrado en ella.
    var centered by remember(focus) { mutableStateOf(false) }
    LaunchedEffect(focus, camera.viewport) {
        if (!centered && focus != null && camera.viewport != Size.Zero) {
            viewModel.uiState.value.selected?.let { camera.centerOn(it.mapPoint, density) }
            centered = true
        }
    }
    MapaScreen(
        uiState = uiState,
        camera = camera,
        onQueryChange = viewModel::onQueryChange,
        onFilter = viewModel::onFilter,
        onToggleSaved = viewModel::onToggleSaved,
        onClearFilters = viewModel::onClearFilters,
        onSelect = viewModel::onSelect,
        onToggleFavorite = viewModel::onToggleFavorite,
        onOpenItem = onOpenItem,
        onOpenRoute = onOpenRoute,
    )
}

@Composable
fun MapaScreen(
    uiState: MapaUiState,
    camera: MapCamera,
    onQueryChange: (String) -> Unit,
    onFilter: (Category?) -> Unit,
    onToggleSaved: () -> Unit,
    onClearFilters: () -> Unit,
    onSelect: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    Box(modifier.fillMaxSize()) {
        MapCanvas(
            modifier = Modifier.fillMaxSize(),
            markers = uiState.markers,
            selectedId = uiState.selected?.id,
            onMarkerClick = onSelect,
            showUser = true,
            camera = camera,
        )
        Column(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = Spacing.s)) {
            MapSearchField(uiState.query, onQueryChange, onClearFilters, Modifier.padding(horizontal = Spacing.m))
            Spacer(Modifier.height(Spacing.s))
            CategoryChips(uiState.filter, onFilter)
            Spacer(Modifier.height(Spacing.s))
            ZoomControl(
                onZoomIn = { camera.zoomBy(ZoomStep, density) },
                onZoomOut = { camera.zoomBy(1 / ZoomStep, density) },
                modifier = Modifier.padding(start = Spacing.m),
            )
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.align(Alignment.End).padding(end = Spacing.m, bottom = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                RoundControl(selected = uiState.onlySaved, onClick = onToggleSaved, description = stringResource(R.string.map_saved_only)) {
                    Icon(Icons.Outlined.Layers, null, tint = MaterialTheme.colorScheme.secondary)
                }
                Box(
                    modifier = Modifier
                        .size(ControlSize)
                        .shadow(Spacing.xxs, ThumbShape)
                        .clip(ThumbShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(role = Role.Button) { camera.centerOn(com.cmarcillovargas.turismovalencia.ui.components.UserLocation, density) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MyLocation, stringResource(R.string.map_locate), tint = MaterialTheme.appColors.brand)
                }
            }
            uiState.selected?.let { item ->
                SelectedSheet(item, uiState.favorite, uiState.downloaded, onToggleFavorite, { onOpenRoute(item.id) }, { onOpenItem(item.id) })
            }
        }
    }
}

@Composable
private fun MapSearchField(query: String, onQueryChange: (String) -> Unit, onTune: () -> Unit, modifier: Modifier = Modifier) {
    val brand = MaterialTheme.appColors.brand
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchHeight)
            .shadow(Spacing.xxs, PillShape)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = Spacing.m, end = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(Spacing.s))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(brand),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text(stringResource(R.string.map_search_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(Spacing.xxl - Spacing.xs)
                .shadow(Spacing.xxs, CircleShape, ambientColor = brand, spotColor = brand)
                .clip(CircleShape)
                .background(brand)
                .clickable(role = Role.Button, onClick = onTune),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Tune, stringResource(R.string.map_clear_filters), tint = Color.White, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        }
    }
}

@Composable
private fun CategoryChips(selected: Category?, onFilter: (Category?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        item {
            AppFilterChip(stringResource(R.string.map_all), selected == null, { onFilter(null) }, style = ChipStyle.Brand, leadingIcon = Icons.Outlined.Check.takeIf { selected == null }, height = Spacing.xl + Spacing.xxs, textStyle = MaterialTheme.typography.labelLarge)
        }
        listOf(
            Category.QueVer to R.string.cat_que_ver,
            Category.Gastronomia to R.string.cat_gastronomia,
            Category.Alojamientos to R.string.cat_alojamientos,
            Category.Sostenible to R.string.cat_sostenible,
            Category.Eventos to R.string.cat_eventos,
        ).forEach { (category, label) ->
            item {
                AppFilterChip(
                    label = stringResource(label),
                    selected = selected == category,
                    onClick = { onFilter(category) },
                    style = ChipStyle.Brand,
                    leadingIcon = Icons.Outlined.Check.takeIf { selected == category },
                    dotColor = category.dotColor(),
                    height = Spacing.xl + Spacing.xxs,
                    textStyle = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ZoomControl(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.width(ControlSize - Spacing.xxs), shape = ZoomShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs) {
        Column {
            Box(Modifier.size(ControlSize - Spacing.xxs).clickable(role = Role.Button, onClick = onZoomIn), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Add, stringResource(R.string.map_zoom_in), tint = MaterialTheme.colorScheme.secondary)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.size(ControlSize - Spacing.xxs).clickable(role = Role.Button, onClick = onZoomOut), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Remove, stringResource(R.string.map_zoom_out), tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun RoundControl(selected: Boolean, onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(ControlSize)
            .shadow(Spacing.xxs, CircleShape)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SelectedSheet(
    item: ContentItem,
    favorite: Boolean,
    downloaded: Boolean,
    onToggleFavorite: () -> Unit,
    onRoute: () -> Unit,
    onDetails: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = BottomSheetShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.s) {
        Column(Modifier.padding(horizontal = Spacing.m).padding(top = Spacing.xs, bottom = Spacing.m)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp, 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.controlInactive),
            )
            Spacer(Modifier.height(Spacing.s))
            Row(verticalAlignment = Alignment.Top) {
                PlaceImage(item.thumb, item.category, Modifier.size(84.dp).clip(ThumbShape), item.title)
                Spacer(Modifier.width(Spacing.s))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Eyebrow("${stringResource((item.mapCategory ?: item.category).label())} · ${item.area}")
                    Text(item.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.rating != null) {
                            RatingRow(item.rating, item.ratingCount)
                            Spacer(Modifier.width(Spacing.s))
                        }
                        Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.m + Spacing.xxs))
                        Spacer(Modifier.width(Spacing.xxs))
                        Text(
                            "${formatDistance(item.distanceMeters)} · ${walkMinutes(item.distanceMeters)} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FavoriteButton(favorite, onToggleFavorite, style = FavoriteStyle.Tinted, circleSize = Spacing.xxl - Spacing.xs)
            }
            Spacer(Modifier.height(Spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (item.openUntil != null) {
                    Icon(Icons.Outlined.AccessTime, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(Spacing.l - Spacing.xxs))
                    Text(item.openUntil, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                if (downloaded) {
                    StatusChip(stringResource(R.string.chip_offline_ready), StatusTone.Success, icon = Icons.Outlined.Download)
                }
            }
            Spacer(Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                AppButton(stringResource(R.string.detail_how_to_get), onRoute, Modifier.weight(1f), leadingIcon = Icons.Outlined.NearMe)
                AppButton(stringResource(R.string.map_details), onDetails, Modifier.weight(1f), AppButtonStyle.Outlined, trailingIcon = Icons.Outlined.ChevronRight)
            }
        }
    }
}

@Composable
private fun Category.label(): Int = when (this) {
    Category.QueVer -> R.string.cat_que_ver
    Category.Gastronomia -> R.string.cat_gastronomia
    Category.Alojamientos -> R.string.cat_alojamientos
    Category.Eventos -> R.string.cat_eventos
    Category.Sostenible -> R.string.cat_sostenible
}

@Preview(name = "C1 · Mapa", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MapaPreview() {
    TurismoValenciaTheme {
        val item = FakeQueVer.first { it.id == "mercado-central" }
        MapaScreen(
            uiState = MapaUiState(
                markers = com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer.map { MapMarker(it.id, it.mapPoint, it.mapCategory ?: it.category, it.title) },
                selected = item,
                favorite = true,
                downloaded = true,
            ),
            camera = rememberMapCamera(),
            onQueryChange = {}, onFilter = {}, onToggleSaved = {}, onClearFilters = {}, onSelect = {}, onToggleFavorite = {}, onOpenItem = {}, onOpenRoute = {},
        )
    }
}
