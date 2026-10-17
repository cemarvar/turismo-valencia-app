package com.cmarcillovargas.turismovalencia.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.DemoData
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import com.cmarcillovargas.turismovalencia.data.remote.NoOffersService
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import com.cmarcillovargas.turismovalencia.data.repo.DownloadsRepository
import com.cmarcillovargas.turismovalencia.data.repo.FavoritesRepository
import com.cmarcillovargas.turismovalencia.data.repo.NotificationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.SearchHistoryRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Lo que el usuario guarda sobrevive a «cerrar y abrir la app». Cada prueba usa una base de datos en archivo que se cierra
 * y se vuelve a abrir (una [Session] por apertura), que es lo que pasa cuando el proceso muere y arranca de nuevo.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private lateinit var context: Context
    private lateinit var content: ContentRepository
    private lateinit var dbName: String
    private val open = mutableListOf<Session>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        content = ContentRepository(WebContentRepository(context))
        dbName = "test-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        open.toList().forEach { it.close() }
        context.deleteDatabase(dbName)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private inner class Session(demo: Boolean = false) {
        val db: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

        init {
            if (demo) runBlocking { DemoData(content).seedIfFirstRun(db) }
        }

        private val store = SettingsStore(db.settings(), scope)
        val favorites = FavoritesRepository(db.favorites(), scope)
        val downloads = DownloadsRepository(content, db.downloads(), store, scope)
        val reservations = ReservationsRepository(content, db.reservations(), scope)
        val settings = SettingsRepository(store)
        val notifications = NotificationsRepository(db.notifications(), scope)
        val searches = SearchHistoryRepository(db.recentSearches(), scope)

        init {
            open += this
        }

        fun close() {
            scope.cancel()
            db.close()
            open -= this
        }
    }

    /** Las escrituras son asíncronas: espera a que el estado llegue al valor esperado (ya está en la base de datos). */
    private fun <T> StateFlow<T>.await(expected: T) = runBlocking { withTimeout(5_000) { first { it == expected } } }

    private fun <T> StateFlow<T>.awaitThat(predicate: (T) -> Boolean) = runBlocking { withTimeout(5_000) { first(predicate) } }

    @Test
    fun favoritosPersistenYConservanElOrden() {
        val first = Session()
        assertTrue(first.favorites.ids.value.isEmpty())
        listOf("a", "b", "c").forEach { first.favorites.toggle(it) }
        first.favorites.ids.await(listOf("a", "b", "c"))
        first.favorites.toggle("b")
        first.favorites.ids.await(listOf("a", "c"))
        first.favorites.toggle("b")
        first.favorites.ids.await(listOf("a", "c", "b"))
        first.close()

        val second = Session()
        assertEquals(listOf("a", "c", "b"), second.favorites.ids.value)
        second.favorites.clear()
        second.favorites.ids.await(emptyList())
        second.close()

        assertTrue(Session().favorites.ids.value.isEmpty())
    }

    @Test
    fun descargasYSusAjustesPersisten() {
        val first = Session()
        first.downloads.toggle("fallas")
        first.downloads.toggle("paella")
        first.downloads.toggle("mercado-central")
        first.downloads.remove("paella")
        first.downloads.setWifiOnly(false)
        first.downloads.setAutoUpdate(false)
        first.downloads.setMapDownloaded(false)
        first.downloads.ids.await(listOf("fallas", "mercado-central"))
        first.downloads.wifiOnly.await(false)
        first.downloads.autoUpdate.await(false)
        first.downloads.mapDownloaded.await(false)
        first.close()

        val second = Session()
        assertEquals(listOf("fallas", "mercado-central"), second.downloads.ids.value)
        assertFalse(second.downloads.wifiOnly.value)
        assertFalse(second.downloads.autoUpdate.value)
        assertFalse(second.downloads.mapDownloaded.value)
        second.downloads.removeAll()
        second.downloads.ids.await(emptyList())
    }

    @Test
    fun reservasSeNumeranYPersisten() {
        val first = Session()
        val date = LocalDate.now().plusDays(3)
        val one = runBlocking { first.reservations.create("albufera-barca", date, "18:30", 2, 1, "a@b.es") }
        val two = runBlocking { first.reservations.create("taller-paella", date.plusDays(1), "13:00", 4, 0, "a@b.es") }

        assertEquals("TV-${date.year}-0413", one.locator)
        assertEquals("TV-${date.plusDays(1).year}-0414", two.locator)
        assertEquals(ReservationStatus.Confirmed, one.status)
        assertEquals(3, one.people)
        // La reserva ya está en la lista cuando `create` devuelve (la pantalla de confirmación la encuentra).
        assertEquals(one, first.reservations.get(one.id))
        assertEquals(listOf(two.id, one.id), first.reservations.items.value.map { it.id })
        first.close()

        val second = Session()
        assertEquals(listOf(two.id, one.id), second.reservations.items.value.map { it.id })
        assertEquals(one, second.reservations.get(one.id))
        val three = runBlocking { second.reservations.create("albufera-barca", date, "16:30", 1, 0, "c@d.es") }
        assertEquals("TV-${date.year}-0415", three.locator)
    }

    @Test
    fun lasReservasDeDemoTienenSuEstado() {
        val seeds = Session(demo = true).reservations.items.value.associateBy { it.id }
        assertEquals(ReservationStatus.Confirmed, seeds.getValue("seed-1").status)
        assertEquals(ReservationStatus.Pending, seeds.getValue("seed-2").status)
        assertEquals(ReservationStatus.Completed, seeds.getValue("seed-3").status)
        assertEquals("TV-${seeds.getValue("seed-1").date.year}-0413", seeds.getValue("seed-1").locator)
    }

    @Test
    fun ajustesPersisten() {
        val first = Session()
        assertFalse(first.settings.onboardingDone.value)
        first.settings.setLanguage(LanguageCode.VA)
        first.settings.setTextScale(TextScale.Large)
        first.settings.setLocation(LocationMode.OnlyOnce)
        first.settings.setNotificationsEnabled(false)
        first.settings.setOnboardingDone(true)
        first.settings.onboardingDone.await(true)
        first.settings.language.await(LanguageCode.VA)
        first.settings.textScale.await(TextScale.Large)
        first.settings.location.await(LocationMode.OnlyOnce)
        first.settings.notificationsEnabled.await(false)
        first.close()

        val second = Session()
        assertEquals(LanguageCode.VA, second.settings.language.value)
        assertEquals(TextScale.Large, second.settings.textScale.value)
        assertEquals(LocationMode.OnlyOnce, second.settings.location.value)
        assertFalse(second.settings.notificationsEnabled.value)
        assertTrue(second.settings.onboardingDone.value)
        // Cerrar sesión vuelve a pedir la bienvenida.
        second.settings.setOnboardingDone(false)
        second.settings.onboardingDone.await(false)
    }

    @Test
    fun cambiarUnAjusteNoPisaLosDemas() {
        val session = Session()
        session.settings.setTextScale(TextScale.ExtraLarge)
        session.downloads.setWifiOnly(false)
        session.settings.language.await(LanguageCode.ES)
        session.settings.textScale.await(TextScale.ExtraLarge)
        session.downloads.wifiOnly.await(false)
        assertEquals(TextScale.ExtraLarge, session.settings.textScale.value)
        assertFalse(session.downloads.wifiOnly.value)
        assertTrue(session.downloads.autoUpdate.value)
    }

    @Test
    fun busquedasRecientesSinDuplicadosYConTope() {
        val first = Session()
        listOf("mercado", "paella", "Fallas", "playa", "museo", "bioparc").forEach { text ->
            first.searches.add(text)
            first.searches.recent.awaitThat { it.firstOrNull() == text }
        }
        assertEquals(listOf("bioparc", "museo", "playa", "Fallas", "paella"), first.searches.recent.value)

        first.searches.add("  FALLAS ")
        first.searches.recent.awaitThat { it.firstOrNull() == "FALLAS" }
        assertEquals(listOf("FALLAS", "bioparc", "museo", "playa", "paella"), first.searches.recent.value)
        first.searches.add("   ")
        first.close()

        val second = Session()
        assertEquals(listOf("FALLAS", "bioparc", "museo", "playa", "paella"), second.searches.recent.value)
        second.searches.clear()
        second.searches.recent.await(emptyList())
    }

    @Test
    fun avisosLeidosPersisten() {
        val first = Session(demo = true)
        assertEquals(2, first.notifications.items.value.count { it.unread })
        first.notifications.markRead("n1")
        first.notifications.items.awaitThat { list -> list.count { it.unread } == 1 }
        first.close()

        val second = Session()
        assertEquals(listOf("n2"), second.notifications.items.value.filter { it.unread }.map { it.id })
        second.notifications.markAllRead()
        second.notifications.items.awaitThat { list -> list.none { it.unread } }
        second.close()

        assertTrue(Session().notifications.items.value.none { it.unread })
    }

    @Test
    fun primerArranqueRellenaLaDemoUnaSolaVez() {
        val first = Session(demo = true)
        assertEquals(listOf("mercado-central", "albufera-barca", "fallas", "paella", "lonja-seda", "catedral-caliz"), first.favorites.ids.value)
        assertEquals(listOf("mercado-central", "fallas", "paella", "lonja-seda"), first.downloads.ids.value)
        assertEquals(listOf("seed-1", "seed-2", "seed-3"), first.reservations.items.value.map { it.id })
        assertEquals(listOf("Mercado Central", "Fallas 2027"), first.searches.recent.value)
        assertEquals(5, first.notifications.items.value.size)
        assertEquals(listOf("fallas"), runBlocking { first.db.reminders().observeIds().first() })
        assertFalse(first.settings.onboardingDone.value)

        // El usuario lo quita todo y reinicia: la demo no vuelve a aparecer.
        first.favorites.clear()
        first.favorites.ids.await(emptyList())
        first.close()

        val second = Session(demo = true)
        assertTrue(second.favorites.ids.value.isEmpty())
        assertEquals(3, second.reservations.items.value.size)

        // Restablecer (gancho de depuración) devuelve la demo.
        runBlocking { DemoData(content).populate(second.db) }
        second.favorites.ids.awaitThat { it.size == 6 }
    }

    @Test
    fun elContenedorArrancaConLaDemoYSePuedeRestablecer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val memory = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        try {
            val container = AppContainer(app, memory, NoOffersService)
            assertEquals(6, container.favorites.ids.value.size)
            assertEquals(setOf("fallas"), container.reminders.ids.value)
            container.favorites.clear()
            container.favorites.ids.await(emptyList())
            container.reminders.set("fallas", false)
            container.reminders.ids.await(emptySet())
            container.resetToDemo()
            container.favorites.ids.awaitThat { it.size == 6 }
            container.reminders.ids.await(setOf("fallas"))
            assertFalse(container.settings.onboardingDone.value)
        } finally {
            memory.close()
        }
    }
}
