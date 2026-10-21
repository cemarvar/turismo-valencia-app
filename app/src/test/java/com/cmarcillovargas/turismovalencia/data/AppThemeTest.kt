package com.cmarcillovargas.turismovalencia.data

import androidx.appcompat.app.AppCompatDelegate
import com.cmarcillovargas.turismovalencia.data.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** El modo claro u oscuro de la app: de dónde se lee y qué se le pide a AppCompat. */
class AppThemeTest {
    @Test
    fun `cada modo se traduce a su valor de AppCompat`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.System.nightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.Light.nightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.Dark.nightMode())
        assertEquals("falta cubrir algún modo en la prueba", 3, ThemeMode.entries.size)
    }

    @Test
    fun `el modo guardado se lee por su nombre y lo que no se conoce es el del movil`() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromName(it.name)) }
        assertEquals(ThemeMode.System, ThemeMode.fromName(null))
        assertEquals(ThemeMode.System, ThemeMode.fromName(""))
        assertEquals(ThemeMode.System, ThemeMode.fromName("dark"))
        assertEquals(ThemeMode.System, ThemeMode.fromName("Sepia"))
    }

    @Test
    fun `el modo por defecto es el del movil`() {
        assertEquals(ThemeMode.System, com.cmarcillovargas.turismovalencia.data.local.SettingsEntity().themeMode)
    }
}
