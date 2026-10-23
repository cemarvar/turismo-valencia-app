package com.cmarcillovargas.turismovalencia.ui.screens.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebItem

/** Página de la web con la política de privacidad (`assets/web/pages/PoliticaPrivacidad.json`). */
internal const val PrivacyPageId = "PoliticaPrivacidad"

/**
 * Lo que añade la app a la política de privacidad de la web: qué datos salen del móvil (reservas, cancelarlas y recuperarlas,
 * identificador, la copia de seguridad de Android, «Cerrar sesión», ValBot, mapa y rutas, actualización automática), adónde van y cómo ejercer los derechos. El texto de la web se importa y se regenera con
 * `tools/web-import`, así que esto vive aparte, en `strings.xml`, y se pinta debajo con los mismos bloques de texto legal. Si la app
 * empieza a mandar otro dato fuera del móvil, hay que añadirlo aquí. Los textos remiten a los puntos 1, 2, 4, 6 y 7 de la política de
 * la web (responsable, datos, base jurídica, destinatarios y derechos): si la web cambia esa numeración, hay que revisarlos.
 */
@Composable
internal fun appPrivacyBlocks(): List<WebBlock> = listOf(
    WebBlock.Items(
        title = "",
        filters = emptyList(),
        items = listOf(
            WebItem(title = stringResource(R.string.privacy_app_title), body = stringResource(R.string.privacy_app_intro)),
            WebItem(
                title = stringResource(R.string.privacy_app_bookings_title),
                body = stringResource(R.string.privacy_app_bookings_body),
                bullets = stringArrayResource(R.array.privacy_app_bookings_bullets).toList(),
            ),
            WebItem(body = stringResource(R.string.privacy_app_bookings_use)),
            WebItem(title = stringResource(R.string.privacy_app_cancel_title), body = stringResource(R.string.privacy_app_cancel_body)),
            WebItem(title = stringResource(R.string.privacy_app_device_title), body = stringResource(R.string.privacy_app_device_body)),
            WebItem(title = stringResource(R.string.privacy_app_backup_title), body = stringResource(R.string.privacy_app_backup_body)),
            WebItem(title = stringResource(R.string.privacy_app_logout_title), body = stringResource(R.string.privacy_app_logout_body)),
            WebItem(title = stringResource(R.string.privacy_app_valbot_title), body = stringResource(R.string.privacy_app_valbot_body)),
            WebItem(title = stringResource(R.string.privacy_app_map_title), body = stringResource(R.string.privacy_app_map_body)),
            WebItem(title = stringResource(R.string.privacy_app_background_title), body = stringResource(R.string.privacy_app_background_body)),
            WebItem(title = stringResource(R.string.privacy_app_none_title), body = stringResource(R.string.privacy_app_none_body)),
            WebItem(title = stringResource(R.string.privacy_app_rights_title), body = stringResource(R.string.privacy_app_rights_body)),
        ),
    ),
)
