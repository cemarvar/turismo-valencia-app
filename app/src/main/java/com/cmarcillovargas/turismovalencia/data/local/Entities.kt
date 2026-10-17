package com.cmarcillovargas.turismovalencia.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.NotificationGroup
import com.cmarcillovargas.turismovalencia.data.model.NotificationKind
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.model.TextScale

// Tablas de la base de datos local (`turismo.db`). Las entidades solo usan tipos simples; el paso a los modelos
// de la app (fechas, localizador…) está en `Mappers.kt`.

/** Ficha guardada como favorita. `position` conserva el orden en que se guardaron. */
@Entity(tableName = "favorites", indices = [Index(value = ["itemId"], unique = true)])
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val itemId: String,
)

/** Ficha guardada para usar sin conexión. */
@Entity(tableName = "downloads", indices = [Index(value = ["itemId"], unique = true)])
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val itemId: String,
)

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey val id: String,
    val offerId: String,
    /** Título y lugar de la oferta en el momento de reservar. */
    val title: String,
    val place: String,
    /** Día de la actividad (`LocalDate.toEpochDay`). */
    val epochDay: Long,
    val time: String,
    val adults: Int,
    val children: Int,
    val email: String,
    val total: Int,
    val status: ReservationStatus,
    /** Número correlativo; el localizador es `TV-<año>-<número>`. */
    val number: Int,
    /** Cuándo se hizo la reserva (milisegundos); las más recientes salen primero. */
    val createdAt: Long,
)

/** Una única fila (`id = 1`) con los ajustes y el estado de la app. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val language: LanguageCode = LanguageCode.ES,
    val notificationsEnabled: Boolean = true,
    val location: LocationMode = LocationMode.WhileUsing,
    val textScale: TextScale = TextScale.System,
    /** La bienvenida (splash, onboarding y permiso) ya se hizo. */
    val onboardingDone: Boolean = false,
    val wifiOnly: Boolean = true,
    val autoUpdate: Boolean = true,
    val mapDownloaded: Boolean = true,
)

/** Búsqueda reciente; no distingue mayúsculas (volver a buscar «mercado central» actualiza la misma fila). */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey @ColumnInfo(collate = ColumnInfo.NOCASE) val text: String,
    val searchedAt: Long,
)

/** Evento del que el usuario quiere recibir el aviso (la mascletà de las Fallas). El calendario del aviso está en `data/notify`. */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val itemId: String,
)

/**
 * Oferta del catálogo del servidor guardada para usarla sin conexión (caché: el servidor manda). `position` conserva el
 * orden en que la API las sirve. El dinero va en céntimos, como en la API.
 */
@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val kind: OfferKind,
    val tag: String,
    val title: String,
    val duration: String,
    val place: String,
    val extra: String,
    val priceFromCents: Int,
    val description: String,
    val placeId: String?,
    /** Horarios como un array JSON de textos (`["16:30","17:30"]`). */
    val slots: String,
)

/** Datos de una caché del servidor (una fila por caché, `name`): la versión recibida (`ETag`) y cuándo se actualizó por última vez. */
@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val name: String,
    val etag: String?,
    /** Última vez que el servidor confirmó que esta es la versión vigente (milisegundos). */
    val fetchedAt: Long,
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    /** Orden en la lista. */
    val position: Int,
    val kind: NotificationKind,
    val period: NotificationGroup,
    val title: String,
    val body: String,
    val whenLabel: String,
    val unread: Boolean,
    val itemId: String?,
)
