package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import com.cmarcillovargas.turismovalencia.data.model.WebFilter
import com.cmarcillovargas.turismovalencia.data.model.WebItem
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.data.model.WebTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Cómo se aplica la traducción de una página sobre el original en español (los archivos reales los revisa `WebPageTextTest`). */
class PageTranslationsTest {
    private val spanish = WebPage(
        id = "Demo",
        family = WebFamily.QueVer,
        title = "Demo",
        eyebrow = "Centro",
        lead = listOf("Primer párrafo.", "Segundo párrafo."),
        hero = "img/hero.webp",
        blocks = listOf(
            WebBlock.Items(
                title = "Zonas",
                filters = listOf(WebFilter("arte", "Arte y pintura")),
                items = listOf(
                    WebItem(
                        num = "01",
                        kicker = "Gastronomía",
                        title = "El mercado",
                        body = "Un mercado muy grande.",
                        facts = listOf("Horario" to "Lunes a sábado"),
                        bullets = listOf("Llega pronto"),
                        bulletsTitle = "Consejos",
                        notes = listOf("Nota" to "Cierra los domingos"),
                        tags = listOf(WebTag("Gratuito", free = true), WebTag("Familia")),
                        image = "img/m.webp",
                        url = "https://example.com",
                        category = "arte",
                    ),
                ),
            ),
            WebBlock.Facts("Datos de la visita", listOf("Precio" to "Gratuito")),
            WebBlock.Bullets("Consejos", listOf("Uno", "Dos")),
        ),
    )

    private val lead = """"lead":["First paragraph.","Second paragraph."]"""
    private val items = """{"title":"Areas","filters":["Art and painting"],"items":[{"kicker":"Gastronomy","title":"The market","body":"A very big market.",""" +
        """"facts":[["Hours","Monday to Saturday"]],"bullets":["Arrive early"],"bulletsTitle":"Tips","notes":[["Note","Closed on Sundays"]],""" +
        """"tags":["Free","Family"]}]}"""
    private val facts = """{"title":"Visit information","rows":[["Price","Free"]]}"""
    private val bullets = """{"title":"Tips","items":["One","Two"]}"""

    private fun json(lead: String = this.lead, blocks: List<String> = listOf(items, facts, bullets)) = "{$lead,\"blocks\":[${blocks.joinToString(",")}]}"

    @Test
    fun `la traduccion cambia los textos y deja lo que no se traduce`() {
        val page = spanish.translated(PageTranslation.parse(json()), LanguageCode.EN)
        assertNotNull(page)
        page!!
        assertEquals(LanguageCode.EN, page.textLanguage)
        assertEquals(listOf("First paragraph.", "Second paragraph."), page.lead)
        assertEquals("img/hero.webp", page.hero)

        val translated = page.blocks[0] as WebBlock.Items
        assertEquals("Areas", translated.title)
        assertEquals(listOf(WebFilter("arte", "Art and painting")), translated.filters)
        val item = translated.items.single()
        assertEquals("The market", item.title)
        assertEquals("Gastronomy", item.kicker)
        assertEquals(listOf("Hours" to "Monday to Saturday"), item.facts)
        assertEquals(listOf("Arrive early"), item.bullets)
        assertEquals("Tips", item.bulletsTitle)
        assertEquals(listOf("Note" to "Closed on Sundays"), item.notes)
        // Lo que no es texto sale del original: el número, la foto, el enlace, el filtro y qué etiqueta es «gratuita».
        assertEquals("01", item.num)
        assertEquals("img/m.webp", item.image)
        assertEquals("https://example.com", item.url)
        assertEquals("arte", item.category)
        assertEquals(listOf(WebTag("Free", free = true), WebTag("Family")), item.tags)

        assertEquals(WebBlock.Facts("Visit information", listOf("Price" to "Free")), page.blocks[1])
        assertEquals(WebBlock.Bullets("Tips", listOf("One", "Two")), page.blocks[2])
    }

    @Test
    fun `una traduccion con otra forma no se aplica`() {
        // Cada una por separado deja la página en español: un párrafo, un dato, una etiqueta, un bloque, un consejo o un título de menos.
        listOf(
            json(lead = """"lead":["First paragraph."]"""),
            json(blocks = listOf(items.replace("""["Hours","Monday to Saturday"]""", ""), facts, bullets)),
            json(blocks = listOf(items.replace(""","Family"]""", "]"), facts, bullets)),
            json(blocks = listOf(items, facts)),
            json(blocks = listOf(items, facts, bullets.replace(""","Two"]""", "]"))),
            json(blocks = listOf(items.replace(""""title":"The market",""", ""), facts, bullets)),
            json(blocks = listOf(items, facts.replace(""""title":"Visit information",""", ""), bullets)),
        ).forEach { translation ->
            assertNull(translation, spanish.translated(PageTranslation.parse(translation), LanguageCode.EN))
        }
    }

    @Test
    fun `la ruta de cada traduccion sale del idioma y la pagina`() {
        assertEquals("i18n/va/pages/MercadoCentral.json", PageTranslation.pathOf(LanguageCode.VA, "MercadoCentral"))
        assertEquals("i18n/en/pages/Bici.json", PageTranslation.pathOf(LanguageCode.EN, "Bici"))
        assertEquals("i18n/fr/pages/Fallas.json", PageTranslation.pathOf(LanguageCode.FR, "Fallas"))
    }
}
