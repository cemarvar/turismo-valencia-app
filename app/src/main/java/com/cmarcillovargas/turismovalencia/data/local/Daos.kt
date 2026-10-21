package com.cmarcillovargas.turismovalencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
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
    @Query("SELECT * FROM reservations ORDER BY createdAt DESC, locator DESC")
    abstract fun observeAll(): Flow<List<ReservationEntity>>

    /** Guarda la reserva o, si ya estaba (misma `id`), la pone al día con lo que dice el servidor. */
    @Upsert
    abstract suspend fun upsert(entity: ReservationEntity)

    @Upsert
    abstract suspend fun upsertAll(entities: List<ReservationEntity>)

    @Insert
    abstract suspend fun insertAll(entities: List<ReservationEntity>)

    @Query("UPDATE reservations SET status = :status WHERE id = :id")
    abstract suspend fun setStatus(id: String, status: ReservationStatus)

    @Query("DELETE FROM reservations")
    abstract suspend fun clear()
}

@Dao
abstract class ReservationAttemptDao {
    @Query(
        "SELECT * FROM reservation_attempts WHERE offerId = :offerId AND epochDay = :epochDay AND time = :time " +
            "AND adults = :adults AND children = :children AND email = :email",
    )
    abstract suspend fun find(offerId: String, epochDay: Long, time: String, adults: Int, children: Int, email: String): ReservationAttemptEntity?

    @Insert
    abstract suspend fun insert(entity: ReservationAttemptEntity)

    @Query("DELETE FROM reservation_attempts WHERE idempotencyKey = :idempotencyKey")
    abstract suspend fun delete(idempotencyKey: String)

    @Query("DELETE FROM reservation_attempts WHERE createdAt < :before")
    abstract suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM reservation_attempts")
    abstract suspend fun clear()

    /**
     * El intento pendiente con estos mismos datos o, si no hay, [attempt] guardado como nuevo. Los mismos datos reutilizan la
     * clave: es lo que evita duplicar la reserva al reintentar tras un corte de red.
     */
    @Transaction
    open suspend fun findOrStart(attempt: ReservationAttemptEntity): ReservationAttemptEntity =
        find(attempt.offerId, attempt.epochDay, attempt.time, attempt.adults, attempt.children, attempt.email)
            ?: attempt.also { insert(it) }
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

    /** El identificador de este móvil; si todavía no hay, lo crea con [generate] y lo guarda (de una vez: nunca salen dos distintos). */
    @Transaction
    open suspend fun deviceId(generate: () -> String): String {
        val settings = current() ?: SettingsEntity()
        settings.deviceId?.let { return it }
        val id = generate()
        save(settings.copy(deviceId = id))
        return id
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

/** Caché del catálogo de ofertas del servidor y la versión (`ETag`) con la que se guardó. */
@Dao
abstract class OfferDao {
    @Query("SELECT * FROM offers ORDER BY position")
    abstract fun observeAll(): Flow<List<OfferEntity>>

    @Query("SELECT COUNT(*) FROM offers")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<OfferEntity>)

    @Query("DELETE FROM offers")
    abstract suspend fun clear()

    @Query("SELECT * FROM cache_meta WHERE name = :name")
    abstract suspend fun meta(name: String): CacheMetaEntity?

    @Upsert
    abstract suspend fun saveMeta(entity: CacheMetaEntity)

    @Query("DELETE FROM cache_meta WHERE name = :name")
    abstract suspend fun deleteMeta(name: String)

    /** Sustituye el catálogo guardado por el recibido y apunta su versión. En una transacción: nunca se ve a medias. */
    @Transaction
    open suspend fun replace(entities: List<OfferEntity>, etag: String?, now: Long) {
        clear()
        insertAll(entities)
        saveMeta(CacheMetaEntity(OffersCache, etag, now))
    }

    /** El servidor confirmó (304) que la versión guardada sigue vigente. */
    @Query("UPDATE cache_meta SET fetchedAt = :now WHERE name = :name")
    abstract suspend fun touch(name: String, now: Long)

    /** Vacía la caché (y su versión), como si la app no hubiera recibido nunca el catálogo. */
    @Transaction
    open suspend fun clearCache() {
        clear()
        deleteMeta(OffersCache)
    }

    companion object {
        /** Nombre de esta caché en `cache_meta`. */
        const val OffersCache = "offers"
    }
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
