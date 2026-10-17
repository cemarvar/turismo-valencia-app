package com.cmarcillovargas.turismovalencia.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

/** Pone al día lo que viene del servidor. Devuelve si quedó al día (falso: sin red o error del servidor, y se puede reintentar). */
fun interface BackgroundRefresher {
    suspend fun refresh(): Boolean
}

/**
 * El trabajo nocturno: pide el catálogo de ofertas y las reservas al servidor (lo mismo que al abrir la app) aunque la app esté
 * cerrada. Si falla, WorkManager lo reintenta con espera creciente hasta [RefreshSchedule.MaxAttempts] veces; después se espera a
 * la noche siguiente.
 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val refresher: BackgroundRefresher,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val refreshed = refresher.refresh()
        Log.i(Tag, "Refresco en segundo plano (intento ${runAttemptCount + 1}): " + if (refreshed) "al día" else "no se pudo")
        return if (RefreshSchedule.shouldRetry(refreshed, runAttemptCount)) Result.retry() else Result.success()
    }

    private companion object {
        const val Tag = "TurismoDb"
    }
}

/** Crea el [RefreshWorker] con lo que necesita (WorkManager solo sabe construir trabajos con constructor vacío). */
class AppWorkerFactory(private val refresher: () -> BackgroundRefresher) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == RefreshWorker::class.java.name) RefreshWorker(appContext, workerParameters, refresher()) else null
}
