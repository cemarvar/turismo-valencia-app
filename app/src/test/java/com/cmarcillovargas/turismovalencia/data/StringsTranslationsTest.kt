package com.cmarcillovargas.turismovalencia.data

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Los textos de la interfaz (`strings.xml` de cada carpeta `values-xx`) están en los cuatro idiomas: no falta ninguno, llevan los mismos marcadores
 * (`%1$s`, `%2$d`…) y los plurales y las listas tienen la misma forma. Lint también avisa de lo que falta (`MissingTranslation`); esto
 * además compara los marcadores y se ejecuta con `./gradlew test`.
 */
class StringsTranslationsTest {
    private sealed interface Entry {
        val translatable: Boolean

        data class Text(val value: String, override val translatable: Boolean) : Entry
        data class Plurals(val items: Map<String, String>, override val translatable: Boolean) : Entry
        data class Items(val items: List<String>, override val translatable: Boolean) : Entry
    }

    private fun load(folder: String): Map<String, Entry> {
        val file = File("src/main/res/$folder/strings.xml")
        assertTrue("Falta ${file.path}", file.isFile)
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val entries = linkedMapOf<String, Entry>()
        for (i in 0 until root.childNodes.length) {
            val node = root.childNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element
            val name = element.getAttribute("name")
            val translatable = element.getAttribute("translatable") != "false"
            val children = (0 until element.childNodes.length).map { element.childNodes.item(it) }.filter { it.nodeType == Node.ELEMENT_NODE } as List<Element>
            entries[name] = when (element.tagName) {
                "string" -> Entry.Text(element.textContent, translatable)
                "plurals" -> Entry.Plurals(children.associate { it.getAttribute("quantity") to it.textContent }, translatable)
                "string-array" -> Entry.Items(children.map { it.textContent }, translatable)
                else -> error("Elemento no esperado: ${element.tagName}")
            }
        }
        return entries
    }

    private val base = load("values")
    private val folders = mapOf(LanguageCode.VA to "values-ca", LanguageCode.EN to "values-en", LanguageCode.FR to "values-fr")

    private fun placeholders(text: String) = Regex("%\\d\\$[sd]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `cada idioma traduce todas las claves y no sobra ninguna`() {
        val needed = base.filterValues { it.translatable }.keys
        folders.forEach { (language, folder) ->
            val translated = load(folder)
            assertEquals("$language: faltan", emptySet<String>(), needed - translated.keys)
            assertEquals("$language: sobran", emptySet<String>(), translated.keys - base.keys)
        }
    }

    @Test
    fun `las traducciones llevan los mismos marcadores que el original`() {
        folders.forEach { (language, folder) ->
            val translated = load(folder)
            base.filterValues { it.translatable }.forEach { (name, original) ->
                val other = translated.getValue(name)
                when (original) {
                    is Entry.Text -> {
                        other as Entry.Text
                        assertEquals("$language/$name", placeholders(original.value), placeholders(other.value))
                    }
                    is Entry.Plurals -> {
                        other as Entry.Plurals
                        assertTrue("$language/$name: le falta «one» u «other»", other.items.keys.containsAll(setOf("one", "other")))
                        original.items.forEach { (quantity, text) ->
                            assertEquals("$language/$name/$quantity", placeholders(text), placeholders(other.items.getValue(quantity)))
                        }
                    }
                    is Entry.Items -> {
                        other as Entry.Items
                        assertEquals("$language/$name: elementos", original.items.size, other.items.size)
                    }
                }
            }
        }
    }

    @Test
    fun `ninguna traduccion esta vacia ni lleva comillas rectas sin escapar`() {
        folders.forEach { (language, folder) ->
            load(folder).forEach { (name, entry) ->
                val texts = when (entry) {
                    is Entry.Text -> listOf(entry.value)
                    is Entry.Plurals -> entry.items.values.toList()
                    is Entry.Items -> entry.items
                }
                texts.forEach { text ->
                    assertTrue("$language/$name: vacío", text.isNotBlank())
                    // Android exige escapar el apóstrofo recto; la app usa el tipográfico (’).
                    assertTrue("$language/$name: apóstrofo recto", '\'' !in text.replace("\\'", ""))
                }
            }
        }
    }

    @Test
    fun `cada idioma dice cual es en locale_code`() {
        assertEquals("ES", (base.getValue("locale_code") as Entry.Text).value)
        folders.forEach { (language, folder) ->
            assertEquals(language.code, (load(folder).getValue("locale_code") as Entry.Text).value)
        }
    }

    @Test
    fun `los idiomas de LanguageCode son los de locales_config`() {
        val config = File("src/main/res/xml/locales_config.xml").readText(Charsets.UTF_8)
        val declared = Regex("android:name=\"([^\"]+)\"").findAll(config).map { it.groupValues[1] }.toList()
        assertEquals(LanguageCode.entries.map { it.tag }, declared)
    }

    @Test
    fun `la etiqueta de cada idioma se lee como el locale esperado`() {
        assertEquals(Locale("es"), LanguageCode.ES.locale)
        assertEquals("ca", LanguageCode.VA.locale.language)
        assertEquals("ES", LanguageCode.VA.locale.country)
        assertEquals("valencia", LanguageCode.VA.locale.variant.lowercase())
        assertEquals(Locale("en"), LanguageCode.EN.locale)
        assertEquals(Locale("fr"), LanguageCode.FR.locale)
        assertEquals(LanguageCode.VA, LanguageCode.fromCode(" va "))
        assertEquals(null, LanguageCode.fromCode("DE"))
        assertEquals(null, LanguageCode.fromCode(null))
    }
}
