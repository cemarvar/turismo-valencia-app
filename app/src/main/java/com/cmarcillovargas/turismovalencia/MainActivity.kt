package com.cmarcillovargas.turismovalencia

import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cmarcillovargas.turismovalencia.data.remote.CannedChatService
import com.cmarcillovargas.turismovalencia.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            (application as TurismoApp).container.resetToDemo()
        }
        if (debuggable && intent.hasExtra("offline")) {
            (application as TurismoApp).container.network.force(!intent.getBooleanExtra("offline", false))
        }
        if (debuggable && intent.getBooleanExtra("emptyFavorites", false)) {
            (application as TurismoApp).container.favorites.clear()
        }
        if (debuggable && intent.getBooleanExtra("chatFake", false)) {
            (application as TurismoApp).container.valbot.useChat(CannedChatService())
        }
        setContent { AppRoot(startRoute = startRoute) }
    }

    override fun onResume() {
        super.onResume()
        // El usuario pudo conceder o quitar el permiso de ubicación desde los Ajustes del sistema.
        (application as TurismoApp).container.location.refreshPermission()
    }
}
