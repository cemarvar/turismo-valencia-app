package com.cmarcillovargas.turismovalencia.data.notify

import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

/**
 * Calcula cuándo toca el próximo aviso. Son cuentas con fechas y sin Android, para probarlas en la JVM.
 *
 * Los actos son en Valencia, así que la hora del aviso se calcula en la hora de Valencia y no en la del móvil: quien tenga el
 * móvil en otra zona horaria recibe el aviso a la misma hora real.
 */
object ReminderPlanner {
    val Valencia: ZoneId = ZoneId.of("Europe/Madrid")

    /**
     * El aviso se programa con una ventana y no a una hora exacta (las alarmas exactas piden un permiso especial en Android 14
     * y Android agrupa las demás para ahorrar batería). La ventana acaba a la hora ideal, la del acto menos la antelación, y
     * dura [WindowMinutes]: Android suele entregar la alarma al final de la ventana, así que el aviso llega justo con esa
     * antelación (y, como mucho, [WindowMinutes] minutos antes), nunca más tarde.
     */
    const val WindowMinutes = 10L

    /** Un año entero más un día: siempre hay una fecha del rango en ese tiempo. */
    private const val DaysToSearch = 367

    /** Cuándo empieza la ventana del aviso del día [day]. */
    fun windowStart(reminder: EventReminder, day: LocalDate): Instant =
        day.atTime(reminder.time).minusMinutes(reminder.leadMinutes + WindowMinutes).atZone(Valencia).toInstant()

    /**
     * Inicio de la ventana del primer aviso que empieza después de [after]. Con `after` = ahora es el próximo aviso; con
     * `after` = la ventana que acaba de saltar, el del día siguiente (nunca se repite el mismo).
     */
    fun next(reminder: EventReminder, after: Instant): Instant {
        var day = after.atZone(Valencia).toLocalDate()
        repeat(DaysToSearch) {
            if (reminder.occursOn(MonthDay.from(day))) {
                val start = windowStart(reminder, day)
                if (start.isAfter(after)) return start
            }
            day = day.plusDays(1)
        }
        error("«${reminder.itemId}» no tiene ninguna fecha en el próximo año")
    }
}
