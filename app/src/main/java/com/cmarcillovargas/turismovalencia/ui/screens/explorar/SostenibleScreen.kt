package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeSostenible
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.ui.components.DetailHero
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val HeroHeight = 300.dp
private val ThumbSize = 72.dp
private val RowShape = RoundedCornerShape(20.dp)
private val ThumbShape = RoundedCornerShape(12.dp)

@Composable
fun SostenibleRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: SostenibleViewModel = viewModel(factory = containerFactory { SostenibleViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SostenibleScreen(uiState, onBack, onOpenItem, viewModel::onToggleCoverFavorite)
}

@Composable
fun SostenibleScreen(
    uiState: SostenibleUiState,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cover = uiState.cover ?: return
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        DetailHero(
            image = cover.hero,
            category = cover.category,
            eyebrow = cover.eyebrow,
            title = cover.detailTitle ?: cover.title,
            stats = cover.stats,
            onBack = onBack,
            height = HeroHeight,
            accent = MaterialTheme.appColors.ecoLight,
            favorite = uiState.coverFavorite,
            onToggleFavorite = onToggleFavorite,
            imageDescription = cover.title,
        )
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RowShape)
                    .background(MaterialTheme.appColors.ecoContainer)
                    .padding(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(Spacing.xl))
                Spacer(Modifier.width(Spacing.m))
                Text(
                    text = buildAnnotatedString {
                        val bold = stringResource(R.string.eco_banner_bold)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                        append(" ")
                        append(stringResource(R.string.eco_banner_text))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            uiState.topics.forEach { topic -> TopicCard(topic) { onOpenItem(topic.id) } }
            Spacer(Modifier.size(Spacing.l))
        }
    }
}

@Composable
private fun TopicCard(topic: ContentItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RowShape).clickable(role = Role.Button, onClick = onClick),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.xxs / 2,
    ) {
        Row(Modifier.padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            PlaceImage(topic.thumb ?: topic.hero, topic.category, Modifier.size(ThumbSize).clip(ThumbShape), topic.title)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(topicIcon(topic.id), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(Spacing.l - Spacing.xxs))
                    Spacer(Modifier.width(Spacing.xxs))
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Text(text = topic.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.m))
            Spacer(Modifier.width(Spacing.xs))
        }
    }
}

private fun topicIcon(id: String): ImageVector = when (id) {
    "muevete" -> Icons.Outlined.PedalBike
    "viaje-responsable" -> Icons.Outlined.Groups
    "comercio-local" -> Icons.Outlined.LocationOn
    else -> Icons.Outlined.Eco
}

@Preview(name = "B7 · Turismo sostenible", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SostenblePreview() {
    TurismoValenciaTheme {
        SostenibleScreen(
            SostenibleUiState(
                cover = FakeSostenible.first { it.id == "ecoturismo" },
                topics = FakeSostenible.filter { it.subcategory == "Tema" },
                coverFavorite = true,
            ),
            onBack = {}, onOpenItem = {}, onToggleFavorite = {},
        )
    }
}
