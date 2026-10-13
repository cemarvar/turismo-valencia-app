package com.cmarcillovargas.turismovalencia.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Devuelve una función que abre el diálogo del sistema para pedir la ubicación (`precise = false` pide solo la aproximada).
 * [onResult] recibe si ha quedado concedida. Si el usuario ya la rechazó dos veces, el sistema no vuelve a preguntar y
 * el resultado llega enseguida como denegado.
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): (precise: Boolean) -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        currentOnResult(grants.values.any { it })
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
