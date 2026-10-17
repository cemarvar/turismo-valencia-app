package com.cmarcillovargas.turismovalencia

import android.app.Application
import androidx.work.Configuration
import com.cmarcillovargas.turismovalencia.data.AppContainer
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
        // Abre la base de datos y lee lo guardado mientras arranca la actividad. Si la actividad llega antes,
        // `lazy` la hace esperar a que termine este hilo, así que nunca se crea dos veces.
        thread(name = "container-init") { container }
    }
}
