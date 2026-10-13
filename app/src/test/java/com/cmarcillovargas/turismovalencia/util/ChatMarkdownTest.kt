package com.cmarcillovargas.turismovalencia.util

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMarkdownTest {
    @Test
    fun `texto plano queda igual`() {
        assertEquals("Hola, ¿qué tal?", parseChatMarkdown("Hola, ¿qué tal?").text)
    }

    @Test
    fun `negrita y cursiva se quitan y se marcan`() {
        val parsed = parseChatMarkdown("Visita **Mercado Central** y prueba la *paella*.")
        assertEquals("Visita Mercado Central y prueba la paella.", parsed.text)
        val bold = parsed.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("Mercado Central", parsed.text.substring(bold.start, bold.end))
        val italic = parsed.spanStyles.single { it.item.fontStyle == FontStyle.Italic }
        assertEquals("paella", parsed.text.substring(italic.start, italic.end))
    }

    @Test
    fun `listas con asterisco o guion pasan a vinetas`() {
        val parsed = parseChatMarkdown("Ideas:\n* **Lonja**: gótico\n- Catedral\n    * Miguelete")
        assertEquals("Ideas:\n• Lonja: gótico\n• Catedral\n    • Miguelete", parsed.text)
    }

    @Test
    fun `titulos van en negrita sin almohadillas`() {
        val parsed = parseChatMarkdown("## Dónde comer\nEn El Palmar.")
        assertEquals("Dónde comer\nEn El Palmar.", parsed.text)
        assertEquals(FontWeight.Bold, parsed.spanStyles.single().item.fontWeight)
    }

    @Test
    fun `enlaces y codigo dejan solo el texto`() {
        assertEquals("Mira la web y esto", parseChatMarkdown("Mira [la web](https://x.es) y `esto`").text)
    }

    @Test
    fun `asteriscos sueltos y multiplicaciones no se rompen`() {
        assertEquals("2 * 3 = 6 y un * suelto", parseChatMarkdown("2 * 3 = 6 y un * suelto").text)
    }
}
