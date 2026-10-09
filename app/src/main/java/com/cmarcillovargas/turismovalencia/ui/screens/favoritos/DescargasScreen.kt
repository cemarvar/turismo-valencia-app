package com.cmarcillovargas.turismovalencia.ui.screens.favoritos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppSwitch
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.BottomActionBar
import com.cmarcillovargas.turismovalencia.ui.components.PlaceListCard
import com.cmarcillovargas.turismovalencia.ui.components.SectionLabel
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val CardShape20 = RoundedCornerShape(20.dp)
private val BarHeight = 8.dp

@Composable
fun DescargasRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: DescargasViewModel = viewModel(factory = containerFactory { DescargasViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DescargasScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenItem = onOpenItem,
        onWifiOnly = viewModel::onWifiOnly,
        onAutoUpdate = viewModel::onAutoUpdate,
        onRemove = viewModel::onRemove,
        onRemoveAll = viewModel::onRemoveAll,
        onUpdate = viewModel::onUpdate,
    )
}

@Composable
fun DescargasScreen(
    uiState: DescargasUiState,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onWifiOnly: (Boolean) -> Unit,
    onAutoUpdate: (Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onRemoveAll: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(stringResource(R.string.downloads_title), onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            StorageCard(uiState)
            SettingsCard(uiState, onWifiOnly, onAutoUpdate)
            SectionLabel(stringResource(R.string.downloads_content), Modifier.padding(top = Spacing.xs))
            if (uiState.items.isEmpty()) {
                Text(
                    stringResource(R.string.downloads_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                )
            }
            uiState.items.forEach { item ->
                PlaceListCard(
                    item = item,
                    favorite = false,
                    onClick = { onOpenItem(item.id) },
                    onToggleFavorite = {},
                    eyebrow = stringResource(R.string.downloads_downloaded),
                    showRating = false,
                    locationText = stringResource(if (item.isEvent) R.string.downloads_event_size else R.string.downloads_place_size, item.offlineMb),
                    trailing = {
                        IconButton(onClick = { onRemove(item.id) }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.downloads_remove), tint = MaterialTheme.colorScheme.secondary)
                        }
                    },
                )
            }
        }
        BottomActionBar {
            AppButton(stringResource(R.string.downloads_clear), { confirmClear = true }, Modifier.weight(1f), AppButtonStyle.Danger, leadingIcon = Icons.Outlined.Delete, enabled = uiState.items.isNotEmpty())
            AppButton(
                stringResource(if (uiState.updating) R.string.downloads_updating else R.string.downloads_update),
                onUpdate,
                Modifier.weight(1.2f),
                leadingIcon = Icons.Outlined.Refresh,
                enabled = !uiState.updating,
            )
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.downloads_clear_title)) },
            text = { Text(stringResource(R.string.downloads_clear_text)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onRemoveAll() }) {
                    Text(stringResource(R.string.downloads_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.common_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun StorageCard(uiState: DescargasUiState) {
    val brand = MaterialTheme.appColors.brand
    val navy = MaterialTheme.colorScheme.secondary
    val mapMb = if (uiState.mapDownloaded) uiState.mapMb else 0
    Surface(Modifier.fillMaxWidth(), shape = CardShape20, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column(Modifier.padding(Spacing.m)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.downloads_used_mb, uiState.usedMb), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.downloads_quota, uiState.quotaMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(painterResource(R.drawable.ic_database), contentDescription = null, tint = brand, modifier = Modifier.size(Spacing.l))
            }
            Spacer(Modifier.height(Spacing.s))
            Row(
                Modifier.fillMaxWidth().height(BarHeight).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                val total = uiState.quotaMb.toFloat()
                if (mapMb > 0) Box(Modifier.weight(mapMb / total).fillMaxHeight().background(brand))
                if (uiState.cardsMb > 0) Box(Modifier.weight(uiState.cardsMb / total).fillMaxHeight().background(navy))
                Spacer(Modifier.weight((total - mapMb - uiState.cardsMb).coerceAtLeast(1f) / total))
            }
            Spacer(Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                LegendItem(brand, stringResource(R.string.downloads_maps_mb, mapMb))
                LegendItem(navy, stringResource(R.string.downloads_cards_mb, uiState.cardsMb))
            }
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(Spacing.s).clip(CircleShape).background(color))
        Spacer(Modifier.width(Spacing.xs))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsCard(uiState: DescargasUiState, onWifiOnly: (Boolean) -> Unit, onAutoUpdate: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape20, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column {
            SettingRow(Icons.Outlined.WifiOff, stringResource(R.string.downloads_wifi_only), stringResource(R.string.downloads_wifi_only_text)) {
                AppSwitch(uiState.wifiOnly, onWifiOnly)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow(Icons.Outlined.Refresh, stringResource(R.string.downloads_auto_update), stringResource(R.string.downloads_auto_update_text)) {
                AppSwitch(uiState.autoUpdate, onAutoUpdate)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow(Icons.Outlined.Map, stringResource(R.string.downloads_map_title), stringResource(R.string.downloads_map_text, uiState.mapMb)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(Spacing.xl))
            }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, text: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Spacing.l))
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Preview(name = "E2 · Descargas sin conexión", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DescargasPreview() {
    TurismoValenciaTheme {
        DescargasScreen(
            DescargasUiState(items = FakeQueVer.filter { it.id == "mercado-central" || it.id == "lonja-seda" }, cardsMb = 14),
            {}, {}, {}, {}, {}, {}, {},
        )
    }
}
