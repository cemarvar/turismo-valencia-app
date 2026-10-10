package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.ui.theme.CardShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

/** Resumen del contacto de la oficina de turismo (pie de la portada y de cada página) con acceso a la pantalla completa. */
@Composable
fun ContactSummaryCard(onOpenContact: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().clip(CardShape).background(MaterialTheme.appColors.brandSoft).padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Eyebrow(stringResource(R.string.contact_title))
        Text(stringResource(R.string.contact_cta_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.contact_brand_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton(
            text = stringResource(R.string.contact_cta_action),
            onClick = onOpenContact,
            modifier = Modifier.padding(top = Spacing.xs),
            style = AppButtonStyle.Tonal,
            leadingIcon = Icons.Outlined.SupportAgent,
            compact = true,
        )
    }
}
