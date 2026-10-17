package com.cmarcillovargas.turismovalencia.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cmarcillovargas.turismovalencia.data.notify.NotificationAccess

/** Cómo acaba pedir las notificaciones. */
enum class NotificationAccessResult {
    /** Las notificaciones están permitidas. */
    Granted,

    /** El usuario ha dicho que no; el sistema todavía dejará preguntar otra vez. */
    Denied,

    /** El sistema ya no pregunta (o están apagadas en Ajustes): solo se pueden activar desde los ajustes del móvil. */
    NeedsSettings,
}

/**
 * Devuelve una función que se asegura de que la app puede notificar: si ya puede, lo dice enseguida; en Android 13 o más
 * abre el diálogo del sistema (si todavía hace falta el permiso) y, en cualquier otro caso, dice que hay que ir a Ajustes.
 * Hay que pedirlo cuando el usuario lo activa (un aviso, el interruptor de Notificaciones) y no al abrir la app.
 */
@SuppressLint("InlinedApi") // `POST_NOTIFICATIONS` es una constante de texto; solo se pide a partir de Android 13.
@Composable
fun rememberNotificationAccessRequest(onResult: (NotificationAccessResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context.findActivity()
        currentOnResult(
            when {
                granted -> if (NotificationAccess.isEnabled(context)) NotificationAccessResult.Granted else NotificationAccessResult.NeedsSettings
                // Tras decir que no, `shouldShow…` es verdadero mientras el sistema siga dispuesto a preguntar otra vez.
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS) ->
                    NotificationAccessResult.Denied

                else -> NotificationAccessResult.NeedsSettings
            },
        )
    }
    return remember(launcher, context) {
        {
            when {
                NotificationAccess.isEnabled(context) -> currentOnResult(NotificationAccessResult.Granted)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context) ->
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)

                else -> currentOnResult(NotificationAccessResult.NeedsSettings)
            }
        }
    }
}

@SuppressLint("InlinedApi") // solo se llama en Android 13 o más
private fun hasNotificationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
