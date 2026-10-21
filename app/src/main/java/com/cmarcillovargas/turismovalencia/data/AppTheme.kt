package com.cmarcillovargas.turismovalencia.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.cmarcillovargas.turismovalencia.data.model.ThemeMode

/**
 * Quien pone el modo claro u oscuro de la app en el sistema. La elección se guarda en Room (`SettingsRepository`), pero quien hace que
 * el tema de Android y los recursos de `values-night` cambien de verdad es AppCompat: ver [AppCompatThemeApplier].
 */
fun interface ThemeApplier {
    fun apply(mode: ThemeMode)
}

/** No toca el sistema: para las pruebas, que no deben cambiar el modo claro u oscuro del móvil en el que corren. */
object NoThemeApplier : ThemeApplier {
    override fun apply(mode: ThemeMode) = Unit
}

/** El valor de `AppCompatDelegate` de este modo (`MODE_NIGHT_FOLLOW_SYSTEM`, `MODE_NIGHT_NO` o `MODE_NIGHT_YES`). */
fun ThemeMode.nightMode(): Int = when (this) {
    ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
}

/**
 * Modo claro u oscuro por app de AppCompat (`AppCompatDelegate.setDefaultNightMode`): `MainActivity` es una `AppCompatActivity` con un
 * tema DayNight, así que AppCompat la recrea con los recursos del modo nuevo (y Compose lo lee en `isSystemInDarkTheme()`, que sigue la
 * configuración de la actividad). AppCompat no guarda el modo entre arranques: se guarda aparte, en [AppThemePrefs], para que
 * `TurismoApp` lo ponga antes de que exista ninguna actividad (Room todavía no está abierto).
 */
class AppCompatThemeApplier(private val context: Context) : ThemeApplier {
    private val main = Handler(Looper.getMainLooper())

    override fun apply(mode: ThemeMode) {
        AppThemePrefs.write(context, mode)
        // `setDefaultNightMode` puede recrear actividades: tiene que hacerse en el hilo principal.
        main.post { AppCompatDelegate.setDefaultNightMode(mode.nightMode()) }
    }
}

/**
 * La copia rápida del modo claro u oscuro, guardada aparte de Room (la fuente de verdad, que es la que enseña Perfil) para quien lo
 * necesita antes de que se pueda abrir la base de datos: `TurismoApp.onCreate`, que lo pone antes de crear la primera actividad para
 * que no haya un cuadro con el modo del móvil y una recreación después. La escribe [AppCompatThemeApplier] cada vez que cambia el modo
 * y al arrancar `AppContainer`, que la corrige con lo que haya en Room.
 */
object AppThemePrefs {
    private const val File = "theme"
    private const val Key = "mode"

    fun read(context: Context): ThemeMode = ThemeMode.fromName(context.getSharedPreferences(File, Context.MODE_PRIVATE).getString(Key, null))

    fun write(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(File, Context.MODE_PRIVATE).edit { putString(Key, mode.name) }
    }
}
