package com.cmarcillovargas.turismovalencia.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Las migraciones de Room conservan lo que el usuario ya tenía guardado. Cada prueba crea una base de datos con el esquema
 * antiguo (el exportado en `app/schemas/`), le mete datos, migra y comprueba que siguen ahí y que el esquema nuevo es el esperado.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test-${System.nanoTime()}.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun deLaV1ALaV2SeConservaTodoYSeAnadenLosAvisos() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO favorites (position, itemId) VALUES (1, 'mercado-central'), (2, 'paella')")
            execSQL("INSERT INTO downloads (position, itemId) VALUES (1, 'fallas')")
            execSQL(
                "INSERT INTO settings (id, language, notificationsEnabled, location, textScale, onboardingDone, wifiOnly, autoUpdate, mapDownloaded) " +
                    "VALUES (1, 'VA', 0, 'OnlyOnce', 'Large', 1, 0, 1, 0)",
            )
            execSQL("INSERT INTO recent_searches (text, searchedAt) VALUES ('Bioparc', 5)")
            execSQL(
                "INSERT INTO notifications (id, position, kind, period, title, body, whenLabel, unread, itemId) " +
                    "VALUES ('n1', 0, 'Event', 'Today', 'Mascletà a las 14:00', 'Texto', 'Hace 10 min', 1, 'fallas')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 2, true, AppDatabase.Migration1To2)

        fun strings(sql: String) = migrated.query(sql).use { cursor -> generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList() }
        assertEquals(listOf("mercado-central", "paella"), strings("SELECT itemId FROM favorites ORDER BY position"))
        assertEquals(listOf("fallas"), strings("SELECT itemId FROM downloads"))
        assertEquals(listOf("Bioparc"), strings("SELECT text FROM recent_searches"))
        assertEquals(listOf("Mascletà a las 14:00"), strings("SELECT title FROM notifications"))
        // El aviso de la mascletà salía activado en la v1 (sin guardarse): sigue activado.
        assertEquals(listOf("fallas"), strings("SELECT itemId FROM reminders"))
        migrated.close()
    }

    @Test
    fun laBaseMigradaSeAbreConRoomYLosAjustesSiguenIntactos() {
        helper.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO settings (id, language, notificationsEnabled, location, textScale, onboardingDone, wifiOnly, autoUpdate, mapDownloaded) " +
                    "VALUES (1, 'VA', 0, 'OnlyOnce', 'Large', 1, 0, 1, 0)",
            )
            close()
        }

        // Es lo que hace la app al arrancar tras una actualización: abrir el archivo antiguo con las migraciones.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(*AppDatabase.Migrations).build()
        try {
            val settings = runBlocking { db.settings().current() }!!
            assertEquals(LanguageCode.VA, settings.language)
            assertEquals(TextScale.Large, settings.textScale)
            assertEquals(LocationMode.OnlyOnce, settings.location)
            assertTrue(settings.onboardingDone)
            assertEquals(false, settings.notificationsEnabled)
            assertEquals(listOf("fallas"), runBlocking { db.reminders().observeIds().first() })
            // Y se puede escribir en la tabla nueva.
            runBlocking {
                db.reminders().delete("fallas")
                assertEquals(emptyList<String>(), db.reminders().observeIds().first())
            }
        } finally {
            db.close()
        }
    }
}
