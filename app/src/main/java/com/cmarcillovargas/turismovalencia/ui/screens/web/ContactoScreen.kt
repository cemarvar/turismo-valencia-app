package com.cmarcillovargas.turismovalencia.ui.screens.web

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.MapPlaceholder
import com.cmarcillovargas.turismovalencia.ui.components.SectionLabel
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.dial
import com.cmarcillovargas.turismovalencia.util.email
import com.cmarcillovargas.turismovalencia.util.openMapSearch
import com.cmarcillovargas.turismovalencia.util.openUrl

// Medidas del diseño: el pie de la web (mapa, contacto, redes y enlaces legales) como pantalla propia.
private val MapHeight = 180.dp
private val RowIcon = Spacing.xl + Spacing.xs

@Composable
fun ContactoRoute(onBack: () -> Unit, onOpenPage: (String) -> Unit) {
    val context = LocalContext.current
    val office = stringResource(R.string.contact_office)
    val phone = stringResource(R.string.contact_phone)
    val mail = stringResource(R.string.contact_email)
    ContactoScreen(
        onBack = onBack,
        onCall = { context.dial(phone) },
        onEmail = { context.email(mail) },
        onOpenMap = { context.openMapSearch(office) },
        onOpenUrl = context::openUrl,
        onOpenPage = onOpenPage,
    )
}

@Composable
fun ContactoScreen(
    onBack: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(title = stringResource(R.string.contact_title), onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.contact_brand), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.contact_brand_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OfficeMap(onOpenMap)
            InfoCard {
                ContactRow(Icons.Outlined.CalendarMonth, stringResource(R.string.contact_hours), onClick = null)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ContactRow(Icons.Outlined.Call, stringResource(R.string.contact_phone), onCall)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ContactRow(Icons.Outlined.Email, stringResource(R.string.contact_email), onEmail)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ContactRow(Icons.Outlined.LocationOn, stringResource(R.string.contact_location), onOpenMap)
            }
            SectionLabel(stringResource(R.string.contact_follow), Modifier.padding(horizontal = 0.dp))
            InfoCard {
                SocialRow(R.string.contact_linkedin, R.string.contact_linkedin_url, onOpenUrl)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SocialRow(R.string.contact_facebook, R.string.contact_facebook_url, onOpenUrl)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SocialRow(R.string.contact_instagram, R.string.contact_instagram_url, onOpenUrl)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SocialRow(R.string.contact_x, R.string.contact_x_url, onOpenUrl)
            }
            SectionLabel(stringResource(R.string.contact_legal), Modifier.padding(horizontal = 0.dp))
            InfoCard {
                LegalRow(R.string.legal_privacy) { onOpenPage("PoliticaPrivacidad") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LegalRow(R.string.legal_notice) { onOpenPage("AvisoLegal") }
            }
            Text(
                stringResource(R.string.contact_copyright),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
            )
        }
    }
}

@Composable
private fun OfficeMap(onOpenMap: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column {
            Box(Modifier.fillMaxWidth().height(MapHeight), contentAlignment = Alignment.Center) {
                MapPlaceholder(Modifier.fillMaxSize())
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.xxl))
            }
            Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.contact_office), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(Spacing.s))
                AppButton(stringResource(R.string.contact_open_map), onOpenMap, style = AppButtonStyle.Outlined, leadingIcon = Icons.Outlined.Map, compact = true)
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column { content() }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, text: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(RowIcon).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        }
        Spacer(Modifier.width(Spacing.m))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SocialRow(name: Int, url: Int, onOpenUrl: (String) -> Unit) {
    val link = stringResource(url)
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenUrl(link) }.padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Spacing.l))
        Spacer(Modifier.width(Spacing.m))
        Text(stringResource(name), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.m))
    }
}

@Composable
private fun LegalRow(title: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.m))
    }
}

@Preview(name = "Contacto", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ContactoPreview() {
    TurismoValenciaTheme { ContactoScreen({}, {}, {}, {}, {}, {}) }
}
