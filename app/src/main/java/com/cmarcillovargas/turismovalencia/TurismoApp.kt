package com.cmarcillovargas.turismovalencia

import android.app.Application
import com.cmarcillovargas.turismovalencia.data.AppContainer
import kotlin.concurrent.thread

class TurismoApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Abre la base de datos y lee lo guardado mientras arranca la actividad. Si la actividad llega antes,
        // `lazy` la hace esperar a que termine este hilo, así que nunca se crea dos veces.
        thread(name = "container-init") { container }
    }
}
