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
 * `startRoute` salta la bienvenida y abre esa pantalla (solo se usa al depurar).
 */
// TODO(room): recordar que la bienvenida ya se hizo para no repetirla en cada arranque.
@Composable
fun AppRoot(startRoute: String? = null) {
    val container = (LocalContext.current.applicationContext as TurismoApp).container
    val textScale by container.settings.textScale.collectAsStateWithLifecycle()
    var welcomeDone by rememberSaveable { mutableStateOf(startRoute != null) }

    TurismoValenciaTheme(fontScaleFactor = textScale.factor) {
        if (welcomeDone) {
            MainApp(onLogout = { welcomeDone = false }, startRoute = startRoute)
        } else {
            BienvenidaFlow(onFinished = { welcomeDone = true })
        }
    }
}
