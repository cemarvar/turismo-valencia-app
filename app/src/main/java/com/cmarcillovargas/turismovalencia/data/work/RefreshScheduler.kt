package com.cmarcillovargas.turismovalencia.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Lo que piden los ajustes de Descargas: [enabled] es «Actualizar automáticamente» y [unmeteredOnly] es «Descargar solo con Wi-Fi»
 * (solo vale una red sin tarifa por datos; si no, basta cualquier conexión).
 */
data class RefreshPlan(val enabled: Boolean, val unmeteredOnly: Boolean)

/** Quién programa o cancela el refresco nocturno (WorkManager en la app; nada en las pruebas que no lo necesitan). */
fun interface RefreshScheduler {
    fun apply(plan: RefreshPlan)
}

object NoRefreshScheduler : RefreshScheduler {
    override fun apply(plan: RefreshPlan) = Unit
}

/**
 * El refresco nocturno con WorkManager: un único trabajo periódico (`UniqueName`) cada 24 h que arranca a partir de las 3:00 de la
 * hora del móvil. Con «Solo Wi-Fi» pide una red sin tarifa (`UNMETERED`), si no, cualquier conexión; en los dos casos, batería no
 * baja. `UPDATE` cambia las condiciones de un trabajo ya programado sin reiniciar su calendario, así que alternar un interruptor no
 * retrasa el siguiente refresco. Si el trabajo falla, WorkManager lo reintenta con espera creciente (15 min, 30 min…).
 */
class WorkRefreshScheduler(
    private val context: Context,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : RefreshScheduler {
    override fun apply(plan: RefreshPlan) {
        val work = WorkManager.getInstance(context)
        if (!plan.enabled) {
            work.cancelUniqueWork(UniqueName)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (plan.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequest.Builder(RefreshWorker::class.java, RefreshSchedule.Period.toHours(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(RefreshSchedule.delayUntilNextNight(now()).toMinutes(), TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BackoffMinutes, TimeUnit.MINUTES)
            .build()
        work.enqueueUniquePeriodicWork(UniqueName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    companion object {
        const val UniqueName = "refresco-nocturno"
        private const val BackoffMinutes = 15L
    }
}
