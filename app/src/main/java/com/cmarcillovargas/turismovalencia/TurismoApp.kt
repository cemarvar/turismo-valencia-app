package com.cmarcillovargas.turismovalencia

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.AppThemePrefs
import com.cmarcillovargas.turismovalencia.data.nightMode
import com.cmarcillovargas.turismovalencia.data.work.AppWorkerFactory
import kotlin.concurrent.thread

class TurismoApp : Application(), Configuration.Provider {
    private val containerLazy = lazy { AppContainer(this) }
    val container: AppContainer get() = containerLazy.value

    /** El contenedor ya está creado (la pantalla de arranque del sistema espera a esto, ver `MainActivity`). */
    val containerReady: Boolean get() = containerLazy.isInitialized()

    /**
     * WorkManager no arranca con la app (el manifiesto quita su inicializador): se crea la primera vez que alguien lo usa, con esta
     * configuración, para que el trabajo nocturno se construya con el `AppContainer` (ver `data/work`).
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(AppWorkerFactory { container.refresher }).build()

    override fun onCreate() {
        super.onCreate()
        // El modo claro u oscuro que eligió el usuario, antes de que exista ninguna actividad: AppCompat lo aplica al crearla y no hay un
        // primer cuadro en el modo del móvil ni una recreación. Sale de una copia rápida (`AppThemePrefs`) porque Room todavía no está.
        AppCompatDelegate.setDefaultNightMode(AppThemePrefs.read(this).nightMode())
        // Abre la base de datos y lee lo guardado mientras arranca la actividad. Si la actividad llega antes,
        // `lazy` la hace esperar a que termine este hilo, así que nunca se crea dos veces.
        thread(name = "container-init") { container }
    }
}
