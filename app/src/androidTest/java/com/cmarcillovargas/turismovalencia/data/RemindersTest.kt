package com.cmarcillovargas.turismovalencia.data

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.ReminderEntity
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.notify.EventReminders
import com.cmarcillovargas.turismovalencia.data.notify.NotificationAccess
import com.cmarcillovargas.turismovalencia.data.notify.ReminderNotifier
import com.cmarcillovargas.turismovalencia.data.notify.ReminderPlanner
import com.cmarcillovargas.turismovalencia.data.notify.ReminderReceiver
import com.cmarcillovargas.turismovalencia.data.notify.ReminderScheduler
import com.cmarcillovargas.turismovalencia.data.repo.RemindersRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Avisos de eventos: el calendario se programa y se cancela con `AlarmManager`, el receptor muestra la notificación y programa la
 * siguiente, y el repositorio programa solo lo que el usuario quiere cuando el sistema deja notificar. Las pruebas usan las alarmas
 * y notificaciones reales del móvil (con el paquete de la app: `connectedDebugAndroidTest` desinstala la app al acabar).
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersTest {
    private lateinit var context: Context
    private lateinit var scheduler: ReminderScheduler
    private lateinit var notifier: ReminderNotifier
    private val reminder = EventReminders.Mascleta
    private val toClose = mutableListOf<() -> Unit>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = ReminderScheduler(context)
        notifier = ReminderNotifier(context)
        scheduler.cancel(reminder.itemId)
        notifier.dismiss(reminder)
    }

    @After
    fun tearDown() {
        AppLanguagePrefs.write(context, LanguageCode.ES)
        toClose.forEach { it() }
        scheduler.cancel(reminder.itemId)
        notifier.dismiss(reminder)
    }

    private fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun awaitTrue(what: String, timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            if (System.currentTimeMillis() > end) throw AssertionError("Tiempo agotado esperando: $what")
            Thread.sleep(50)
        }
    }

    private fun activeReminderNotification() = context.getSystemService(NotificationManager::class.java).activeNotifications
        .firstOrNull { it.tag == ReminderNotifier.Tag }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Madrid")).toInstant()

    @Test
    fun elAvisoSaleEnElIdiomaDeLaApp() {
        grantNotifications()
        val manager = context.getSystemService(NotificationManager::class.java)
        val expected = mapOf(
            LanguageCode.ES to ("Mascletà a las 14:00" to "Avisos de eventos"),
            LanguageCode.VA to ("Mascletà a les 14:00" to "Avisos d’esdeveniments"),
            LanguageCode.EN to ("Mascletà at 14:00" to "Event alerts"),
            LanguageCode.FR to ("Mascletà à 14 h 00" to "Alertes d’événements"),
        )
        expected.forEach { (language, texts) ->
            notifier.dismiss(reminder)
            // El idioma se lee de las preferencias, no de Room: la alarma despierta el proceso sin abrir la base de datos.
            AppLanguagePrefs.write(context, language)
            assertTrue("$language: el aviso se muestra", ReminderNotifier(context).show(reminder))
            awaitTrue("$language: la notificación") { activeReminderNotification() != null }
            val shown = activeReminderNotification()!!.notification
            assertEquals("$language: título", texts.first, shown.extras.getString("android.title"))
            assertEquals("$language: texto", AppStrings(context, language).get(R.string.reminder_mascleta_text), shown.extras.getString("android.text"))
            // El canal se actualiza con el idioma de cada aviso.
            assertEquals("$language: nombre del canal", texts.second, manager.getNotificationChannel(ReminderNotifier.ChannelId).name.toString())
        }
    }

    @Test
    fun elProgramadorProgramaCancelaYSincroniza() {
        assertFalse(scheduler.isScheduled(reminder.itemId))
        val start = scheduler.schedule(reminder, after = at(2027, 3, 5, 10, 0))
        assertEquals(at(2027, 3, 5, 13, 20), start)
        assertTrue(scheduler.isScheduled(reminder.itemId))

        scheduler.cancel(reminder.itemId)
        assertFalse(scheduler.isScheduled(reminder.itemId))

        scheduler.sync(setOf(reminder.itemId))
        assertTrue(scheduler.isScheduled(reminder.itemId))
        scheduler.sync(emptySet())
        assertFalse(scheduler.isScheduled(reminder.itemId))
        // Un id desconocido no programa nada ni falla.
        scheduler.sync(setOf("no-existe"))
        assertFalse(scheduler.isScheduled("no-existe"))
    }

    @Test
    fun elReceptorMuestraElAvisoYProgramaElDelDiaSiguiente() {
        grantNotifications()
        val manager = context.getSystemService(NotificationManager::class.java)
        // La ventana de hoy acaba de empezar: el siguiente aviso tiene que ser el de mañana, nunca el mismo.
        val firedWindow = ReminderPlanner.next(reminder, Instant.now())
        context.sendBroadcast(
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ActionRemind)
                .putExtra(ReminderReceiver.ExtraItem, reminder.itemId)
                .putExtra(ReminderReceiver.ExtraWindowStart, firedWindow.toEpochMilli()),
        )

        awaitTrue("la notificación del aviso") { activeReminderNotification() != null }
        val shown = activeReminderNotification()!!.notification
        // Sin idioma guardado, el de la app por defecto (español), tenga el móvil el idioma que tenga.
        val spanish = AppStrings(context, LanguageCode.ES)
        assertEquals(spanish.get(reminder.title), shown.extras.getString("android.title"))
        assertEquals(spanish.get(reminder.text), shown.extras.getString("android.text"))
        assertEquals("Mascletà a las 14:00", shown.extras.getString("android.title"))
        assertNotNull("al tocarla abre la app", shown.contentIntent)
        assertTrue("se cierra al tocarla", shown.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)

        // El canal se crea al mostrar el primer aviso, con importancia alta.
        val channel = manager.getNotificationChannel(ReminderNotifier.ChannelId)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)

        // Y queda programado el siguiente.
        awaitTrue("la alarma del siguiente aviso") { scheduler.isScheduled(reminder.itemId) }
    }

    @Test
    fun elReceptorIgnoraEventosSinAviso() {
        grantNotifications()
        context.sendBroadcast(
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ActionRemind)
                .putExtra(ReminderReceiver.ExtraItem, "mercado-central"),
        )
        Thread.sleep(800)
        assertEquals(null, activeReminderNotification())
        assertFalse(scheduler.isScheduled("mercado-central"))
        assertFalse(scheduler.isScheduled(reminder.itemId))
    }

    // ---- Repositorio: programa solo lo que el usuario quiere cuando el sistema deja notificar ----

    private inner class Session {
        val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val scope = testScope()
        val allowed = booleanArrayOf(true)
        val access = NotificationAccess { allowed[0] }
        val settings = SettingsRepository(SettingsStore(db.settings(), scope))
        val reminders = RemindersRepository(db.reminders(), settings, access, scheduler, scope)

        init {
            toClose += { scope.closeAndJoin(); db.close() }
        }
    }

    /** Las escrituras son asíncronas: espera a que el estado llegue al valor esperado. */
    private fun <T> StateFlow<T>.await(expected: T) = runBlocking { withTimeout(5_000) { first { it == expected } } }

    @Test
    fun elRepositorioProgramaLoQueElUsuarioPideCuandoSePuedeNotificar() {
        val s = Session()
        assertTrue(s.reminders.ids.value.isEmpty())
        assertFalse(scheduler.isScheduled(reminder.itemId))

        s.reminders.set(reminder.itemId, true)
        s.reminders.ids.await(setOf(reminder.itemId))
        awaitTrue("alarma al pedir el aviso") { scheduler.isScheduled(reminder.itemId) }

        // El sistema deja de permitir notificar (el usuario las apaga en Ajustes): se cancela; al volver a permitirlas, se programa.
        s.allowed[0] = false
        s.access.refresh()
        s.reminders.active.await(false)
        awaitTrue("alarma cancelada sin permiso") { !scheduler.isScheduled(reminder.itemId) }
        s.allowed[0] = true
        s.access.refresh()
        s.reminders.active.await(true)
        awaitTrue("alarma de nuevo con permiso") { scheduler.isScheduled(reminder.itemId) }

        // El ajuste general de Notificaciones también manda.
        s.settings.setNotificationsEnabled(false)
        s.reminders.active.await(false)
        awaitTrue("alarma cancelada con Notificaciones apagado") { !scheduler.isScheduled(reminder.itemId) }
        s.settings.setNotificationsEnabled(true)
        awaitTrue("alarma de nuevo con Notificaciones encendido") { scheduler.isScheduled(reminder.itemId) }

        // Apagar el aviso lo cancela y lo guarda.
        s.reminders.set(reminder.itemId, false)
        s.reminders.ids.await(emptySet())
        awaitTrue("alarma cancelada al apagar el aviso") { !scheduler.isScheduled(reminder.itemId) }
    }

    @Test
    fun alReiniciarElMovilSeReprogramaConSyncNow() {
        val s = Session()
        s.reminders.set(reminder.itemId, true)
        s.reminders.ids.await(setOf(reminder.itemId))
        awaitTrue("alarma inicial") { scheduler.isScheduled(reminder.itemId) }

        // Android borra las alarmas al reiniciar: lo simulamos y comprobamos que `syncNow` (lo que hace BootReceiver) las devuelve.
        scheduler.cancel(reminder.itemId)
        assertFalse(scheduler.isScheduled(reminder.itemId))
        s.reminders.syncNow()
        assertTrue(scheduler.isScheduled(reminder.itemId))

        // Sin permiso, `syncNow` no programa nada.
        s.allowed[0] = false
        s.access.refresh()
        s.reminders.active.await(false)
        scheduler.cancel(reminder.itemId)
        s.reminders.syncNow()
        assertFalse(scheduler.isScheduled(reminder.itemId))
    }

    @Test
    fun losAvisosPedidosSobrevivenAReabrirLaBaseDeDatos() {
        val name = "reminders-${System.nanoTime()}.db"
        try {
            fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val first = open()
            runBlocking { first.reminders().insert(ReminderEntity("fallas")) }
            first.close()
            val second = open()
            assertEquals(listOf("fallas"), runBlocking { second.reminders().observeIds().first() })
            runBlocking { second.reminders().delete("fallas") }
            second.close()
            val third = open()
            assertEquals(emptyList<String>(), runBlocking { third.reminders().observeIds().first() })
            third.close()
        } finally {
            context.deleteDatabase(name)
        }
    }
}
