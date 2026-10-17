package com.cmarcillovargas.turismovalencia.data.notify

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ¿Puede la app mostrar notificaciones? En Android 13 o más hace falta el permiso `POST_NOTIFICATIONS`; en cualquier versión
 * el usuario puede apagarlas (la app entera o el canal de avisos) en los Ajustes del sistema. Se vuelve a mirar al volver a la
 * app ([refresh]) porque pudo cambiarse fuera de ella.
 *
 * [read] se separa para poder sustituirlo en las pruebas.
 */
class NotificationAccess(private val read: () -> Boolean) {
    private val state = MutableStateFlow(read())

    /** Las notificaciones de la app pueden mostrarse ahora mismo. */
    val enabled: StateFlow<Boolean> = state.asStateFlow()

    fun refresh() {
        state.value = read()
    }

    companion object {
        fun of(context: Context): NotificationAccess {
            val app = context.applicationContext
            return NotificationAccess { isEnabled(app) }
        }

        /**
         * Permiso concedido (donde se pide) y ni la app ni el canal de avisos están apagados. El canal no existe hasta que se
         * muestra el primer aviso: crearlo antes haría que Android 13 pidiera el permiso nada más abrir la app.
         */
        fun isEnabled(context: Context): Boolean {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return false
            val channel = manager.getNotificationChannelCompat(ReminderNotifier.ChannelId)
            return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
        }
    }
}
