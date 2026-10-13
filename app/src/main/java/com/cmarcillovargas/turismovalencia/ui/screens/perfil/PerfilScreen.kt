package com.cmarcillovargas.turismovalencia.ui.screens.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import com.cmarcillovargas.turismovalencia.ui.components.AppSwitch
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.SectionLabel
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.components.rememberLocationPermissionRequest
import com.cmarcillovargas.turismovalencia.ui.screens.bienvenida.IdiomaSheetHost
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

private val AvatarSize = 80.dp
private val GroupShape = RoundedCornerShape(24.dp)

private enum class Dialog { Location, TextSize, Privacy, Logout }

@Composable
fun PerfilRoute(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenPage: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = containerFactory { PerfilViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguage by remember { mutableStateOf(false) }
    // Elegir «Mientras se usa» o «Solo esta vez» sin tener el permiso del sistema abre su diálogo; lo que responda decide el ajuste.
    var pendingMode by remember { mutableStateOf(LocationMode.WhileUsing) }
    val askLocation = rememberLocationPermissionRequest { granted -> viewModel.onLocation(if (granted) pendingMode else LocationMode.Denied) }
    PerfilScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenLanguage = { showLanguage = true },
        onOpenDownloads = onOpenDownloads,
        onOpenContact = onOpenContact,
        onOpenPage = onOpenPage,
        onNotifications = viewModel::onNotifications,
        onLocation = { mode ->
            if (mode != LocationMode.Denied && !viewModel.locationPermissionGranted()) {
                pendingMode = mode
                askLocation(true)
            } else {
                viewModel.onLocation(mode)
            }
        },
        onTextScale = viewModel::onTextScale,
        onLogout = onLogout,
    )
    IdiomaSheetHost(visible = showLanguage, onDismiss = { showLanguage = false })
}

@Composable
fun PerfilScreen(
    uiState: PerfilUiState,
    onBack: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenPage: (String) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onLocation: (LocationMode) -> Unit,
    onTextScale: (TextScale) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(stringResource(R.string.profile_title), onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ProfileHeader(uiState)
            SectionLabel(stringResource(R.string.profile_prefs), Modifier.padding(top = Spacing.m))
            Group {
                SettingRow(Icons.Outlined.Translate, stringResource(R.string.common_language), stringResource(uiState.language.label()), onClick = onOpenLanguage) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Outlined.NotificationsNone, stringResource(R.string.common_notifications), stringResource(R.string.profile_notifications_text)) {
                    AppSwitch(uiState.notifications, onNotifications)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Outlined.GpsFixed, stringResource(R.string.profile_location), stringResource(uiState.location.label()), onClick = { dialog = Dialog.Location }) { Chevron() }
            }
            SectionLabel(stringResource(R.string.profile_offline_a11y), Modifier.padding(top = Spacing.m))
            Group {
                SettingRow(Icons.Outlined.CloudDownload, stringResource(R.string.downloads_title), stringResource(R.string.profile_downloads_text, uiState.offlineMb), onClick = onOpenDownloads) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Outlined.AccessibilityNew, stringResource(R.string.profile_text_size), stringResource(uiState.textScale.label()), onClick = { dialog = Dialog.TextSize }) { Chevron() }
            }
            SectionLabel(stringResource(R.string.profile_account), Modifier.padding(top = Spacing.m))
            Group {
                SettingRow(Icons.Outlined.SupportAgent, stringResource(R.string.profile_contact), stringResource(R.string.profile_contact_text), onClick = onOpenContact) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Outlined.Shield, stringResource(R.string.profile_privacy), null, onClick = { dialog = Dialog.Privacy }) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Outlined.Gavel, stringResource(R.string.legal_notice), null, onClick = { onOpenPage("AvisoLegal") }) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Outlined.PrivacyTip, stringResource(R.string.legal_privacy), null, onClick = { onOpenPage("PoliticaPrivacidad") }) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.profile_logout), null, onClick = { dialog = Dialog.Logout }) {}
            }
            Spacer(Modifier.height(Spacing.l))
        }
    }
    when (dialog) {
        Dialog.Location -> ChoiceDialog(
            title = stringResource(R.string.profile_location),
            options = LocationMode.entries.map { it to stringResource(it.label()) },
            selected = uiState.location,
            onSelect = { onLocation(it); dialog = null },
            onDismiss = { dialog = null },
        )

        Dialog.TextSize -> ChoiceDialog(
            title = stringResource(R.string.profile_text_size),
            options = TextScale.entries.map { it to stringResource(it.label()) },
            selected = uiState.textScale,
            onSelect = { onTextScale(it); dialog = null },
            onDismiss = { dialog = null },
        )

        Dialog.Privacy -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.profile_privacy)) },
            text = { Text(stringResource(R.string.profile_privacy_text)) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.common_close)) } },
            containerColor = MaterialTheme.colorScheme.surface,
        )

        Dialog.Logout -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.profile_logout_title)) },
            text = { Text(stringResource(R.string.profile_logout_text)) },
            confirmButton = {
                TextButton(onClick = { dialog = null; onLogout() }) { Text(stringResource(R.string.profile_logout), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.common_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface,
        )

        null -> Unit
    }
}

@Composable
private fun ProfileHeader(uiState: PerfilUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.appColors.brandSoft).padding(Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(AvatarSize).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.profile_initials), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSecondary)
        }
        Spacer(Modifier.height(Spacing.s))
        Text(stringResource(R.string.profile_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.profile_email_city), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            StatusChip(stringResource(R.string.profile_offline_mb, uiState.cardsMb), StatusTone.Success, icon = Icons.Outlined.Download)
            StatusChip(pluralStringResource(R.plurals.profile_reservations, uiState.reservations, uiState.reservations), StatusTone.Success, icon = Icons.Outlined.Check)
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.xxs / 2,
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String?, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = Spacing.m)
            .height(Spacing.xxl + Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Spacing.l))
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.m))
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(Spacing.xxl).selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.appColors.brand))
                        Spacer(Modifier.width(Spacing.s))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

private fun LanguageCode.label(): Int = when (this) {
    LanguageCode.ES -> R.string.idioma_es_nombre
    LanguageCode.VA -> R.string.idioma_va_nombre
    LanguageCode.EN -> R.string.idioma_en_nombre
    LanguageCode.FR -> R.string.idioma_fr_nombre
}

private fun LocationMode.label(): Int = when (this) {
    LocationMode.WhileUsing -> R.string.permiso_mientras_se_usa
    LocationMode.OnlyOnce -> R.string.permiso_solo_esta_vez
    LocationMode.Denied -> R.string.permiso_no_permitir
}

private fun TextScale.label(): Int = when (this) {
    TextScale.System -> R.string.profile_text_system
    TextScale.Small -> R.string.profile_text_small
    TextScale.Large -> R.string.profile_text_large
    TextScale.ExtraLarge -> R.string.profile_text_xlarge
}

@Preview(name = "G1 · Perfil y ajustes", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PerfilPreview() {
    TurismoValenciaTheme { PerfilScreen(PerfilUiState(offlineMb = 48, reservations = 3), {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}
