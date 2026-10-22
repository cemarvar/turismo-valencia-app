package com.cmarcillovargas.turismovalencia.ui.screens.explorar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.appLanguage
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventos
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.InfoKind
import com.cmarcillovargas.turismovalencia.ui.components.InfoNotice
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppSwitch
import com.cmarcillovargas.turismovalencia.ui.components.BottomActionBar
import com.cmarcillovargas.turismovalencia.ui.components.ContactSummaryCard
import com.cmarcillovargas.turismovalencia.ui.components.DetailHero
import com.cmarcillovargas.turismovalencia.ui.components.InfoItemRow
import com.cmarcillovargas.turismovalencia.ui.components.NotificationAccessResult
import com.cmarcillovargas.turismovalencia.ui.components.OpenSettingsDialog
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.SectionTitle
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.components.rememberNotificationAccessRequest
import com.cmarcillovargas.turismovalencia.ui.components.webBlocks
import com.cmarcillovargas.turismovalencia.util.openNotificationSettings
import com.cmarcillovargas.turismovalencia.util.openUrl
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import java.util.Calendar

private val GalleryHeight = 84.dp
private val ActWidth = 150.dp
private val ActHeight = 104.dp
private val MediaShape = RoundedCornerShape(12.dp)

@Composable
fun DetalleRoute(
    id: String,
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenContact: () -> Unit,
    viewModel: DetalleViewModel = viewModel(key = "detalle-$id", factory = containerFactory { DetalleViewModel(it, id) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // El aviso necesita que el sistema deje notificar: al encenderlo se pide el permiso (Android 13+) y, si el sistema ya no
    // pregunta, se explica cómo activarlo en los ajustes del móvil.
    var showBlocked by remember { mutableStateOf(false) }
    val askNotifications = rememberNotificationAccessRequest { result -> if (result == NotificationAccessResult.NeedsSettings) showBlocked = true }
    if (showBlocked) {
        OpenSettingsDialog(
            title = stringResource(R.string.notifications_blocked_title),
            text = stringResource(R.string.notifications_blocked_text),
            onOpenSettings = context::openNotificationSettings,
            onDismiss = { showBlocked = false },
        )
    }
    DetalleScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleFavorite = viewModel::onToggleFavorite,
        onToggleDownload = viewModel::onToggleDownload,
        onReminderChange = { on ->
            viewModel.onReminderChange(on)
            if (on) askNotifications()
        },
        onShare = { uiState.item?.let { share(context, it) } },
        onCalendar = { uiState.item?.let { addToCalendar(context, it) } },
        onOpenRoute = { onOpenRoute(id) },
        onOpenMap = { onOpenMap(id) },
        onOpenUrl = context::openUrl,
        onOpenContact = onOpenContact,
    )
}

@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onCalendar: () -> Unit,
    onOpenRoute: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = remember { mutableStateMapOf<Int, String>() }
    val appLanguage = LocalContext.current.appLanguage()
    val item = uiState.item
    if (item == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_not_found), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            item(key = "hero") {
            DetailHero(
                image = item.hero ?: item.thumb,
                category = item.category,
                eyebrow = item.detailEyebrow ?: "${item.eyebrow} · ${item.area}",
                title = item.detailTitle ?: item.title,
                stats = item.stats,
                onBack = onBack,
                onShare = onShare,
                favorite = uiState.favorite,
                onToggleFavorite = onToggleFavorite,
                imageDescription = item.title,
                accent = if (item.category == Category.Sostenible) MaterialTheme.appColors.ecoLight else MaterialTheme.appColors.gold,
                assetPath = item.heroAsset ?: item.thumbAsset,
            )
            }
            item(key = "summary") {
            Column(Modifier.padding(horizontal = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                if (item.category != Category.Eventos) Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    if (item.isFree) StatusChip(stringResource(R.string.detail_free), StatusTone.Success, height = Spacing.l, small = true)
                    StatusChip(
                        text = stringResource(if (uiState.downloaded) R.string.detail_available_offline else R.string.chip_download),
                        tone = if (uiState.downloaded) StatusTone.Success else StatusTone.Neutral,
                        icon = Icons.Outlined.Download,
                        onClick = onToggleDownload,
                        height = Spacing.l,
                        small = true,
                    )
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.gallery.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        item.gallery.forEach { image ->
                            PlaceImage(image, item.category, Modifier.weight(1f).height(GalleryHeight).clip(MediaShape))
                        }
                    }
                }
                if (item.acts.isNotEmpty()) {
                    Acts(item, uiState.reminderAvailable, uiState.reminderOn, onReminderChange)
                }
                if (item.info.isNotEmpty()) {
                    Text(stringResource(R.string.detail_info), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        item.info.forEach { row -> InfoItemRow(row.kind.icon(), row.title, row.text) }
                    }
                }
            }
            }
            uiState.page?.let { page ->
                item(key = "guide") { SectionTitle(stringResource(R.string.detail_guide), Modifier.padding(horizontal = Spacing.m).padding(top = Spacing.s)) }
                // La guía completa es el texto de la web: se avisa si no está traducida al idioma de la app (sigue en español).
                if (page.textLanguage != appLanguage) {
                    item(key = "guide-spanish-only") { InfoNotice(stringResource(R.string.web_only_spanish), Modifier.padding(horizontal = Spacing.m)) }
                }
                webBlocks(page.blocks, filters, onOpenUrl)
                item(key = "contact") { ContactSummaryCard(onOpenContact, Modifier.padding(horizontal = Spacing.m).padding(bottom = Spacing.m)) }
            }
        }
        BottomActionBar {
            if (item.isEvent) {
                AppButton(stringResource(R.string.detail_calendar), onCalendar, Modifier.weight(1f), AppButtonStyle.Outlined, leadingIcon = Icons.Outlined.CalendarMonth)
                AppButton(stringResource(R.string.common_map), onOpenMap, Modifier.weight(1.2f), leadingIcon = Icons.Outlined.Map)
            } else {
                if (uiState.downloaded) {
                    AppButton(stringResource(R.string.detail_saved), onToggleDownload, Modifier.weight(1f), AppButtonStyle.Tonal, leadingIcon = Icons.Outlined.Check)
                } else {
                    AppButton(stringResource(R.string.detail_save), onToggleDownload, Modifier.weight(1f), AppButtonStyle.Outlined, leadingIcon = Icons.Outlined.Download)
                }
                if (item.distanceMeters > 0) {
                    AppButton(stringResource(R.string.detail_how_to_get), onOpenRoute, Modifier.weight(1.2f), leadingIcon = Icons.Outlined.NearMe)
                } else {
                    AppButton(stringResource(R.string.common_map), onOpenMap, Modifier.weight(1.2f), leadingIcon = Icons.Outlined.Map)
                }
            }
        }
    }
}

@Composable
private fun Acts(item: ContentItem, reminderAvailable: Boolean, reminderOn: Boolean, onReminderChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionTitle(stringResource(R.string.detail_acts))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            itemsIndexed(item.acts) { _, act ->
                Column(Modifier.width(ActWidth)) {
                    PlaceImage(act.image, item.category, Modifier.size(ActWidth, ActHeight).clip(MediaShape), act.title)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(act.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(act.whenLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        if (reminderAvailable) Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MediaShape)
                .background(MaterialTheme.appColors.brandSoft)
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l - Spacing.xxs))
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.detail_remind_title, item.acts.first().title.replaceFirstChar { it.lowercase() }),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(R.string.detail_remind_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppSwitch(reminderOn, onReminderChange)
        }
    }
}

private fun InfoKind.icon() = when (this) {
    InfoKind.Schedule -> Icons.Outlined.AccessTime
    InfoKind.Address -> Icons.Outlined.LocationOn
    InfoKind.Price -> Icons.Outlined.Euro
    InfoKind.Transport -> Icons.Outlined.DirectionsBus
    InfoKind.Tip -> Icons.Outlined.Lightbulb
}

private fun share(context: Context, item: ContentItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, "${item.detailTitle ?: item.title}: ${item.summary}")
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.detail_share_chooser)))
}

/** Abre la app de calendario con el evento rellenado (no necesita permisos). */
private fun addToCalendar(context: Context, item: ContentItem) {
    val start = Calendar.getInstance().apply {
        if (item.id == "fallas") {
            // Las Fallas son del 1 al 19 de marzo; se propone la próxima edición.
            val year = get(Calendar.YEAR) + if (get(Calendar.MONTH) >= Calendar.MARCH) 1 else 0
            set(year, Calendar.MARCH, 1, 14, 0, 0)
        } else {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, if (item.id == "fallas") 18 else 0); add(Calendar.HOUR_OF_DAY, 2) }
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, item.detailTitle ?: item.title)
        putExtra(CalendarContract.Events.DESCRIPTION, item.summary)
        putExtra(CalendarContract.Events.EVENT_LOCATION, item.area)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.timeInMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.timeInMillis)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.detail_no_calendar, Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "B4 · Detalle de lugar", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DetallePlacePreview() {
    TurismoValenciaTheme {
        DetalleScreen(
            DetalleUiState(item = FakeQueVer.first { it.id == "mercado-central" }, downloaded = true, favorite = true),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "B5 · Detalle de evento", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DetalleEventPreview() {
    TurismoValenciaTheme {
        DetalleScreen(
            DetalleUiState(item = FakeEventos.first { it.id == "fallas" }, favorite = true, reminderAvailable = true, reminderOn = true),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
