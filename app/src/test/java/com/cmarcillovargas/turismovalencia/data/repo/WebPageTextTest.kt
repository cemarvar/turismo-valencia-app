package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebItem
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El texto de las 65 páginas de la web en valenciano, inglés y francés (`assets/i18n/<idioma>/pages/<Pagina>.json`, que genera
 * `tools/i18n/pages.py`) tiene que estar completo y con la forma y los datos del español: si alguien vuelve a importar la web o cambia una
 * traducción a mano, esta prueba dice qué página de qué idioma falla.
 */
class WebPageTextTest {
    private val languages = listOf(LanguageCode.VA, LanguageCode.EN, LanguageCode.FR)
    private val nbsp = ' '

    private fun assets(path: String) = File("src/main/assets/$path")

    private val pages: Map<String, WebPage> = assets("web/pages").listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
        .associate { it.nameWithoutExtension to WebPageParser.parse(it.nameWithoutExtension, JSONObject(it.readText(Charsets.UTF_8))) }

    private fun translation(language: LanguageCode, id: String): PageTranslation? =
        assets(PageTranslation.pathOf(language, id)).takeIf { it.isFile }?.readText(Charsets.UTF_8)?.let(PageTranslation::parse)

    /** Todos los textos de una página en un orden fijo (el mismo para el original y para su traducción, que tienen la misma forma). */
    private fun WebPage.texts(): List<String> = lead + blocks.flatMap { block ->
        when (block) {
            is WebBlock.Items -> listOf(block.title) + block.filters.map { it.label } + block.items.flatMap { it.texts() }
            is WebBlock.Facts -> listOf(block.title) + block.rows.flatMap { it.toList() }
            is WebBlock.Bullets -> listOf(block.title) + block.items
        }
    }

    private fun WebItem.texts(): List<String> =
        listOfNotNull(kicker, title, subtitle, body, bulletsTitle) + facts.flatMap { it.toList() } + bullets + notes.flatMap { it.toList() } + tags.map { it.label }

    /** Las cifras de un texto, sin ceros a la izquierda y sin los «00» de los minutos («10:00» es «10 h» en francés; «08:00», «8 h»). */
    private fun digits(text: String): Map<String, Int> =
        Regex("""\d+""").findAll(text).map { it.value }.filter { it != "00" }.map { it.trimStart('0').ifEmpty { "0" } }.groupingBy { it }.eachCount()

    @Test
    fun `la web tiene las 65 paginas`() {
        assertEquals(65, pages.size)
    }

    @Test
    fun `cada pagina esta traducida a los tres idiomas`() {
        val missing = languages.flatMap { language -> pages.keys.filter { translation(language, it) == null }.map { "$language/$it" } }
        assertTrue("Faltan ${missing.size} traducciones (python tools/i18n/pages.py status):\n" + missing.take(30).joinToString("\n"), missing.isEmpty())
    }

    @Test
    fun `las paginas traducidas tienen la forma del original`() {
        for (language in languages) for ((id, page) in pages) {
            val t = translation(language, id) ?: continue
            val translated = page.translated(t, language)
            assertNotNull("$language/$id: la traducción no tiene la forma de la página en español", translated)
            assertEquals("$language/$id", language, translated!!.textLanguage)
        }
    }

    @Test
    fun `los textos traducidos estan completos y no pierden datos`() {
        val problems = mutableListOf<String>()
        for (language in languages) for ((id, page) in pages) {
            val translated = page.translated(translation(language, id) ?: continue, language) ?: continue
            page.texts().zip(translated.texts()).forEach { (es, text) ->
                val where = "$language/$id: «${es.take(40)}»"
                when {
                    es.isBlank() -> if (text.isNotBlank()) problems += "$where: debería estar vacío"
                    text.isBlank() -> problems += "$where: sin traducir"
                    else -> {
                        if ("[object" in text || Regex("</?[a-zA-Z][^>]*>").containsMatchIn(text)) problems += "$where: lleva marcas de código"
                        if ('"' in text) problems += "$where: comillas rectas"
                        if ('\'' in text) problems += "$where: apóstrofo recto"
                        // Los datos que no se traducen (la «5/5 » de un dato) se copian tal cual, con los espacios que ya tengan en español.
                        if ((text != text.trim() && es == es.trim()) || ("  " in text && "  " !in es)) problems += "$where: espacios de más"
                        val lost = digits(es).filter { (number, count) -> (digits(text)[number] ?: 0) < count }.keys
                        if (lost.isNotEmpty()) problems += "$where: faltan las cifras $lost"
                        if (language == LanguageCode.FR && Regex(" [:;?!»]|« ").containsMatchIn(text)) problems += "$where: espacio normal ante la puntuación francesa"
                    }
                }
            }
        }
        assertTrue("${problems.size} problemas:\n" + problems.take(30).joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `el frances usa espacios de no separacion en los miles y las unidades`() {
        val problems = mutableListOf<String>()
        for ((id, page) in pages) {
            val translated = page.translated(translation(LanguageCode.FR, id) ?: continue, LanguageCode.FR) ?: continue
            translated.texts().forEach { text ->
                if (Regex("""\d \d{3}(?!\d)""").containsMatchIn(text) || Regex("""\d (€|km|m²|%)""").containsMatchIn(text)) problems += "fr/$id: «${text.take(60)}»"
            }
        }
        assertTrue("Falta el espacio de no separación ($nbsp):\n" + problems.take(20).joinToString("\n"), problems.isEmpty())
    }
}
