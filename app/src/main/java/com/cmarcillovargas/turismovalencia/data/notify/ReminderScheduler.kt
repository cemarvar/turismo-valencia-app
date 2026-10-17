package com.cmarcillovargas.turismovalencia.data.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.time.Instant

/**
 * Programa los avisos con `AlarmManager`. Cada evento tiene una sola alarma pendiente, la del próximo aviso: al saltar,
 * [ReminderReceiver] muestra la notificación y programa la siguiente (la mascletà es todos los días, del 1 al 19 de marzo).
 * Las alarmas se pierden al reiniciar el móvil: [BootReceiver] las vuelve a programar.
 *
 * La alarma es una ventana de [ReminderPlanner.WindowMinutes] minutos (`setWindow`) y no una hora exacta: las exactas
 * piden el permiso «Alarmas y recordatorios», que Android 14 deja desactivado por defecto. Con el móvil en reposo profundo
 * el sistema puede retrasarla algo más.
 */
class ReminderScheduler(context: Context) {
    private val context = context.applicationContext
    private val alarms = this.context.getSystemService(AlarmManager::class.java)

    /**
     * Programa el primer aviso de [reminder] que empieza después de [after] (por defecto, ahora) y devuelve cuándo empieza su
     * ventana. Si ya había una alarma de ese evento, la sustituye.
     */
    fun schedule(reminder: EventReminder, after: Instant = Instant.now()): Instant {
        val start = ReminderPlanner.next(reminder, after)
        val operation = pending(reminder.itemId, start, PendingIntent.FLAG_UPDATE_CURRENT) ?: error("No se pudo crear la alarma de «${reminder.itemId}»")
        alarms.setWindow(AlarmManager.RTC_WAKEUP, start.toEpochMilli(), ReminderPlanner.WindowMinutes * MillisPerMinute, operation)
        return start
    }

    fun cancel(itemId: String) {
        pending(itemId, windowStart = null, PendingIntent.FLAG_NO_CREATE)?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    /** ¿Hay una alarma pendiente de ese evento? */
    fun isScheduled(itemId: String): Boolean = pending(itemId, windowStart = null, PendingIntent.FLAG_NO_CREATE) != null

    /** Deja programados exactamente los avisos de [wanted] (ids de evento) y cancela los demás. */
    fun sync(wanted: Set<String>) {
        EventReminders.all.forEach { reminder ->
            if (reminder.itemId in wanted) schedule(reminder) else cancel(reminder.itemId)
        }
    }

    // La dirección `turismovalencia://reminder/<id>` no se abre nunca: solo hace distintas las alarmas de cada evento
    // (Android compara la acción y los datos del Intent, no sus extras).
    private fun pending(itemId: String, windowStart: Instant?, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ActionRemind)
            .setData("turismovalencia://reminder/$itemId".toUri())
            .putExtra(ReminderReceiver.ExtraItem, itemId)
        if (windowStart != null) intent.putExtra(ReminderReceiver.ExtraWindowStart, windowStart.toEpochMilli())
        return PendingIntent.getBroadcast(context, itemId.hashCode(), intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val MillisPerMinute = 60_000L
    }
}
