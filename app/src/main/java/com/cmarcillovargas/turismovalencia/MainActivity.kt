package com.cmarcillovargas.turismovalencia

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cmarcillovargas.turismovalencia.data.appLanguage
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.remote.CannedChatService
import com.cmarcillovargas.turismovalencia.data.remote.DownChatService
import com.cmarcillovargas.turismovalencia.ui.AppRoot

// AppCompatActivity (y un tema AppCompat) porque el idioma por app de AppCompat lo exige en Android 12 o menos; ver `data/AppLanguage.kt`.
class MainActivity : AppCompatActivity() {
    /** Ficha que hay que abrir porque el usuario tocó un aviso; `MainApp` la abre y la da por atendida. */
    private var openItem by mutableStateOf<String?>(null)

    /** Idioma con el que se crearon las pantallas de esta actividad (para saber, al recrearla, si ha cambiado). */
    private lateinit var language: LanguageCode

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de arranque del sistema (logotipo sobre el color de marca): sustituye a la ventana en blanco de antes y se
        // queda hasta que el contenedor de datos esté listo.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as TurismoApp
        splash.setKeepOnScreenCondition { !app.containerReady }
        // El idioma lo deciden los recursos (el que eligió el usuario en la app o en los ajustes del móvil o, si no, el del móvil si es
        // uno de los cuatro). Al cambiarlo, Android recrea la actividad pero conserva sus ViewModel, con los textos en el idioma
        // anterior: se descartan para que cada pantalla vuelva a construirse con el nuevo.
        language = appLanguage()
        if (savedInstanceState != null && savedInstanceState.getString(StateLanguage) != language.code) viewModelStore.clear()
        app.container.applyLanguage(language)
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
        // `--ez chatFake true` hace que ValBot conteste como Gemini sin llamar al servidor (`--ez chatDown true`, que el asistente en línea no
        // responda y salgan las reglas de la app) y `--es lang ES|VA|EN|FR` cambia el idioma.
        // Los que tocan los datos (`resetData`, `emptyFavorites`) y el idioma solo se aplican al abrir la app, no al recrear la
        // actividad (por ejemplo al cambiar de idioma desde la propia app, que la recrea con el mismo intent).
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val freshLaunch = savedInstanceState == null
        val startRoute = if (debuggable) intent.getStringExtra("route") else null
        if (debuggable && freshLaunch && intent.getBooleanExtra("resetData", false)) {
            app.container.resetToDemo()
        }
        if (debuggable && freshLaunch) {
            LanguageCode.fromCode(intent.getStringExtra("lang"))?.takeIf { it != language }?.let(app.container.settings::setLanguage)
        }
        if (debuggable && intent.hasExtra("offline")) {
            app.container.network.force(!intent.getBooleanExtra("offline", false))
        }
        if (debuggable && freshLaunch && intent.getBooleanExtra("emptyFavorites", false)) {
            app.container.favorites.clear()
        }
        if (debuggable && intent.getBooleanExtra("chatFake", false)) {
            app.container.valbot.useChat(CannedChatService())
        }
        if (debuggable && intent.getBooleanExtra("chatDown", false)) {
            app.container.valbot.useChat(DownChatService())
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(StateLanguage, language.code)
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

        private const val StateLanguage = "language"
    }
}
