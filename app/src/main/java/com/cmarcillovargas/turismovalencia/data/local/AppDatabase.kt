package com.cmarcillovargas.turismovalencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local: lo que el usuario guarda o cambia (favoritos, descargas, reservas, ajustes, búsquedas
 * recientes, avisos y recordatorios de eventos) para que siga ahí al volver a abrir la app (las reservas son las que confirmó
 * el servidor, y `reservation_attempts` guarda las que aún no han tenido respuesta), y la caché del catálogo de ofertas del
 * servidor (`offers` y `cache_meta`; esa sí la manda el servidor y se puede borrar sin perder nada del usuario).
 * El resto del contenido turístico no está aquí: sale de los JSON de `assets/`.
 *
 * El esquema se exporta a `app/schemas/` en cada versión: al cambiarlo hay que subir `version` y añadir una migración
 * (nunca se borra la base de datos: es lo que el usuario ha guardado).
 */
@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        ReservationEntity::class,
        SettingsEntity::class,
        RecentSearchEntity::class,
        NotificationEntity::class,
        ReminderEntity::class,
        OfferEntity::class,
        CacheMetaEntity::class,
        ReservationAttemptEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao

    abstract fun downloads(): DownloadDao

    abstract fun reservations(): ReservationDao

    abstract fun settings(): SettingsDao

    abstract fun recentSearches(): RecentSearchDao

    abstract fun notifications(): NotificationDao

    abstract fun reminders(): ReminderDao

    abstract fun offers(): OfferDao

    abstract fun reservationAttempts(): ReservationAttemptDao

    companion object {
        const val Name = "turismo.db"

        /**
         * v1 → v2: tabla de recordatorios de eventos. El aviso de la mascletà salía ya activado en la versión anterior
         * (aunque no se guardaba ni avisaba de nada), así que se conserva activado.
         */
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`itemId` TEXT NOT NULL, PRIMARY KEY(`itemId`))")
                db.execSQL("INSERT OR IGNORE INTO `reminders` (`itemId`) VALUES ('fallas')")
            }
        }

        /** v2 → v3: la caché del catálogo de ofertas del servidor (vacía: se rellena la primera vez que hay conexión). */
        val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `offers` (`id` TEXT NOT NULL, `position` INTEGER NOT NULL, `kind` TEXT NOT NULL, `tag` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `duration` TEXT NOT NULL, `place` TEXT NOT NULL, `extra` TEXT NOT NULL, `priceFromCents` INTEGER NOT NULL, " +
                        "`description` TEXT NOT NULL, `placeId` TEXT, `slots` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE TABLE IF NOT EXISTS `cache_meta` (`name` TEXT NOT NULL, `etag` TEXT, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`name`))")
            }
        }

        /**
         * v3 → v4: las reservas pasan a ser las del servidor. El total pasa a céntimos, el localizador se guarda (el que la app
         * derivaba del año de la actividad y del número) y desaparece `number`; los ajustes ganan el identificador del móvil
         * (se crea cuando hace falta) y aparece la tabla de intentos de reserva pendientes de respuesta.
         */
        val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reservations_new` (`id` TEXT NOT NULL, `offerId` TEXT NOT NULL, `title` TEXT NOT NULL, `place` TEXT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `time` TEXT NOT NULL, `adults` INTEGER NOT NULL, `children` INTEGER NOT NULL, `email` TEXT NOT NULL, " +
                        "`totalCents` INTEGER NOT NULL, `status` TEXT NOT NULL, `locator` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO `reservations_new` (`id`, `offerId`, `title`, `place`, `epochDay`, `time`, `adults`, `children`, `email`, `totalCents`, `status`, `locator`, `createdAt`) " +
                        "SELECT `id`, `offerId`, `title`, `place`, `epochDay`, `time`, `adults`, `children`, `email`, `total` * 100, `status`, " +
                        "'TV-' || strftime('%Y', `epochDay` * 86400, 'unixepoch') || '-' || printf('%04d', `number`), `createdAt` FROM `reservations`",
                )
                db.execSQL("DROP TABLE `reservations`")
                db.execSQL("ALTER TABLE `reservations_new` RENAME TO `reservations`")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `deviceId` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reservation_attempts` (`idempotencyKey` TEXT NOT NULL, `offerId` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                        "`time` TEXT NOT NULL, `adults` INTEGER NOT NULL, `children` INTEGER NOT NULL, `email` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`idempotencyKey`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_reservation_attempts_offerId_epochDay_time_adults_children_email` " +
                        "ON `reservation_attempts` (`offerId`, `epochDay`, `time`, `adults`, `children`, `email`)",
                )
            }
        }

        /** v4 → v5: el modo claro u oscuro de la app (Perfil → Tema). Quien ya tenía la app sigue con el del móvil, como hasta ahora. */
        val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `themeMode` TEXT NOT NULL DEFAULT 'System'")
            }
        }

        val Migrations = arrayOf(Migration1To2, Migration2To3, Migration3To4, Migration4To5)

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, Name)
                .addMigrations(*Migrations)
                .build()
    }
}
