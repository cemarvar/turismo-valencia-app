package com.cmarcillovargas.turismovalencia

import android.app.Application
import com.cmarcillovargas.turismovalencia.data.AppContainer

class TurismoApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
