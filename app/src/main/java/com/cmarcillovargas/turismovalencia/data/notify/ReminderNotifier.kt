package com.cmarcillovargas.turismovalencia.data.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cmarcillovargas.turismovalencia.MainActivity
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.AppLanguagePrefs
import com.cmarcillovargas.turismovalencia.data.AppStrings

/** Muestra la notificación de un aviso de evento; al tocarla se abre la ficha del evento. */
class ReminderNotifier(context: Context) {
    private val context = context.applicationContext
    private val manager = NotificationManagerCompat.from(this.context)

    /** Los textos van en el último idioma de la app, que se lee aparte de Room: la alarma despierta el proceso sin abrir la base de datos. */
    private val strings = AppStrings(this.context, AppLanguagePrefs.read(this.context))

    /**
     * Muestra el aviso y devuelve `true`. Si el sistema no deja notificar (falta el permiso o están apagadas) no hace nada
     * y no crea el canal, porque crearlo antes de tiempo hace que Android 13 pida el permiso al abrir la app.
     */
    // `POST_NOTIFICATIONS` es una constante de texto (existe desde Android 13); `ContextCompat` la entiende también en versiones anteriores.
    @SuppressLint("InlinedApi")
    fun show(reminder: EventReminder): Boolean {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed || !manager.areNotificationsEnabled()) return false
        ensureChannel()
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(strings.get(reminder.title))
            .setContentText(strings.get(reminder.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openItem(reminder))
            .build()
        manager.notify(Tag, notificationId(reminder), notification)
        return true
    }

    /** Quita el aviso de la bandeja (lo usan las pruebas). */
    fun dismiss(reminder: EventReminder) = manager.cancel(Tag, notificationId(reminder))

    // Se vuelve a crear con cada aviso: si el idioma cambió, el nombre y la descripción del canal se actualizan.
    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(strings.get(R.string.reminder_channel_name))
            .setDescription(strings.get(R.string.reminder_channel_description))
            .build()
        manager.createNotificationChannel(channel)
    }

    private fun openItem(reminder: EventReminder): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.ExtraOpenItem, reminder.itemId)
        return PendingIntent.getActivity(context, notificationId(reminder), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun notificationId(reminder: EventReminder) = reminder.itemId.hashCode()

    companion object {
        const val ChannelId = "event_reminders"
        const val Tag = "event-reminder"
    }
}
