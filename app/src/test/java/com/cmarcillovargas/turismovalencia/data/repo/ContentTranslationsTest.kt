package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.fake.FakeAlojamientos
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventos
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventosWeb
import com.cmarcillovargas.turismovalencia.data.fake.FakeGastronomia
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVerWeb
import com.cmarcillovargas.turismovalencia.data.fake.FakeSostenible
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Las traducciones del contenido (`assets/i18n/<idioma>/content.json`) tienen que cubrir todo lo que hay en español: si alguien
 * añade una ficha, una oferta o una página, esta prueba avisa de en qué idiomas falta.
 */
class ContentTranslationsTest {
    private val items: List<ContentItem> = FakeQueVer + FakeQueVerWeb + FakeGastronomia + FakeAlojamientos + FakeEventos + FakeEventosWeb + FakeSostenible
    private val languages = listOf(LanguageCode.VA, LanguageCode.EN, LanguageCode.FR)

    private fun assets(path: String) = File("src/main/assets/$path")

    private fun texts(language: LanguageCode): LanguageTexts = ContentTranslations.parse(assets(ContentTranslations.pathOf(language)).readText(Charsets.UTF_8))

    private val web = JSONObject(assets("web/index.json").readText(Charsets.UTF_8))

    @Test
    fun `cada idioma tiene su archivo de traducciones`() {
        languages.forEach { assertTrue("Falta ${ContentTranslations.pathOf(it)}", assets(ContentTranslations.pathOf(it)).isFile) }
        assertEquals("i18n/va/content.json", ContentTranslations.pathOf(LanguageCode.VA))
    }

    @Test
    fun `todas las fichas estan traducidas y con la misma forma que el original`() {
        languages.forEach { language ->
            val texts = texts(language)
            val missing = items.map { it.id }.filter { it !in texts.items }
            assertTrue("$language: faltan las fichas $missing", missing.isEmpty())
            items.forEach { item ->
                val t = texts.items.getValue(item.id)
                val where = "$language/${item.id}"
                assertTrue("$where: sin título", !t.title.isNullOrBlank())
                assertTrue("$where: sin resumen", !t.summary.isNullOrBlank())
                assertTrue("$where: sin descripción", !t.description.isNullOrBlank())
                assertTrue("$where: sin etiqueta", !t.eyebrow.isNullOrBlank())
                assertTrue("$where: sin zona", !t.area.isNullOrBlank())
                assertEquals("$where: cifras", item.stats.size, t.stats.size)
                assertEquals("$where: información práctica", item.info.size, t.info.size)
                assertEquals("$where: actos", item.acts.size, t.acts.size)
                assertEquals("$where: horario de hoy", item.openUntil != null, t.openUntil != null)
                assertEquals("$where: fecha", item.dateLabel != null, t.dateLabel != null)
                assertEquals("$where: título de la ficha", item.detailTitle != null, t.detailTitle != null)
                assertEquals("$where: etiqueta de la ficha", item.detailEyebrow != null, t.detailEyebrow != null)
                assertEquals("$where: etiqueta del destacado", item.featureLabel != null, t.featureLabel != null)
                assertEquals("$where: subtítulo del destacado", item.featureSubtitle != null, t.featureSubtitle != null)
                assertEquals("$where: número de valoraciones", item.ratingCount != null, t.ratingCount != null)
                (t.stats + t.info + t.acts).forEach { (a, b) -> assertTrue("$where: texto vacío", a.isNotBlank() && b.isNotBlank()) }
                if (item.subcategory !in setOf("Tema", "Experiencia")) assertTrue("$where: sin subcategoría", !t.subcategory.isNullOrBlank())
            }
        }
    }

    @Test
    fun `una subcategoria se traduce siempre igual para que el filtro de la lista funcione`() {
        languages.forEach { language ->
            val texts = texts(language)
            items.filter { it.subcategory !in setOf("Tema", "Experiencia") }.groupBy { it.subcategory }.forEach { (spanish, group) ->
                val translated = group.map { texts.items.getValue(it.id).subcategory }.toSet()
                assertEquals("$language: «$spanish» se traduce de varias formas: $translated", 1, translated.size)
            }
        }
    }

    @Test
    fun `las subcategorias que usa el codigo como clave no se tocan`() {
        val item = FakeSostenible.first { it.id == "ecoturismo" }
        assertEquals("Tema", item.subcategory)
        val translated = item.translated(texts(LanguageCode.EN).items.getValue("ecoturismo"))
        assertEquals("Tema", translated.subcategory)
        assertEquals("Ecotourism", translated.title)
    }

    @Test
    fun `traducir una ficha cambia los textos y conserva ids, fotos y coordenadas`() {
        val original = FakeQueVer.first { it.id == "mercado-central" }
        val en = original.translated(texts(LanguageCode.EN).items.getValue(original.id))
        assertEquals("Central Market", en.title)
        assertEquals("Valencia Central Market", en.detailTitle)
        assertEquals("Monuments", en.subcategory)
        assertEquals(original.id, en.id)
        assertEquals(original.location, en.location)
        assertEquals(original.hero, en.hero)
        assertEquals(original.category, en.category)
        assertEquals(original.info.map { it.kind }, en.info.map { it.kind })
        assertTrue(en.keywords.containsAll(original.keywords))
        assertTrue("market" in en.keywords)
        assertNotEquals(original.description, en.description)
        // Las actos de las Fallas conservan sus fotos.
        val fallas = FakeEventos.first { it.id == "fallas" }
        val fr = fallas.translated(texts(LanguageCode.FR).items.getValue("fallas"))
        assertEquals(fallas.acts.map { it.image }, fr.acts.map { it.image })
        assertEquals("Mascletà", fr.acts.first().title)
    }

    @Test
    fun `todas las ofertas estan traducidas`() {
        languages.forEach { language ->
            val texts = texts(language)
            FakeOfertas.forEach { offer ->
                val t = texts.offers[offer.id]
                assertTrue("$language: falta la oferta ${offer.id}", t != null)
                val translated = offer.translated(t!!)
                assertTrue("$language/${offer.id}: sin título", translated.title.isNotBlank())
                assertTrue("$language/${offer.id}: sin descripción", translated.description.isNotBlank())
                // Lo que usa el código (horarios, precio, tipo, foto, enlace al lugar) no cambia.
                assertEquals(offer.times, translated.times)
                assertEquals(offer.priceFromCents, translated.priceFromCents)
                assertEquals(offer.kind, translated.kind)
                assertEquals(offer.placeId, translated.placeId)
                assertEquals(offer.image, translated.image)
                // La clave con la que se decide «gastronómica» sigue siendo la del servidor.
                assertEquals(offer.tagKey, translated.tagKey)
            }
        }
    }

    @Test
    fun `todas las paginas, tarjetas y secciones de la web tienen titulo en cada idioma`() {
        val pages = web.getJSONObject("pages").keys().asSequence().toSet()
        val home = web.getJSONObject("home")
        val tiles = listOf("esencial", "planes", "sostenible").flatMap { section ->
            val array = home.getJSONArray(section)
            List(array.length()) { array.getJSONObject(it).getString("route") }
        }.toSet()
        val sections = web.getJSONObject("categories").keys().asSequence().toSet()
        languages.forEach { language ->
            val texts = texts(language)
            assertEquals("$language: páginas", pages, texts.pages.keys)
            assertEquals("$language: tarjetas de la portada", tiles, texts.tiles.keys)
            assertEquals("$language: secciones", sections, texts.sections.keys)
            texts.pages.forEach { (id, page) ->
                assertTrue("$language/$id: sin título", !page.title.isNullOrBlank())
                assertTrue("$language/$id: sin etiqueta", !page.eyebrow.isNullOrBlank())
            }
            texts.tiles.forEach { (route, title) -> assertTrue("$language/$route: título vacío", title.isNotBlank()) }
        }
    }

    @Test
    fun `el cargador elige el archivo del idioma y el español no traduce nada`() {
        val loaded = mutableListOf<String>()
        val translations = ContentTranslations { path -> loaded += path; assets(path).takeIf { it.isFile }?.readText(Charsets.UTF_8) }
        assertEquals(LanguageCode.ES, translations.language)
        assertEquals(null, translations.item("mercado-central"))

        translations.setLanguage(LanguageCode.FR)
        assertEquals("Marché central", translations.item("mercado-central")?.title)
        assertEquals(listOf("i18n/fr/content.json"), loaded)

        translations.setLanguage(LanguageCode.ES)
        assertEquals(null, translations.item("mercado-central"))
        assertEquals(null, translations.offer("taller-paella"))
        assertEquals(listOf("i18n/fr/content.json"), loaded)
    }

    @Test
    fun `sin archivo de traducciones se ve el contenido original`() {
        val translations = ContentTranslations { null }
        translations.setLanguage(LanguageCode.EN)
        assertEquals(LanguageCode.EN, translations.language)
        assertEquals(null, translations.item("mercado-central"))
        assertEquals(null, translations.page("Navidad"))
    }
}
