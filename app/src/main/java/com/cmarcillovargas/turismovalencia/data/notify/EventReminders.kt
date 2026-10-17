package com.cmarcillovargas.turismovalencia.data.notify

import androidx.annotation.StringRes
import com.cmarcillovargas.turismovalencia.R
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay

/**
 * Aviso de un acto que se repite cada año, todos los días entre dos fechas, a la misma hora (la mascletà de las Fallas:
 * del 1 al 19 de marzo a las 14:00). Un [EventReminder] solo describe el calendario; que el usuario lo quiera lo guarda
 * la tabla `reminders` y de programarlo se encarga [ReminderScheduler].
 */
data class EventReminder(
    /** Ficha del evento: el aviso lleva a ella y es la clave con la que se guarda. */
    val itemId: String,
    val firstDay: MonthDay,
    val lastDay: MonthDay,
    /** Hora del acto, en la hora de Valencia. */
    val time: LocalTime,
    /** Minutos de antelación con que se avisa. */
    val leadMinutes: Int,
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
) {
    init {
        require(!lastDay.isBefore(firstDay)) { "«$itemId»: el último día va antes que el primero" }
    }

    fun occursOn(day: MonthDay): Boolean = !day.isBefore(firstDay) && !day.isAfter(lastDay)
}

/** Los avisos que ofrece la app. */
object EventReminders {
    /** La mascletà de las Fallas: cada día del 1 al 19 de marzo a las 14:00 en la Plaza del Ayuntamiento. */
    val Mascleta = EventReminder(
        itemId = "fallas",
        firstDay = MonthDay.of(Month.MARCH, 1),
        lastDay = MonthDay.of(Month.MARCH, 19),
        time = LocalTime.of(14, 0),
        leadMinutes = 30,
        title = R.string.reminder_mascleta_title,
        text = R.string.reminder_mascleta_text,
    )

    val all: List<EventReminder> = listOf(Mascleta)

    /** El aviso de esa ficha, si la ficha tiene alguno. */
    fun of(itemId: String): EventReminder? = all.firstOrNull { it.itemId == itemId }
}
