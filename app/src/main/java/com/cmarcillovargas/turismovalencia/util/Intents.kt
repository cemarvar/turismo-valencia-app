package com.cmarcillovargas.turismovalencia.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Direcciones de la web, para compartir y «Ver en la web». */
const val WebBaseUrl = "https://turismovalenciaa.netlify.app"

fun webUrlOf(pageId: String): String = "$WebBaseUrl/$pageId"

/** Abre una dirección web en el navegador. */
fun Context.openUrl(url: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

/** Abre el marcador con el número, sin llamar. */
fun Context.dial(phone: String) = launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))

/** Abre el correo con el destinatario ya puesto. */
fun Context.email(address: String) = launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))

/** Abre una búsqueda en el mapa del móvil. */
fun Context.openMapSearch(query: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))

/** Abre los ajustes de la app en el sistema (permisos, almacenamiento…). */
fun Context.openAppSettings() =
    launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

/**
 * Abre los ajustes de notificaciones de la app (si están desactivadas, el único sitio donde se pueden volver a activar).
 * Esa pantalla solo existe desde Android 8: en Android 7 se abren los ajustes generales de la app, que también las tienen.
 */
fun Context.openNotificationSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    } else {
        openAppSettings()
    }
}

/** Comparte un texto con el selector del sistema. */
fun Context.shareText(subject: String, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    launch(Intent.createChooser(intent, chooserTitle))
}

private fun Context.launch(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin app que lo gestione (emulador sin navegador, por ejemplo): no pasa nada.
    }
}
