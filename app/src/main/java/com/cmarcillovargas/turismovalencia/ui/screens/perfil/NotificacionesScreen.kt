package com.cmarcillovargas.turismovalencia.ui.screens.perfil

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.AppStrings
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.model.AppNotification
import com.cmarcillovargas.turismovalencia.data.model.NotificationGroup
import com.cmarcillovargas.turismovalencia.data.model.NotificationKind
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.SectionLabel
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class NotificacionesUiState(val items: List<AppNotification> = emptyList())

class NotificacionesViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<NotificacionesUiState> = kotlinx.coroutines.flow.combine(container.notifications.items) { lists ->
        NotificacionesUiState(lists.first().map { it.inLanguage(container.strings) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificacionesUiState())

    fun onRead(id: String) = container.notifications.markRead(id)
}

private val IconCircle = Spacing.xl + Spacing.xs

/** Título, texto y hora de las cinco notificaciones de demostración (`DemoData`), que la base de datos guarda en español. */
private class DemoTexts(@param:StringRes val title: Int, @param:StringRes val body: Int, @param:StringRes val whenLabel: Int)

private val DemoNotificationTexts = mapOf(
    "n1" to DemoTexts(R.string.reminder_mascleta_title, R.string.reminder_mascleta_text, R.string.notif_when_10min),
    "n2" to DemoTexts(R.string.notif_demo_2_title, R.string.notif_demo_2_body, R.string.notif_when_1h),
    "n3" to DemoTexts(R.string.notif_demo_3_title, R.string.notif_demo_3_body, R.string.notif_when_yesterday),
    "n4" to DemoTexts(R.string.notif_demo_4_title, R.string.notif_demo_4_body, R.string.notif_when_monday),
    "n5" to DemoTexts(R.string.notif_demo_5_title, R.string.notif_demo_5_body, R.string.notif_when_sunday),
)

/** Las de demostración salen en el idioma de la app; cualquier otra se enseña con el texto con que se guardó. */
private fun AppNotification.inLanguage(strings: AppStrings): AppNotification {
    val texts = DemoNotificationTexts[id] ?: return this
    return copy(title = strings.get(texts.title), body = strings.get(texts.body), whenLabel = strings.get(texts.whenLabel))
}

@Composable
fun NotificacionesRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenReservations: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenValBot: () -> Unit,
    viewModel: NotificacionesViewModel = viewModel(factory = containerFactory { NotificacionesViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotificacionesScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onOpen = { notification ->
            viewModel.onRead(notification.id)
            when {
                notification.itemId != null -> onOpenItem(notification.itemId)
                notification.kind == NotificationKind.Booking -> onOpenReservations()
                notification.kind == NotificationKind.Download -> onOpenDownloads()
                notification.kind == NotificationKind.Bot -> onOpenValBot()
            }
        },
    )
}

@Composable
fun NotificacionesScreen(
    uiState: NotificacionesUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpen: (AppNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AppTopBar(stringResource(R.string.common_notifications), onBack = onBack) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.notifications_settings), tint = MaterialTheme.colorScheme.secondary)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            listOf(NotificationGroup.Today to R.string.notifications_today, NotificationGroup.ThisWeek to R.string.res_this_week).forEach { (group, title) ->
                val rows = uiState.items.filter { it.group == group }
                if (rows.isNotEmpty()) {
                    item { SectionLabel(stringResource(title), Modifier.padding(top = Spacing.s)) }
                    rows.forEach { notification -> item(key = notification.id) { NotificationRow(notification) { onOpen(notification) } } }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.unread) MaterialTheme.appColors.brandSoft else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.Top,
    ) {
        val (icon, background, tint) = notification.kind.style()
        Box(Modifier.size(IconCircle).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        }
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(notification.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(notification.whenLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (notification.unread) {
            Box(Modifier.padding(start = Spacing.s, top = Spacing.xxs).size(Spacing.s).clip(CircleShape).background(MaterialTheme.appColors.brand))
        }
    }
}

private data class KindStyle(val icon: ImageVector, val background: Color, val tint: Color)

@Composable
private fun NotificationKind.style(): KindStyle {
    val app = MaterialTheme.appColors
    val colors = MaterialTheme.colorScheme
    return when (this) {
        NotificationKind.Event -> KindStyle(Icons.Outlined.LocalFireDepartment, colors.primaryContainer, app.brand)
        NotificationKind.Booking -> KindStyle(Icons.Outlined.EventAvailable, colors.tertiaryContainer, colors.onTertiaryContainer)
        NotificationKind.Download -> KindStyle(Icons.Outlined.CloudDownload, app.slateContainer, colors.secondary)
        NotificationKind.Eco -> KindStyle(Icons.Outlined.Eco, app.ecoContainer, colors.tertiary)
        NotificationKind.Bot -> KindStyle(Icons.Outlined.SmartToy, app.botContainer, app.botAccent)
    }
}

private val PreviewItems = listOf(
    AppNotification("1", NotificationKind.Event, NotificationGroup.Today, "Mascletà a las 14:00", "Plaza del Ayuntamiento · llega 30 min antes para coger sitio.", "Hace 10 min", true),
    AppNotification("2", NotificationKind.Booking, NotificationGroup.Today, "Reserva confirmada", "Paseo en barca por la Albufera · sáb 12 oct, 18:30.", "Hace 1 h", true),
    AppNotification("3", NotificationKind.Download, NotificationGroup.ThisWeek, "Fichas actualizadas", "4 lugares guardados se han actualizado para usarlos sin conexión.", "Ayer", false),
    AppNotification("4", NotificationKind.Eco, NotificationGroup.ThisWeek, "Ruta sostenible recomendada", "Recorre el Jardín del Turia en Valenbisi: 9 km sin coches.", "Lunes", false),
    AppNotification("5", NotificationKind.Bot, NotificationGroup.ThisWeek, "ValBot tiene novedades", "Ahora responde también en valenciano e inglés.", "Domingo", false),
)

@Preview(name = "G2 · Notificaciones", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificacionesPreview() {
    TurismoValenciaTheme { NotificacionesScreen(NotificacionesUiState(PreviewItems), {}, {}, {}) }
}
