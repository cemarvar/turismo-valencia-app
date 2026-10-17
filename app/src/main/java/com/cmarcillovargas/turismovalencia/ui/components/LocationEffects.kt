package com.cmarcillovargas.turismovalencia.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Devuelve una función que abre el diálogo del sistema para pedir la ubicación (`precise = false` pide solo la aproximada).
 * [onResult] recibe si ha quedado concedida. Si el usuario ya la rechazó dos veces, el sistema no vuelve a preguntar y el
 * resultado llega enseguida como denegado; en ese caso (y tras un «no» que ya no se puede repetir) se llama además a
 * [onBlocked]: el permiso solo se puede dar desde los ajustes del móvil (ver [OpenSettingsDialog]).
 */
@Composable
fun rememberLocationPermissionRequest(
    onBlocked: () -> Unit = {},
    onResult: (granted: Boolean) -> Unit,
): (precise: Boolean) -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants.values.any { it }
        currentOnResult(granted)
        // Tras un «no», `shouldShow…` es verdadero mientras el sistema siga dispuesto a preguntar otra vez.
        val activity = context.findActivity()
        if (!granted && activity != null && grants.keys.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }) {
            currentOnBlocked()
        }
    }
    return remember(launcher) {
        { precise ->
            val permissions = if (precise) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            launcher.launch(permissions)
        }
    }
}

/** Llama a [onStart] mientras la pantalla se ve (al abrirla y al volver a ella) y a [onStop] cuando deja de verse. */
@Composable
fun OnScreenVisible(onStart: () -> Unit, onStop: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> currentOnStart()
                Lifecycle.Event.ON_STOP -> currentOnStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        // Si la pantalla ya está visible al componerse no llegará un ON_START nuevo.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) currentOnStart()
        onDispose {
            lifecycle.removeObserver(observer)
            currentOnStop()
        }
    }
}
