package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.model.SearchSuggestion
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppFilterChip
import com.cmarcillovargas.turismovalencia.ui.components.EmptyState
import com.cmarcillovargas.turismovalencia.ui.components.SectionLabel
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val FieldHeight = 48.dp
private val FocusBorder = 2.dp
private val RowIconSize = 28.dp

@Composable
fun BusquedaRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenOffer: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onAskValBot: (String) -> Unit,
    viewModel: BusquedaViewModel = viewModel(factory = containerFactory { BusquedaViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BusquedaScreen(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onOpenSuggestion = { suggestion ->
            viewModel.remember(uiState.query.ifBlank { suggestion.title })
            when (suggestion.kind) {
                SearchSuggestion.Kind.Dish, SearchSuggestion.Kind.Place -> onOpenItem(suggestion.target)
                SearchSuggestion.Kind.Offer -> onOpenOffer(suggestion.target)
                SearchSuggestion.Kind.Map -> onOpenMap(suggestion.target)
            }
        },
        onAskValBot = onAskValBot,
    )
}

@Composable
fun BusquedaScreen(
    uiState: BusquedaUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenSuggestion: (SearchSuggestion) -> Unit,
    onAskValBot: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SearchTopBar(uiState.query, onBack, onQueryChange, autoFocus) { uiState.suggestions.firstOrNull()?.let(onOpenSuggestion) }
        if (uiState.noResults) {
            NoResults(uiState.query, onQueryChange, onAskValBot)
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (uiState.suggestions.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.search_suggestions))
                    uiState.suggestions.forEach { suggestion ->
                        SuggestionRow(suggestion.kind.icon(), suggestion.title, suggestion.subtitle) { onOpenSuggestion(suggestion) }
                    }
                }
                if (uiState.recent.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.search_recent), Modifier.padding(top = Spacing.xs))
                    uiState.recent.forEach { text ->
                        SuggestionRow(Icons.Outlined.History, text, null) { onQueryChange(text) }
                    }
                }
                SectionLabel(stringResource(R.string.search_trends), Modifier.padding(top = Spacing.s))
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.m).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    uiState.trends.forEachIndexed { index, trend ->
                        TrendChip(trend, trendIcon(index), if (index == 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary) {
                            onQueryChange(trend)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    autoFocus: Boolean,
    onSearch: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs) {
        Row(
            modifier = Modifier.statusBarsPadding().padding(horizontal = Spacing.xxs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back), tint = MaterialTheme.colorScheme.secondary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.appColors.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                interactionSource = remember { MutableInteractionSource() },
                modifier = Modifier
                    .weight(1f)
                    .height(FieldHeight)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .clip(PillShape)
                    .border(
                        if (focused) FocusBorder else 1.dp,
                        if (focused) MaterialTheme.appColors.brand else MaterialTheme.colorScheme.outlineVariant,
                        PillShape,
                    ),
                decorationBox = { inner ->
                    Row(Modifier.fillMaxSize().padding(start = Spacing.m, end = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(stringResource(R.string.home_search_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            }
                            inner()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.search_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.width(Spacing.xs))
        }
    }
}

@Composable
private fun SuggestionRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(RowIconSize))
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendChip(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(Spacing.xl + Spacing.xs).clip(PillShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(Spacing.l - Spacing.xxs))
            Spacer(Modifier.width(Spacing.xs))
            Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}

@Composable
private fun NoResults(query: String, onQueryChange: (String) -> Unit, onAskValBot: (String) -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        EmptyState(
            icon = Icons.Outlined.Search,
            title = stringResource(R.string.search_empty_title),
            text = stringResource(R.string.search_empty_text, query),
            actionText = stringResource(R.string.search_ask_valbot),
            actionIcon = Icons.Outlined.Search,
            onAction = { onAskValBot(query) },
        )
        Spacer(Modifier.weight(1.2f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            AppFilterChip(stringResource(R.string.search_quick_horchata), false, { onQueryChange("horchata") }, leadingIcon = Icons.Outlined.Restaurant, height = Spacing.xl + Spacing.m)
            AppFilterChip(stringResource(R.string.search_quick_open_now), false, { onQueryChange("abierto") }, leadingIcon = Icons.Outlined.AccountBalance, height = Spacing.xl + Spacing.m)
        }
        Spacer(Modifier.height(Spacing.xl + Spacing.l))
    }
}

private fun SearchSuggestion.Kind.icon(): ImageVector = when (this) {
    SearchSuggestion.Kind.Dish -> Icons.Outlined.Restaurant
    SearchSuggestion.Kind.Place -> Icons.Outlined.AccountBalance
    SearchSuggestion.Kind.Offer -> Icons.Outlined.ConfirmationNumber
    SearchSuggestion.Kind.Map -> Icons.Outlined.LocationOn
}

private fun trendIcon(index: Int): ImageVector = when (index) {
    0 -> Icons.Outlined.LocalFireDepartment
    1 -> Icons.Outlined.AccountBalance
    else -> Icons.Outlined.Eco
}

private val PreviewSuggestions = listOf(
    SearchSuggestion("Paella valenciana", "Gastronomía · plato típico", SearchSuggestion.Kind.Dish, "paella"),
    SearchSuggestion("Taller de paella valenciana", "Experiencia · desde 45 €", SearchSuggestion.Kind.Offer, "taller-paella"),
    SearchSuggestion("Restaurantes de paella cerca", "Mapa · 12 resultados", SearchSuggestion.Kind.Map, "paella"),
)

@Preview(name = "B2 · Búsqueda", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BusquedaPreview() {
    TurismoValenciaTheme {
        BusquedaScreen(
            uiState = BusquedaUiState("pae", PreviewSuggestions, listOf("Mercado Central", "Fallas 2027"), listOf("Mascletà", "Oceanogràfic", "Albufera")),
            onBack = {}, onQueryChange = {}, onOpenSuggestion = {}, onAskValBot = {}, autoFocus = false,
        )
    }
}

@Preview(name = "H4 · Búsqueda sin resultados", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BusquedaEmptyPreview() {
    TurismoValenciaTheme {
        BusquedaScreen(
            uiState = BusquedaUiState("horchatería abierta 24h", emptyList(), emptyList(), emptyList()),
            onBack = {}, onQueryChange = {}, onOpenSuggestion = {}, onAskValBot = {}, autoFocus = false,
        )
    }
}
