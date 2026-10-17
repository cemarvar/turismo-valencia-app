package com.cmarcillovargas.turismovalencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Las consultas que devuelven `Flow` avisan solas cuando cambia la tabla; las `suspend` no bloquean el hilo principal.
// Los DAO son clases abstractas porque llevan métodos `@Transaction` con cuerpo.

private const val RecentLimit = 5

@Dao
abstract class FavoriteDao {
    @Query("SELECT itemId FROM favorites ORDER BY position")
    abstract fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: FavoriteEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(entities: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE itemId = :itemId")
    abstract suspend fun delete(itemId: String): Int

    @Query("DELETE FROM favorites")
    abstract suspend fun clear()

    /** Quita el favorito si estaba y lo añade (al final) si no. */
    @Transaction
    open suspend fun toggle(itemId: String) {
        if (delete(itemId) == 0) insert(FavoriteEntity(itemId = itemId))
    }
}

@Dao
abstract class DownloadDao {
    @Query("SELECT itemId FROM downloads ORDER BY position")
    abstract fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(entities: List<DownloadEntity>)

    @Query("DELETE FROM downloads WHERE itemId = :itemId")
    abstract suspend fun delete(itemId: String): Int

    @Query("DELETE FROM downloads")
    abstract suspend fun clear()

    /** Quita la ficha guardada si estaba y la guarda (al final) si no. */
    @Transaction
    open suspend fun toggle(itemId: String) {
        if (delete(itemId) == 0) insert(DownloadEntity(itemId = itemId))
    }
}

@Dao
abstract class ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY createdAt DESC, number DESC")
    abstract fun observeAll(): Flow<List<ReservationEntity>>

    @Insert
    abstract suspend fun insert(entity: ReservationEntity)

    @Insert
    abstract suspend fun insertAll(entities: List<ReservationEntity>)

    /** Último número de reserva; los localizadores empiezan en el 413 si no hay ninguna. */
    @Query("SELECT COALESCE(MAX(number), 412) FROM reservations")
    abstract suspend fun lastNumber(): Int

    @Query("DELETE FROM reservations")
    abstract suspend fun clear()

    /** Guarda una reserva con el siguiente número correlativo y la devuelve. */
    @Transaction
    open suspend fun insertNext(build: (number: Int) -> ReservationEntity): ReservationEntity {
        val entity = build(lastNumber() + 1)
        insert(entity)
        return entity
    }
}

@Dao
abstract class SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    abstract fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    abstract suspend fun current(): SettingsEntity?

    @Upsert
    abstract suspend fun save(entity: SettingsEntity)

    /** Cambia los ajustes de forma atómica; si todavía no hay fila, parte de los valores por defecto. */
    @Transaction
    open suspend fun update(change: (SettingsEntity) -> SettingsEntity) {
        save(change(current() ?: SettingsEntity()))
    }
}

@Dao
abstract class RecentSearchDao {
    @Query("SELECT text FROM recent_searches ORDER BY searchedAt DESC, rowid DESC LIMIT $RecentLimit")
    abstract fun observe(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: RecentSearchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<RecentSearchEntity>)

    @Query("DELETE FROM recent_searches WHERE text NOT IN (SELECT text FROM recent_searches ORDER BY searchedAt DESC, rowid DESC LIMIT $RecentLimit)")
    abstract suspend fun trim()

    @Query("DELETE FROM recent_searches")
    abstract suspend fun clear()

    /**
     * Guarda la búsqueda (o la sube al principio si ya estaba) y deja solo las últimas [RecentLimit]. Si dos tienen la misma
     * hora, va primero la insertada después (`rowid`: REPLACE borra la fila antigua y crea otra).
     */
    @Transaction
    open suspend fun add(text: String, now: Long) {
        upsert(RecentSearchEntity(text, now))
        trim()
    }
}

@Dao
abstract class ReminderDao {
    @Query("SELECT itemId FROM reminders")
    abstract fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(entities: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE itemId = :itemId")
    abstract suspend fun delete(itemId: String)

    @Query("DELETE FROM reminders")
    abstract suspend fun clear()
}

@Dao
abstract class NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY position")
    abstract fun observeAll(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<NotificationEntity>)

    @Query("UPDATE notifications SET unread = 0 WHERE id = :id")
    abstract suspend fun markRead(id: String)

    @Query("UPDATE notifications SET unread = 0")
    abstract suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    abstract suspend fun clear()
}
