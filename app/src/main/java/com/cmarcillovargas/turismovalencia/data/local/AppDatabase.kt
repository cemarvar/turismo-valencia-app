package com.cmarcillovargas.turismovalencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local: lo que el usuario guarda o cambia (favoritos, descargas, reservas, ajustes, búsquedas
 * recientes, avisos y recordatorios de eventos) para que siga ahí al volver a abrir la app. El contenido turístico no
 * está aquí: sale de los JSON de `assets/` hasta que llegue el backend.
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
    ],
    version = 2,
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

        val Migrations = arrayOf(Migration1To2)

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, Name)
                .addMigrations(*Migrations)
                .build()
    }
}
