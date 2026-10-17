package com.cmarcillovargas.turismovalencia.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Iconos claros u oscuros en las barras del sistema mientras esta pantalla está en composición.
 * La app es de tema claro, así que por defecto son oscuros; las pantallas con fondo oscuro
 * (splash, fotos, velos) los piden claros. Al salir restaura el estado anterior.
 */
@Composable
fun SystemBarIcons(lightStatusBarIcons: Boolean, lightNavigationBarIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findActivity()?.window ?: return
    DisposableEffect(window, lightStatusBarIcons, lightNavigationBarIcons) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars
        // En la API de Android «appearance light» significa barras claras con iconos OSCUROS.
        controller.isAppearanceLightStatusBars = !lightStatusBarIcons
        controller.isAppearanceLightNavigationBars = !lightNavigationBarIcons
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}

/** La `Activity` que hay detrás de un `Context` (los de Compose suelen ser un `ContextWrapper`). */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
