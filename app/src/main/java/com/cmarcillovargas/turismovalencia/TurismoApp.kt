package com.cmarcillovargas.turismovalencia

import android.app.Application
import com.cmarcillovargas.turismovalencia.data.AppContainer
import kotlin.concurrent.thread

class TurismoApp : Application() {
    private val containerLazy = lazy { AppContainer(this) }
    val container: AppContainer get() = containerLazy.value

    /** El contenedor ya está creado (la pantalla de arranque del sistema espera a esto, ver `MainActivity`). */
    val containerReady: Boolean get() = containerLazy.isInitialized()

    override fun onCreate() {
        super.onCreate()
        // Abre la base de datos y lee lo guardado mientras arranca la actividad. Si la actividad llega antes,
        // `lazy` la hace esperar a que termine este hilo, así que nunca se crea dos veces.
        thread(name = "container-init") { container }
    }
}
