package com.cmarcillovargas.turismovalencia.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * Salta cuando llega la ventana de un aviso: muestra la notificación y programa la del día siguiente. No lee la base de
 * datos: si el usuario apaga el aviso, la alarma se cancela en ese momento (`RemindersRepository`).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ActionRemind) return
        val reminder = intent.getStringExtra(ExtraItem)?.let(EventReminders::of) ?: return
        val app = context.applicationContext
        ReminderNotifier(app).show(reminder)
        // El siguiente aviso va siempre después de este: así no se repite si el sistema lo entrega con la ventana ya empezada.
        val fired = Instant.ofEpochMilli(intent.getLongExtra(ExtraWindowStart, 0L))
        ReminderScheduler(app).schedule(reminder, after = maxOf(Instant.now(), fired))
    }

    companion object {
        const val ActionRemind = "com.cmarcillovargas.turismovalencia.action.REMIND"
        const val ExtraItem = "item"
        const val ExtraWindowStart = "window_start"
    }
}
