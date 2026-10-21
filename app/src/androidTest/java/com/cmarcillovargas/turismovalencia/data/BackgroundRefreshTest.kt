package com.cmarcillovargas.turismovalencia.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.remote.NoOffersService
import com.cmarcillovargas.turismovalencia.data.remote.NoReservationsService
import com.cmarcillovargas.turismovalencia.data.work.AppWorkerFactory
import com.cmarcillovargas.turismovalencia.data.work.BackgroundRefresher
import com.cmarcillovargas.turismovalencia.data.work.RefreshPlan
import com.cmarcillovargas.turismovalencia.data.work.RefreshSchedule
import com.cmarcillovargas.turismovalencia.data.work.RefreshScheduler
import com.cmarcillovargas.turismovalencia.data.work.RefreshWorker
import com.cmarcillovargas.turismovalencia.data.work.WorkRefreshScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * El refresco nocturno en segundo plano: lo que se programa en WorkManager según «Actualizar automáticamente» y «Descargar solo
 * con Wi-Fi», lo que hace el trabajo cuando le toca y cómo el contenedor de la app reprograma al cambiar los ajustes. WorkManager
 * es el de pruebas: no ejecuta nada por su cuenta, así que se mira lo que quedó programado.
 */
@RunWith(AndroidJUnit4::class)
class BackgroundRefreshTest {
    private lateinit var context: Context

    // Las 14:00 en Madrid: faltan 13 horas para las 3:00.
    private val now = ZonedDateTime.of(2026, 10, 17, 14, 0, 0, 0, ZoneId.of("Europe/Madrid"))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
    }

    private fun scheduler() = WorkRefreshScheduler(context) { now }

    private fun infos(): List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkRefreshScheduler.UniqueName).get()

    private fun pending() = infos().filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }

    @Test
    fun conLosAjustesDeSiempreSeProgramaUnTrabajoNocturnoSoloConWifi() {
        scheduler().apply(RefreshPlan(enabled = true, unmeteredOnly = true))

        val work = pending().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.UNMETERED, work.constraints.requiredNetworkType)
        assertTrue(work.constraints.requiresBatteryNotLow())
        assertEquals(RefreshSchedule.Period.toMillis(), work.periodicityInfo!!.repeatIntervalMillis)
        assertEquals(TimeUnit.HOURS.toMillis(13), work.initialDelayMillis)
    }

    @Test
    fun sinSoloWifiBastaCualquierConexionYSigueHabiendoUnSoloTrabajo() {
        val scheduler = scheduler()
        scheduler.apply(RefreshPlan(enabled = true, unmeteredOnly = true))
        scheduler.apply(RefreshPlan(enabled = true, unmeteredOnly = false))

        assertEquals(NetworkType.CONNECTED, pending().single().constraints.requiredNetworkType)

        scheduler.apply(RefreshPlan(enabled = true, unmeteredOnly = true))
        assertEquals(NetworkType.UNMETERED, pending().single().constraints.requiredNetworkType)
    }

    @Test
    fun apagarLaActualizacionAutomaticaCancelaElTrabajoYVolverAEncenderlaLoProgramaDeNuevo() {
        val scheduler = scheduler()
        scheduler.apply(RefreshPlan(enabled = true, unmeteredOnly = true))
        scheduler.apply(RefreshPlan(enabled = false, unmeteredOnly = true))
        assertTrue("sigue pendiente: ${infos()}", pending().isEmpty())

        scheduler.apply(RefreshPlan(enabled = true, unmeteredOnly = true))
        assertEquals(WorkInfo.State.ENQUEUED, pending().single().state)
    }

    @Test
    fun elTrabajoRefrescaYSeReintentaSiFallaHastaAgotarLosIntentos() {
        var calls = 0

        fun run(refreshed: Boolean, attempt: Int): ListenableWorker.Result = runBlocking {
            TestListenableWorkerBuilder<RefreshWorker>(context)
                .setWorkerFactory(AppWorkerFactory { BackgroundRefresher { calls++; refreshed } })
                .setRunAttemptCount(attempt)
                .build()
                .doWork()
        }

        assertEquals(ListenableWorker.Result.success(), run(refreshed = true, attempt = 0))
        assertEquals(ListenableWorker.Result.retry(), run(refreshed = false, attempt = 0))
        assertEquals(ListenableWorker.Result.retry(), run(refreshed = false, attempt = RefreshSchedule.MaxAttempts - 2))
        // Sin intentos, se rinde hasta la noche siguiente: el trabajo periódico sigue programado.
        assertEquals(ListenableWorker.Result.success(), run(refreshed = false, attempt = RefreshSchedule.MaxAttempts - 1))
        assertEquals(4, calls)
    }

    @Test
    fun elContenedorReprogramaElTrabajoCadaVezQueCambiaUnAjusteDeDescargas() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val memory = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val plans = CopyOnWriteArrayList<RefreshPlan>()
        try {
            val container = AppContainer(app, memory, NoOffersService, NoReservationsService, RefreshScheduler { plans += it }, NoLanguageApplier, NoThemeApplier)
            // La demostración trae los dos interruptores activados.
            awaitPlans(plans, RefreshPlan(enabled = true, unmeteredOnly = true))

            container.downloads.setWifiOnly(false)
            awaitPlans(plans, RefreshPlan(true, true), RefreshPlan(true, false))

            container.downloads.setAutoUpdate(false)
            awaitPlans(plans, RefreshPlan(true, true), RefreshPlan(true, false), RefreshPlan(false, false))

            // Repetir un ajuste que ya estaba así no reprograma nada.
            container.downloads.setAutoUpdate(false)
            container.downloads.setAutoUpdate(true)
            awaitPlans(plans, RefreshPlan(true, true), RefreshPlan(true, false), RefreshPlan(false, false), RefreshPlan(true, false))
        } finally {
            memory.close()
        }
    }

    /** Las escrituras de ajustes son asíncronas: espera a que el contenedor haya programado exactamente estos planes, en orden. */
    private fun awaitPlans(actual: List<RefreshPlan>, vararg expected: RefreshPlan) {
        val end = System.currentTimeMillis() + 5_000
        while (actual.toList() != expected.toList() && System.currentTimeMillis() < end) Thread.sleep(25)
        assertEquals(expected.toList(), actual.toList())
    }
}
