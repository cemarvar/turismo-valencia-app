package com.cmarcillovargas.turismovalencia.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmarcillovargas.turismovalencia.TurismoApp
import com.cmarcillovargas.turismovalencia.navigation.MainApp
import com.cmarcillovargas.turismovalencia.ui.screens.bienvenida.BienvenidaFlow
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme

/**
 * Raíz de la app: el recorrido de bienvenida y, al terminarlo, la app principal.
 * `startRoute` salta la bienvenida y abre esa pantalla (solo se usa al depurar). `openItem` es la ficha que abre un aviso
 * tocado; se abre cuando la app principal está en pantalla y entonces se llama a `onOpenItemHandled`.
 */
@Composable
fun AppRoot(startRoute: String? = null, openItem: String? = null, onOpenItemHandled: () -> Unit = {}) {
    val container = (LocalContext.current.applicationContext as TurismoApp).container
    val textScale by container.settings.textScale.collectAsStateWithLifecycle()
    // La bienvenida ya se hizo (guardado en la base de datos) o se salta al depurar. Se decide al abrir la app; después
    // lo cambian «Cerrar sesión» y el final de la bienvenida, que además lo guardan.
    var welcomeDone by rememberSaveable { mutableStateOf(startRoute != null || container.settings.onboardingDone.value) }

    TurismoValenciaTheme(fontScaleFactor = textScale.factor) {
        if (welcomeDone) {
            MainApp(
                onLogout = {
                    welcomeDone = false
                    container.settings.setOnboardingDone(false)
                },
                startRoute = startRoute,
                openItem = openItem,
                onOpenItemHandled = onOpenItemHandled,
            )
        } else {
            BienvenidaFlow(
                onFinished = {
                    welcomeDone = true
                    container.settings.setOnboardingDone(true)
                },
            )
        }
    }
}
