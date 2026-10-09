package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeAlojamientos
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventos
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.fake.FakeSostenible
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.ui.components.AppFilterChip
import com.cmarcillovargas.turismovalencia.ui.components.ChipStyle
import com.cmarcillovargas.turismovalencia.ui.components.FavoriteButton
import com.cmarcillovargas.turismovalencia.ui.components.FavoriteStyle
import com.cmarcillovargas.turismovalencia.ui.components.OfflineBanner
import com.cmarcillovargas.turismovalencia.ui.components.OfflineSnackbar
import com.cmarcillovargas.turismovalencia.ui.components.PageIndicator
import com.cmarcillovargas.turismovalencia.ui.components.PlaceGridCard
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.SectionTitle
import com.cmarcillovargas.turismovalencia.ui.screens.bienvenida.IdiomaSheetHost
import com.cmarcillovargas.turismovalencia.ui.screens.estados.InicioCargando
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.eyebrow
import com.cmarcillovargas.turismovalencia.ui.theme.screenTitle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Medidas del diseño (B1).
private val LogoSize = 36.dp
private val FeatureHeight = 176.dp
private val FeatureShape = RoundedCornerShape(20.dp)
private val SearchHeight = 48.dp
private val FilterButtonSize = 40.dp
private val GridPadding = 20.dp
private const val BadgeSize = 10

@Composable
fun InicioRoute(
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenTickets: () -> Unit,
    onOpenSustainable: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = containerFactory { InicioViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguage by remember { mutableStateOf(false) }
    InicioScreen(
        uiState = uiState,
        onOpenItem = onOpenItem,
        onOpenSearch = onOpenSearch,
        onOpenNotifications = onOpenNotifications,
        onOpenLanguage = { showLanguage = true },
        onSelectCategory = viewModel::onSelectCategory,
        onSeeAll = { onOpenCategory(uiState.selected ?: Category.QueVer) },
        onOpenTickets = onOpenTickets,
        onOpenSustainable = onOpenSustainable,
        onToggleFavorite = viewModel::onToggleFavorite,
        onToggleDownload = viewModel::onToggleDownload,
        onRetry = viewModel::onRetry,
    )
    IdiomaSheetHost(visible = showLanguage, onDismiss = { showLanguage = false })
}

@Composable
fun InicioScreen(
    uiState: InicioUiState,
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLanguage: () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onSeeAll: () -> Unit,
    onOpenTickets: () -> Unit,
    onOpenSustainable: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleDownload: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(uiState.hasUnread, uiState.language, onOpenLanguage, onOpenNotifications)
            if (uiState.offline) OfflineBanner()
            if (uiState.loading) {
                InicioCargando(onOpenSearch)
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xxl + Spacing.l)) {
                    item { HomeSearchField(onOpenSearch, Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) }
                    item {
                        FeaturedCarousel(
                            items = uiState.featured,
                            favorites = uiState.favorites,
                            onOpenItem = onOpenItem,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }
                    item {
                        CategoryChips(
                            selected = uiState.selected,
                            onSelectCategory = onSelectCategory,
                            onOpenTickets = onOpenTickets,
                            onOpenSustainable = onOpenSustainable,
                            modifier = Modifier.padding(top = Spacing.m),
                        )
                    }
                    item {
                        SectionTitle(
                            text = stringResource(R.string.home_nearby),
                            modifier = Modifier.padding(start = GridPadding, end = Spacing.m, top = Spacing.l, bottom = Spacing.s),
                            trailing = {
                                TextButton(onClick = onSeeAll) {
                                    Text(
                                        text = stringResource(R.string.home_see_all),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                    items(uiState.items.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = GridPadding).padding(bottom = GridPadding),
                            horizontalArrangement = Arrangement.spacedBy(GridPadding),
                        ) {
                            pair.forEach { item ->
                                val downloaded = item.id in uiState.downloaded
                                PlaceGridCard(
                                    item = item,
                                    favorite = item.id in uiState.favorites,
                                    downloaded = downloaded,
                                    dimmed = uiState.offline && !downloaded,
                                    onClick = { if (!uiState.offline || downloaded) onOpenItem(item.id) },
                                    onToggleFavorite = { onToggleFavorite(item.id) },
                                    onToggleDownload = { onToggleDownload(item.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (uiState.offline) {
            OfflineSnackbar(onRetry = onRetry, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.m))
        }
    }
}

@Composable
private fun HomeHeader(hasUnread: Boolean, language: String, onOpenLanguage: () -> Unit, onOpenNotifications: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs) {
        Row(
            modifier = Modifier.statusBarsPadding().height(56.dp).padding(start = Spacing.m, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_turismo_valencia),
                contentDescription = stringResource(R.string.splash_logo_descripcion),
                modifier = Modifier
                    .size(LogoSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(1.dp),
            )
            Spacer(Modifier.width(Spacing.s))
            Text(
                text = stringResource(R.string.splash_titulo),
                style = MaterialTheme.typography.screenTitle,
                color = MaterialTheme.appColors.brand,
            )
            Spacer(Modifier.width(Spacing.xxs))
            Text(
                text = stringResource(R.string.splash_lema),
                style = MaterialTheme.typography.screenTitle,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            LanguageChip(language, onOpenLanguage)
            IconButton(onClick = onOpenNotifications) {
                Box {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.common_notifications),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    if (hasUnread) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(BadgeSize.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.appColors.brand),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(language: String, onClick: () -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(LogoSize).clip(PillShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Language, contentDescription = stringResource(R.string.common_language), tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l - Spacing.xxs))
            Spacer(Modifier.width(Spacing.xxs))
            Text(text = language, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Campo de búsqueda del inicio: al tocarlo se abre la pantalla de búsqueda. */
@Composable
fun HomeSearchField(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = MaterialTheme.appColors.brand
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchHeight)
            .shadow(Spacing.xxs, PillShape)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = Spacing.m, end = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Spacing.l + Spacing.xxs))
        Spacer(Modifier.width(Spacing.s))
        Text(
            text = stringResource(R.string.home_search_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.size(FilterButtonSize).shadow(Spacing.xxs, CircleShape, ambientColor = brand, spotColor = brand).clip(CircleShape).background(brand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.common_filter), tint = Color.White, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        }
    }
}

@Composable
private fun FeaturedCarousel(
    items: List<ContentItem>,
    favorites: Set<String>,
    onOpenItem: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState { items.size }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(start = Spacing.m, end = Spacing.xxl),
        pageSpacing = Spacing.s,
        modifier = Modifier.fillMaxWidth().height(FeatureHeight),
    ) { index ->
        val item = items[index]
        Box(
            Modifier
                .fillMaxSize()
                .clip(FeatureShape)
                .clickable(onClick = { onOpenItem(item.id) }),
        ) {
            PlaceImage(item.feature, item.category, Modifier.fillMaxSize(), item.title)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                    ),
                ),
            )
            FavoriteButton(
                favorite = item.id in favorites,
                onToggle = { onToggleFavorite(item.id) },
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.s),
                style = FavoriteStyle.OnPhotoDark,
                circleSize = Spacing.xxl - Spacing.xs,
            )
            Column(Modifier.align(Alignment.BottomStart).padding(Spacing.m)) {
                Text(
                    text = (item.featureLabel ?: item.eyebrow).uppercase(),
                    style = MaterialTheme.typography.eyebrow,
                    color = MaterialTheme.appColors.gold,
                    maxLines = 1,
                )
                Text(text = item.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    text = item.featureSubtitle ?: item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                )
            }
            PageIndicator(
                pageCount = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.m),
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    selected: Category?,
    onSelectCategory: (Category?) -> Unit,
    onOpenTickets: () -> Unit,
    onOpenSustainable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        item { CategoryChip(R.string.cat_que_ver, Icons.Outlined.AccountBalance, selected == Category.QueVer) { onSelectCategory(Category.QueVer) } }
        item { CategoryChip(R.string.cat_gastronomia, Icons.Outlined.Restaurant, selected == Category.Gastronomia) { onSelectCategory(Category.Gastronomia) } }
        item { CategoryChip(R.string.cat_alojamientos, Icons.Outlined.Hotel, selected == Category.Alojamientos) { onSelectCategory(Category.Alojamientos) } }
        item { CategoryChip(R.string.cat_eventos, Icons.Outlined.LocalFireDepartment, selected == Category.Eventos) { onSelectCategory(Category.Eventos) } }
        item { CategoryChip(R.string.cat_tickets, Icons.Outlined.ConfirmationNumber, false, onOpenTickets) }
        item { CategoryChip(R.string.cat_sostenible, Icons.Outlined.Eco, false, onOpenSustainable) }
    }
}

@Composable
private fun CategoryChip(label: Int, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    AppFilterChip(
        label = stringResource(label),
        selected = selected,
        onClick = onClick,
        style = ChipStyle.Brand,
        leadingIcon = icon,
        height = Spacing.xl + Spacing.xxs,
    )
}

private val PreviewItems = (FakeQueVer + FakeAlojamientos + FakeEventos + FakeSostenible).let { all ->
    listOf("mercado-central", "albufera-barca", "paella", "playas").mapNotNull { id -> all.firstOrNull { it.id == id } ?: com.cmarcillovargas.turismovalencia.data.fake.FakeGastronomia.firstOrNull { it.id == id } }
}
private val PreviewFeatured = (FakeEventos + FakeQueVer).filter { it.feature != null }

@Composable
private fun InicioPreview(state: InicioUiState) {
    TurismoValenciaTheme {
        InicioScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

private val PreviewState = InicioUiState(
    loading = false,
    featured = PreviewFeatured,
    items = PreviewItems,
    favorites = setOf("albufera-barca"),
    downloaded = setOf("mercado-central", "paella"),
    hasUnread = true,
)

@Preview(name = "B1 · Inicio", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InicioLoadedPreview() = InicioPreview(PreviewState)

@Preview(name = "H1 · Inicio sin conexión", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InicioOfflinePreview() = InicioPreview(PreviewState.copy(offline = true))

@Preview(name = "H2 · Inicio cargando", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun InicioLoadingPreview() = InicioPreview(PreviewState.copy(loading = true, hasUnread = true))
