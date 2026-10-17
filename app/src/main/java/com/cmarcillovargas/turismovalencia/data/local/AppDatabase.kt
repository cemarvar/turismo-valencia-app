package com.cmarcillovargas.turismovalencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local: lo que el usuario guarda o cambia (favoritos, descargas, reservas, ajustes, búsquedas
 * recientes, avisos y recordatorios de eventos) para que siga ahí al volver a abrir la app, y la caché del catálogo de
 * ofertas del servidor (`offers` y `cache_meta`; esa sí la manda el servidor y se puede borrar sin perder nada del usuario).
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
    ],
    version = 3,
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

        val Migrations = arrayOf(Migration1To2, Migration2To3)

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, Name)
                .addMigrations(*Migrations)
                .build()
    }
}
