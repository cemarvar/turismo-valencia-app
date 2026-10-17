package com.cmarcillovargas.turismovalencia

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cmarcillovargas.turismovalencia.data.remote.CannedChatService
import com.cmarcillovargas.turismovalencia.ui.AppRoot

class MainActivity : ComponentActivity() {
    /** Ficha que hay que abrir porque el usuario tocó un aviso; `MainApp` la abre y la da por atendida. */
    private var openItem by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de arranque del sistema (logotipo sobre el color de marca): sustituye a la ventana en blanco de antes y se
        // queda hasta que el contenedor de datos esté listo.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as TurismoApp
        splash.setKeepOnScreenCondition { !app.containerReady }
        // Tema solo claro: iconos oscuros en las barras del sistema aunque el móvil esté en modo oscuro.
        // Las pantallas con fondo oscuro los cambian con SystemBarIcons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Solo en builds de depuración: `adb shell am start -n …/.MainActivity --es route detalle/mercado-central`
        // abre una pantalla concreta, `--ez offline true` simula que no hay conexión y
        // `--ez emptyFavorites true` vacía los favoritos, `--ez resetData true` devuelve la base de datos a los datos de
        // demostración (hay que pasarlo primero para que el resto de extras se apliquen sobre ellos) y
        // `--ez chatFake true` hace que ValBot conteste como Gemini sin llamar al servidor.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val startRoute = if (debuggable) intent.getStringExtra("route") else null
        if (debuggable && intent.getBooleanExtra("resetData", false)) {
            app.container.resetToDemo()
        }
        if (debuggable && intent.hasExtra("offline")) {
            app.container.network.force(!intent.getBooleanExtra("offline", false))
        }
        if (debuggable && intent.getBooleanExtra("emptyFavorites", false)) {
            app.container.favorites.clear()
        }
        if (debuggable && intent.getBooleanExtra("chatFake", false)) {
            app.container.valbot.useChat(CannedChatService())
        }
        // Al recrear la actividad (girar el móvil, proceso cerrado) el grafo de navegación se restaura solo: la ficha del aviso
        // solo se abre en un arranque nuevo.
        if (savedInstanceState == null) openItem = intent.getStringExtra(ExtraOpenItem)
        setContent {
            AppRoot(
                startRoute = startRoute,
                openItem = openItem,
                onOpenItemHandled = {
                    openItem = null
                    intent.removeExtra(ExtraOpenItem)
                },
            )
        }
    }

    /** La app ya estaba abierta y el usuario tocó un aviso. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(ExtraOpenItem)?.let { openItem = it }
    }

    override fun onResume() {
        super.onResume()
        // El usuario pudo conceder o quitar el permiso de ubicación y las notificaciones desde los Ajustes del sistema.
        val app = application as TurismoApp
        if (app.containerReady) {
            app.container.location.refreshPermission()
            app.container.notificationAccess.refresh()
        }
    }

    companion object {
        /** Extra con el id de la ficha que abre un aviso (lo pone `ReminderNotifier`). */
        const val ExtraOpenItem = "open_item"
    }
}
