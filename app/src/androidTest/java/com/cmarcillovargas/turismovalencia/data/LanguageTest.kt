package com.cmarcillovargas.turismovalencia.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Idiomas: los textos de cada idioma salen de sus recursos (también el valenciano, que es `values-ca`), el contenido y los títulos de
 * la web se traducen con los archivos de `assets/i18n`, y elegir un idioma lo pide al sistema una sola vez y lo guarda.
 */
@RunWith(AndroidJUnit4::class)
class LanguageTest {
    private lateinit var app: Application
    private lateinit var web: WebContentRepository
    private lateinit var strings: AppStrings
    private lateinit var content: ContentRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        web = WebContentRepository(app)
        strings = AppStrings(app)
        content = ContentRepository(web, strings = strings)
    }

    @After
    fun tearDown() {
        content.setLanguage(LanguageCode.ES)
    }

    @Test
    fun cadaIdiomaTieneSusRecursos() {
        val nav = LanguageCode.entries.associateWith { language ->
            strings.language = language
            strings.get(R.string.nav_inicio)
        }
        assertEquals(mapOf(LanguageCode.ES to "Inicio", LanguageCode.VA to "Inici", LanguageCode.EN to "Home", LanguageCode.FR to "Accueil"), nav)
    }

    @Test
    fun cadaIdiomaSabeCualEs() {
        // El idioma que dan los recursos (`locale_code`) coincide con el que se les pidió.
        LanguageCode.entries.forEach { language ->
            strings.language = language
            assertEquals(language, LanguageCode.fromCode(strings.get(R.string.locale_code)))
        }
    }

    @Test
    fun losPluralesYLasListasSonDeCadaIdioma() {
        strings.language = LanguageCode.EN
        assertEquals("1 booking", strings.plural(R.plurals.res_count, 1, 1))
        assertEquals("3 bookings", strings.plural(R.plurals.res_count, 3, 3))
        assertEquals("What can I see in Valencia for free today?", strings.array(R.array.valbot_suggestions).first())
        strings.language = LanguageCode.FR
        assertEquals("1 réservation", strings.plural(R.plurals.res_count, 1, 1))
        assertEquals(6, strings.array(R.array.valbot_suggestions).size)
        strings.language = LanguageCode.VA
        assertEquals("2 persones · 36 €", strings.plural(R.plurals.res_people_price, 2, 2, "36 €"))
        strings.language = LanguageCode.ES
        assertEquals("1 pers. · 18 €", strings.plural(R.plurals.res_people_price, 1, 1, "18 €"))
    }

    @Test
    fun lasFichasSeTraducenAlCambiarDeIdioma() {
        assertEquals("Mercado Central", content.item("mercado-central")!!.title)
        content.setLanguage(LanguageCode.EN)
        val en = content.item("mercado-central")!!
        assertEquals("Central Market", en.title)
        assertEquals("Valencia Central Market", en.detailTitle)
        assertEquals("Monuments", en.subcategory)
        content.setLanguage(LanguageCode.VA)
        assertEquals("Mercat Central", content.item("mercado-central")!!.title)
        content.setLanguage(LanguageCode.FR)
        assertEquals("Marché central", content.item("mercado-central")!!.title)
        content.setLanguage(LanguageCode.ES)
        assertEquals("Mercado Central", content.item("mercado-central")!!.title)
    }

    @Test
    fun todasLasFichasTienenTituloEnCadaIdioma() {
        val spanish = content.all.associate { it.id to it.title }
        LanguageCode.entries.forEach { language ->
            content.setLanguage(language)
            assertEquals("$language: mismas fichas", spanish.keys, content.all.map { it.id }.toSet())
            content.all.forEach { item ->
                assertTrue("$language/${item.id}: sin título", item.title.isNotBlank())
                assertTrue("$language/${item.id}: sin descripción", item.description.isNotBlank())
            }
        }
    }

    @Test
    fun lasFichasConservanSuCategoriaYSusFiltros() {
        // Cada filtro de la lista de una sección sale de las subcategorías de sus fichas: en cualquier idioma, sin repeticiones raras.
        LanguageCode.entries.forEach { language ->
            content.setLanguage(language)
            Category.entries.filter { it != Category.Sostenible }.forEach { category ->
                val filters = content.byCategory(category).map { it.subcategory }
                assertEquals("$language/$category: filtros", filters.distinct().sorted(), filters.toSet().sorted())
            }
            // Las claves que usa el código no se traducen.
            assertEquals("Tema", content.item("ecoturismo")!!.subcategory)
            assertEquals("Experiencia", content.item("albufera-barca")!!.subcategory)
            assertEquals("$language: los temas sostenibles", 6, content.sustainableTopics().size)
        }
    }

    @Test
    fun lasOfertasSeTraducenYConservanLoQueUsaElCodigo() {
        content.setLanguage(LanguageCode.EN)
        val offer = content.offer("taller-paella")!!
        assertEquals("Valencian paella workshop", offer.title)
        assertEquals("Gastronomic", offer.tag)
        assertEquals("Gastronómica", offer.tagKey)
        assertEquals(FakeOfertas.first { it.id == "taller-paella" }.times, offer.times)
        assertEquals(FakeOfertas.size, content.offers.size)
        content.setLanguage(LanguageCode.ES)
        assertEquals("Taller de paella valenciana", content.offer("taller-paella")!!.title)
    }

    @Test
    fun elBuscadorEncuentraPorElNombreEnCadaIdiomaYTambienEnEspanol() {
        content.setLanguage(LanguageCode.EN)
        assertTrue(content.search("silk exchange").any { it.target == "lonja-seda" })
        assertTrue("también por el nombre original", content.search("lonja").any { it.target == "lonja-seda" })
        content.setLanguage(LanguageCode.FR)
        assertTrue(content.search("bourse de la soie").any { it.target == "lonja-seda" })
        content.setLanguage(LanguageCode.VA)
        assertTrue(content.search("llotja").any { it.target == "lonja-seda" })
        // Sugerencias con sus textos: «Ver … en el mapa» en el idioma de la app.
        content.setLanguage(LanguageCode.EN)
        strings.language = LanguageCode.EN
        val map = content.search("market").firstOrNull { it.kind.name == "Map" }
        assertTrue("hay sugerencia de mapa", map != null)
        assertTrue(map!!.subtitle.startsWith("Map · "))
    }

    @Test
    fun losTitulosDeLaWebSeTraducen() {
        content.setLanguage(LanguageCode.EN)
        assertEquals("The brightest Christmas", web.index.pages.getValue("Navidad").title)
        assertEquals("Christmas", web.index.esencial.first { it.route == "Navidad" }.title)
        assertEquals("Unique places", web.categoryIndex(Category.QueVer)!!.title)
        content.setLanguage(LanguageCode.ES)
        assertEquals("La Navidad más luminosa", web.index.pages.getValue("Navidad").title)
        assertEquals("Navidad", web.index.esencial.first { it.route == "Navidad" }.title)
    }

    @Test
    fun elIdiomaElegidoSePideAlSistemaUnaVezYSeGuarda() {
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val requested = mutableListOf<LanguageCode>()
            val settings = SettingsRepository(SettingsStore(db.settings(), scope), languageApplier = LanguageApplier { requested += it })
            assertEquals(LanguageCode.ES, settings.language.value)
            settings.setLanguage(LanguageCode.FR)
            assertEquals(listOf(LanguageCode.FR), requested)
            awaitLanguage(settings, LanguageCode.FR)
            // Anotar el idioma con el que ya arrancó la actividad no se lo vuelve a pedir al sistema.
            settings.syncLanguage(LanguageCode.VA)
            awaitLanguage(settings, LanguageCode.VA)
            assertEquals(listOf(LanguageCode.FR), requested)
            assertFalse(requested.isEmpty())
            assertNotEquals(LanguageCode.ES, settings.language.value)
        } finally {
            scope.cancel()
            db.close()
        }
    }

    private fun awaitLanguage(settings: SettingsRepository, expected: LanguageCode) {
        val end = System.currentTimeMillis() + 5_000
        while (settings.language.value != expected) {
            if (System.currentTimeMillis() > end) throw AssertionError("Tiempo agotado: el idioma sigue en ${settings.language.value}")
            Thread.sleep(40)
        }
    }

    @Test
    fun elUltimoIdiomaSeGuardaAparteParaLasAlarmas() {
        AppLanguagePrefs.write(app, LanguageCode.EN)
        assertEquals(LanguageCode.EN, AppLanguagePrefs.read(app))
        AppLanguagePrefs.write(app, LanguageCode.ES)
        assertEquals(LanguageCode.ES, AppLanguagePrefs.read(app))
    }
}
