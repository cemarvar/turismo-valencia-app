package com.cmarcillovargas.turismovalencia.ui.screens.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.ContactSummaryCard
import com.cmarcillovargas.turismovalencia.ui.components.DetailHero
import com.cmarcillovargas.turismovalencia.ui.components.webBlocks
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.util.openUrl
import com.cmarcillovargas.turismovalencia.util.shareText
import com.cmarcillovargas.turismovalencia.util.webUrlOf

private val HeroHeight = 280.dp

@Composable
fun WebPageRoute(
    id: String,
    onBack: () -> Unit,
    onOpenContact: () -> Unit,
    viewModel: WebPageViewModel = viewModel(key = "pagina-$id", factory = containerFactory { WebPageViewModel(it, id) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooser = stringResource(R.string.detail_share_chooser)
    val shareBody = uiState.page?.let { stringResource(R.string.web_share_text, it.title, webUrlOf(it.id)) }
    WebPageScreen(
        uiState = uiState,
        onBack = onBack,
        onShare = { uiState.page?.let { page -> shareBody?.let { context.shareText(page.title, it, chooser) } } },
        onOpenWeb = { uiState.page?.let { context.openUrl(webUrlOf(it.id)) } },
        onOpenUrl = context::openUrl,
        onOpenContact = onOpenContact,
    )
}

@Composable
fun WebPageScreen(
    uiState: WebPageUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenWeb: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenContact: () -> Unit,
    modifier: Modifier = Modifier,
    filters: SnapshotStateMap<Int, String> = remember { mutableStateMapOf() },
) {
    val page = uiState.page
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            page == null -> {
                AppTopBar(title = "", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (uiState.loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    else Text(stringResource(R.string.detail_not_found), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            page.family == WebFamily.Legal -> {
                AppTopBar(title = page.title, onBack = onBack)
                PageContent(page, filters, onOpenUrl, onOpenContact, hero = false, onBack = onBack, onShare = onShare, onOpenWeb = onOpenWeb)
            }
            else -> PageContent(page, filters, onOpenUrl, onOpenContact, hero = true, onBack = onBack, onShare = onShare, onOpenWeb = onOpenWeb)
        }
    }
}

@Composable
private fun PageContent(
    page: WebPage,
    filters: SnapshotStateMap<Int, String>,
    onOpenUrl: (String) -> Unit,
    onOpenContact: () -> Unit,
    hero: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenWeb: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState(), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (hero) {
            item(key = "hero") {
                DetailHero(
                    image = null,
                    category = page.family.category(),
                    eyebrow = page.eyebrow ?: page.family.name,
                    title = page.title,
                    stats = emptyList(),
                    onBack = onBack,
                    onShare = onShare,
                    height = HeroHeight,
                    imageDescription = page.title,
                    assetPath = page.hero,
                )
            }
        }
        if (page.lead.isNotEmpty()) {
            item(key = "lead") {
                Column(Modifier.padding(horizontal = Spacing.m).padding(top = if (hero) 0.dp else Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    page.lead.forEachIndexed { index, text ->
                        Text(
                            text = text,
                            style = if (index == 0 && hero) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        webBlocks(page.blocks, filters, onOpenUrl, legal = page.family == WebFamily.Legal)
        item(key = "web") {
            AppButton(
                text = stringResource(R.string.web_view_on_web),
                onClick = onOpenWeb,
                modifier = Modifier.padding(horizontal = Spacing.m).padding(top = Spacing.m).fillMaxWidth(),
                style = AppButtonStyle.Outlined,
                trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        }
        item(key = "contact") { ContactSummaryCard(onOpenContact, Modifier.padding(horizontal = Spacing.m).padding(bottom = Spacing.l)) }
    }
}

private fun WebFamily.category(): Category = when (this) {
    WebFamily.Gastronomia -> Category.Gastronomia
    WebFamily.Alojamientos -> Category.Alojamientos
    WebFamily.Eventos, WebFamily.Planes -> Category.Eventos
    WebFamily.Sostenible -> Category.Sostenible
    else -> Category.QueVer
}
