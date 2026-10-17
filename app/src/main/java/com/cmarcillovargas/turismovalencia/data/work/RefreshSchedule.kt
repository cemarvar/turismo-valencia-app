package com.cmarcillovargas.turismovalencia.data.work

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Cuándo se pone al día en segundo plano lo que viene del servidor (el catálogo de ofertas y las reservas): una vez cada noche, a
 * partir de las 3:00 de la hora del móvil. Es Kotlin puro (sin Android) para probar en la JVM las cuentas de la hora, incluidos los
 * cambios de hora de verano e invierno.
 */
object RefreshSchedule {
    /** Hora local (de 0 a 23) a partir de la cual se intenta el refresco cada noche. */
    const val NightHour = 3

    /** Cada cuánto se repite. */
    val Period: Duration = Duration.ofHours(24)

    /** Intentos seguidos que se dan a un refresco que falla (sin red, error del servidor) antes de esperar a la noche siguiente. */
    const val MaxAttempts = 3

    /** Lo que falta para la próxima vez que el reloj marque las [NightHour]:00 (si ya son esa hora en punto o más, la de mañana). */
    fun delayUntilNextNight(now: ZonedDateTime): Duration {
        val today = now.toLocalDate().atTime(NightHour, 0).atZone(now.zone)
        val next = if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(NightHour, 0).atZone(now.zone)
        return Duration.between(now, next)
    }

    /** Si hay que volver a intentarlo: el refresco falló y quedan intentos (`runAttemptCount` cuenta desde 0 los ya hechos). */
    fun shouldRetry(refreshed: Boolean, runAttemptCount: Int): Boolean = !refreshed && runAttemptCount + 1 < MaxAttempts
}
