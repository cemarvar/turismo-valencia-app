package com.cmarcillovargas.turismovalencia.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cmarcillovargas.turismovalencia.TurismoApp
import kotlin.concurrent.thread

/** Al reiniciar el móvil Android borra las alarmas: las vuelve a programar según lo que el usuario tiene activado. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as TurismoApp
        // Abrir la base de datos tarda: se hace fuera del hilo principal y el receiver sigue vivo hasta terminar.
        val pending = goAsync()
        thread(name = "reminders-boot") {
            try {
                app.container.reminders.syncNow()
            } finally {
                pending.finish()
            }
        }
    }
}
