package com.cmarcillovargas.turismovalencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local: lo que el usuario guarda o cambia (favoritos, descargas, reservas, ajustes, búsquedas
 * recientes y avisos) para que siga ahí al volver a abrir la app. El contenido turístico no está aquí: sale de
 * los JSON de `assets/` hasta que llegue el backend.
 *
 * El esquema se exporta a `app/schemas/` en cada versión: al cambiarlo hay que subir `version` y añadir una migración.
 */
@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        ReservationEntity::class,
        SettingsEntity::class,
        RecentSearchEntity::class,
        NotificationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao

    abstract fun downloads(): DownloadDao

    abstract fun reservations(): ReservationDao

    abstract fun settings(): SettingsDao

    abstract fun recentSearches(): RecentSearchDao

    abstract fun notifications(): NotificationDao

    companion object {
        const val Name = "turismo.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, Name).build()
    }
}
