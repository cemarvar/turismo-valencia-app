package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.notify.ReminderPlanner
import java.time.LocalTime
import java.time.ZonedDateTime

// Reglas de las reservas que no dependen de Android: Kotlin puro, para probarlo en la JVM.

/** Un correo con pinta de serlo (algo, arroba y un dominio con punto): lo mínimo para no mandar al servidor lo que seguro no vale. */
fun isPlausibleEmail(value: String): Boolean {
    val email = value.trim()
    return email.contains("@") && email.substringAfter("@").contains(".")
}

/**
 * La reserva se puede cancelar: no está ya cancelada ni completada y la actividad no ha empezado. Es la misma regla que aplica el
 * servidor (`app.cancel_reservation`), en la hora de Valencia: un día pasado no vale; el de hoy, hasta la hora del horario (uno sin hora,
 * «Cualquier hora», vale todo el día). La app solo la usa para enseñar o no el botón: quien decide es el servidor.
 */
fun Reservation.canCancel(now: ZonedDateTime = ZonedDateTime.now(ReminderPlanner.Valencia)): Boolean {
    if (status == ReservationStatus.Cancelled || status == ReservationStatus.Completed) return false
    val today = now.withZoneSameInstant(ReminderPlanner.Valencia).toLocalDate()
    if (date.isBefore(today)) return false
    if (date == today) {
        val start = runCatching { LocalTime.parse(time) }.getOrNull()
        if (start != null && !start.isAfter(now.withZoneSameInstant(ReminderPlanner.Valencia).toLocalTime())) return false
    }
    return true
}
