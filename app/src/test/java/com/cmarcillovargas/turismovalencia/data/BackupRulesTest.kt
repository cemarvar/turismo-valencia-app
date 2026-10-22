package com.cmarcillovargas.turismovalencia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Qué copia Android de la app (Auto Backup). `backup_rules.xml` (Android 11 o menos) y `data_extraction_rules.xml` (Android 12 o más:
 * copia en la nube y traspaso de móvil a móvil) tienen que decir lo mismo, y lo que dicen tiene que ser lo que cuenta la política de
 * privacidad de la app (`AppPrivacy.kt`): se copia lo del usuario (la base de datos de Room y las preferencias del idioma y del tema) y
 * se deja fuera la caché del mapa y lo que es de este móvil. Sin `<include>`: «todo menos lo excluido», para que un archivo nuevo no se
 * quede fuera de la copia sin que nadie lo decida (y que quien lo añada piense si lleva datos personales o pesa).
 */
class BackupRulesTest {
    private fun root(path: String): Element {
        val file = File(path)
        assertTrue("Falta ${file.path}", file.isFile)
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun children(parent: Element, tag: String): List<Element> =
        (0 until parent.childNodes.length).map { parent.childNodes.item(it) }.filter { it.nodeType == Node.ELEMENT_NODE }.map { it as Element }.filter { it.tagName == tag }

    private fun excludes(parent: Element): Set<Pair<String, String>> = children(parent, "exclude").map { it.getAttribute("domain") to it.getAttribute("path") }.toSet()

    private val fullBackup = root("src/main/res/xml/backup_rules.xml")
    private val extraction = root("src/main/res/xml/data_extraction_rules.xml")
    private val cloud = children(extraction, "cloud-backup").single()
    private val transfer = children(extraction, "device-transfer").single()

    /** La caché de teselas de OpenStreetMap, la configuración de osmdroid (rutas de este móvil) y la marca de ProfileInstaller. */
    private val expected = setOf("file" to "osmdroid", "sharedpref" to "osmdroid.xml", "file" to "profileInstalled")

    @Test
    fun `la copia deja fuera la cache del mapa y lo que es de este movil`() {
        assertEquals(expected, excludes(fullBackup))
    }

    @Test
    fun `en Android 12 o mas la copia en la nube y el traspaso dejan fuera lo mismo que en Android 11 o menos`() {
        assertEquals(expected, excludes(cloud))
        assertEquals(expected, excludes(transfer))
    }

    @Test
    fun `se copia todo menos lo excluido y ninguna regla enumera lo que se copia`() {
        listOf(fullBackup, cloud, transfer).forEach { assertEquals(emptyList<Element>(), children(it, "include")) }
    }

    @Test
    fun `lo del usuario no se excluye`() {
        val all = excludes(fullBackup)
        assertFalse("la base de datos de Room se copia", all.any { it.first == "database" })
        // Las preferencias del idioma y del tema (`language.xml`, `theme.xml`) tampoco.
        assertEquals(setOf("osmdroid.xml"), all.filter { it.first == "sharedpref" }.map { it.second }.toSet())
    }

    @Test
    fun `la copia en la nube exige poder cifrarla de extremo a extremo`() {
        // Lleva el correo de las reservas y el identificador del móvil.
        assertEquals("true", cloud.getAttribute("disableIfNoEncryptionCapabilities"))
    }

    @Test
    fun `el manifiesto deja la copia activa y apunta a las dos reglas`() {
        val manifest = root("src/main/AndroidManifest.xml")
        val application = children(manifest, "application").single()
        assertEquals("true", application.getAttribute("android:allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttribute("android:dataExtractionRules"))
        assertEquals("@xml/backup_rules", application.getAttribute("android:fullBackupContent"))
    }
}
