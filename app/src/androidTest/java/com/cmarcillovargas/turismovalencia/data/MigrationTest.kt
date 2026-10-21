package com.cmarcillovargas.turismovalencia.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import com.cmarcillovargas.turismovalencia.data.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

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
    fun deLaV2ALaV3SeConservaTodoYSeAnadeLaCacheDeOfertas() {
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO favorites (position, itemId) VALUES (1, 'mercado-central')")
            execSQL("INSERT INTO reminders (itemId) VALUES ('fallas')")
            execSQL(
                "INSERT INTO reservations (id, offerId, title, place, epochDay, time, adults, children, email, total, status, number, createdAt) " +
                    "VALUES ('res-413', 'albufera-barca', 'Paseo en barca por la Albufera', 'El Palmar', 20000, '18:30', 2, 0, 'a@b.c', 36, 'Confirmed', 413, 1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 3, true, AppDatabase.Migration2To3)

        fun strings(sql: String) = migrated.query(sql).use { cursor -> generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList() }
        // Lo del usuario sigue ahí…
        assertEquals(listOf("mercado-central"), strings("SELECT itemId FROM favorites"))
        assertEquals(listOf("fallas"), strings("SELECT itemId FROM reminders"))
        assertEquals(listOf("res-413"), strings("SELECT id FROM reservations"))
        // …y la caché de ofertas empieza vacía y se puede escribir.
        assertEquals(emptyList<String>(), strings("SELECT id FROM offers"))
        assertEquals(emptyList<String>(), strings("SELECT name FROM cache_meta"))
        migrated.execSQL(
            "INSERT INTO offers (id, position, kind, tag, title, duration, place, extra, priceFromCents, description, placeId, slots) " +
                "VALUES ('albufera-barca', 0, 'Experiencia', 'Excursión', 'Paseo', '1 h', 'El Palmar', 'español', 1800, 'Texto', NULL, '[\"16:30\"]')",
        )
        migrated.execSQL("INSERT INTO cache_meta (name, etag, fetchedAt) VALUES ('offers', 'W/\"v1\"', 5)")
        assertEquals(listOf("albufera-barca"), strings("SELECT id FROM offers"))
        assertEquals(listOf("W/\"v1\""), strings("SELECT etag FROM cache_meta"))
        migrated.close()
    }

    @Test
    fun deLaV3ALaV4LasReservasPasanACentimosConSuLocalizadorYLlegaElIdentificadorDelMovil() {
        val feb = LocalDate.of(2027, 2, 11).toEpochDay()
        val dec = LocalDate.of(2026, 12, 31).toEpochDay()
        helper.createDatabase(name, 3).apply {
            execSQL(
                "INSERT INTO settings (id, language, notificationsEnabled, location, textScale, onboardingDone, wifiOnly, autoUpdate, mapDownloaded) " +
                    "VALUES (1, 'VA', 0, 'OnlyOnce', 'Large', 1, 0, 1, 0)",
            )
            execSQL(
                "INSERT INTO reservations (id, offerId, title, place, epochDay, time, adults, children, email, total, status, number, createdAt) VALUES " +
                    "('res-413', 'albufera-barca', 'Paseo en barca por la Albufera', 'El Palmar', $feb, '18:30', 2, 1, 'a@b.c', 36, 'Confirmed', 413, 100), " +
                    "('seed-2', 'taller-paella', 'Taller de paella valenciana', 'Mercado', $dec, '13:00', 4, 0, 'd@e.f', 180, 'Pending', 7, 90), " +
                    "('res-12345', 'albufera-barca', 'Paseo en barca por la Albufera', 'El Palmar', $feb, '16:30', 1, 0, 'g@h.i', 18, 'Completed', 12345, 80)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 4, true, AppDatabase.Migration3To4)

        fun rows(sql: String) = migrated.query(sql).use { c ->
            generateSequence { if (c.moveToNext()) (0 until c.columnCount).map { c.getString(it) } else null }.toList()
        }
        // El total pasa a céntimos, el localizador se guarda (año de la actividad y número de cuatro cifras como mínimo) y no se pierde nada más.
        assertEquals(
            listOf(
                listOf("res-12345", "1800", "TV-2027-12345", "Completed"),
                listOf("res-413", "3600", "TV-2027-0413", "Confirmed"),
                listOf("seed-2", "18000", "TV-2026-0007", "Pending"),
            ),
            rows("SELECT id, totalCents, locator, status FROM reservations ORDER BY id"),
        )
        assertEquals(listOf(listOf("Paseo en barca por la Albufera", "El Palmar", "18:30", "2", "1", "a@b.c", "100")), rows("SELECT title, place, time, adults, children, email, createdAt FROM reservations WHERE id = 'res-413'"))
        // Los ajustes siguen intactos y el identificador del móvil empieza vacío (se crea cuando hace falta).
        assertEquals(listOf(listOf("VA", "Large", "OnlyOnce", "1", null)), rows("SELECT language, textScale, location, onboardingDone, deviceId FROM settings"))
        // La tabla de intentos existe, está vacía y no admite dos intentos con los mismos datos.
        assertEquals(emptyList<List<String?>>(), rows("SELECT * FROM reservation_attempts"))
        val attempt = "INSERT INTO reservation_attempts (idempotencyKey, offerId, epochDay, time, adults, children, email, createdAt) VALUES (%s, 'albufera-barca', $feb, '18:30', 2, 0, 'a@b.c', 1)"
        migrated.execSQL(attempt.format("'k1'"))
        try {
            migrated.execSQL(attempt.format("'k2'"))
            fail("los mismos datos no pueden tener dos claves a la vez")
        } catch (_: SQLiteConstraintException) {
            // esperado
        }
        migrated.close()
    }

    @Test
    fun deLaV4ALaV5LlegaElModoDelTemaSinPerderNadaYEmpiezaEnElDelMovil() {
        helper.createDatabase(name, 4).apply {
            execSQL(
                "INSERT INTO settings (id, language, notificationsEnabled, location, textScale, onboardingDone, wifiOnly, autoUpdate, mapDownloaded, deviceId) " +
                    "VALUES (1, 'FR', 0, 'OnlyOnce', 'Large', 1, 0, 1, 0, 'dev-1')",
            )
            execSQL("INSERT INTO favorites (position, itemId) VALUES (1, 'paella')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 5, true, AppDatabase.Migration4To5)

        fun rows(sql: String) = migrated.query(sql).use { c ->
            generateSequence { if (c.moveToNext()) (0 until c.columnCount).map { c.getString(it) } else null }.toList()
        }
        // Lo que ya había sigue ahí y el modo nuevo empieza en «el del móvil», que es lo que la app hacía hasta ahora.
        assertEquals(
            listOf(listOf("FR", "Large", "OnlyOnce", "1", "dev-1", "System")),
            rows("SELECT language, textScale, location, onboardingDone, deviceId, themeMode FROM settings"),
        )
        assertEquals(listOf(listOf("paella")), rows("SELECT itemId FROM favorites"))
        // Y se puede guardar otro.
        migrated.execSQL("UPDATE settings SET themeMode = 'Dark'")
        assertEquals(listOf(listOf("Dark")), rows("SELECT themeMode FROM settings"))
        migrated.close()
    }

    @Test
    fun deLaV1ALaV5PasaPorLasCuatroMigraciones() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO favorites (position, itemId) VALUES (1, 'paella')")
            execSQL(
                "INSERT INTO reservations (id, offerId, title, place, epochDay, time, adults, children, email, total, status, number, createdAt) " +
                    "VALUES ('res-413', 'albufera-barca', 'Paseo', 'El Palmar', ${LocalDate.of(2026, 10, 18).toEpochDay()}, '18:30', 2, 0, 'a@b.c', 36, 'Confirmed', 413, 1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 5, true, *AppDatabase.Migrations)

        fun strings(sql: String) = migrated.query(sql).use { cursor -> generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList() }
        assertEquals(listOf("paella"), strings("SELECT itemId FROM favorites"))
        assertEquals(listOf("fallas"), strings("SELECT itemId FROM reminders"))
        assertEquals(emptyList<String>(), strings("SELECT id FROM offers"))
        assertEquals(listOf("TV-2026-0413"), strings("SELECT locator FROM reservations"))
        assertEquals(listOf("3600"), strings("SELECT totalCents FROM reservations"))
        assertEquals(emptyList<String>(), strings("SELECT themeMode FROM settings")) // esta base no tenía ajustes que migrar
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
            assertEquals(ThemeMode.System, settings.themeMode)
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
