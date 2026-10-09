package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.AppNotification
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.NotificationGroup
import com.cmarcillovargas.turismovalencia.data.model.NotificationKind
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// TODO(room): todo lo de este archivo vive en memoria y se pierde al cerrar la app.

/** Favoritos del usuario, en orden de guardado. */
class FavoritesRepository {
    private val _ids = MutableStateFlow(listOf("mercado-central", "albufera-barca", "fallas", "paella", "lonja-seda", "catedral-caliz"))
    val ids: StateFlow<List<String>> = _ids.asStateFlow()

    fun toggle(id: String) = _ids.update { if (id in it) it - id else it + id }

    fun clear() = _ids.update { emptyList() }
}

/** Fichas y mapa guardados para usar sin conexión. */
class DownloadsRepository(private val content: ContentRepository) {
    private val _ids = MutableStateFlow(listOf("mercado-central", "fallas", "paella", "lonja-seda"))
    val ids: StateFlow<List<String>> = _ids.asStateFlow()

    private val _mapDownloaded = MutableStateFlow(true)
    val mapDownloaded: StateFlow<Boolean> = _mapDownloaded.asStateFlow()

    private val _wifiOnly = MutableStateFlow(true)
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _autoUpdate = MutableStateFlow(true)
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()

    /** Megas que ocupan las fichas guardadas. */
    fun cardsMb(ids: List<String>): Int = ids.sumOf { content.item(it)?.offlineMb ?: 0 }

    fun toggle(id: String) = _ids.update { if (id in it) it - id else it + id }

    fun remove(id: String) = _ids.update { it - id }

    fun removeAll() = _ids.update { emptyList() }

    fun setMapDownloaded(value: Boolean) { _mapDownloaded.value = value }

    fun setWifiOnly(value: Boolean) { _wifiOnly.value = value }

    fun setAutoUpdate(value: Boolean) { _autoUpdate.value = value }

    companion object {
        /** Peso del mapa del centro guardado. */
        const val MAP_MB = 32
        const val QUOTA_MB = 500
    }
}

class ReservationsRepository(private val offers: ContentRepository) {
    private val today = LocalDate.now()
    private var counter = 412

    private val _items = MutableStateFlow(
        listOf(
            seed("seed-1", "albufera-barca", today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)), "18:30", 2, 0, ReservationStatus.Confirmed),
            seed("seed-2", "taller-paella", today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusDays(7), "13:00", 4, 0, ReservationStatus.Pending),
            seed("seed-3", "ruta-ciutat-vella", today.minusDays(5), "10:00", 1, 0, ReservationStatus.Completed),
        ),
    )
    val items: StateFlow<List<Reservation>> = _items.asStateFlow()

    fun get(id: String): Reservation? = _items.value.firstOrNull { it.id == id }

    fun create(offerId: String, date: LocalDate, time: String, adults: Int, children: Int, email: String): Reservation {
        val offer = checkNotNull(offers.offer(offerId))
        counter += 1
        val reservation = Reservation(
            id = "res-$counter",
            offerId = offerId,
            title = offer.title,
            place = offer.place,
            date = date,
            time = time,
            adults = adults,
            children = children,
            email = email,
            total = offer.priceFrom * adults,
            status = ReservationStatus.Confirmed,
            locator = "TV-${date.year}-${counter.toString().padStart(4, '0')}",
        )
        _items.update { listOf(reservation) + it }
        return reservation
    }

    private fun seed(id: String, offerId: String, date: LocalDate, time: String, adults: Int, children: Int, status: ReservationStatus): Reservation {
        val offer = checkNotNull(offers.offer(offerId))
        return Reservation(
            id = id,
            offerId = offerId,
            title = offer.title,
            place = offer.place,
            date = date,
            time = time,
            adults = adults,
            children = children,
            email = "laura.garcia@email.com",
            total = if (offerId == "taller-paella") 180 else offer.priceFrom * adults,
            status = status,
            locator = "TV-${date.year}-${(412 + id.last().digitToInt()).toString().padStart(4, '0')}",
        )
    }
}

/** Ajustes del usuario. */
class SettingsRepository {
    private val _language = MutableStateFlow(LanguageCode.ES)
    val language: StateFlow<LanguageCode> = _language.asStateFlow()

    private val _notifications = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _location = MutableStateFlow(LocationMode.WhileUsing)
    val location: StateFlow<LocationMode> = _location.asStateFlow()

    private val _textScale = MutableStateFlow(TextScale.System)
    val textScale: StateFlow<TextScale> = _textScale.asStateFlow()

    // TODO(i18n): aplicar el idioma al contenido y al locale de la app.
    fun setLanguage(value: LanguageCode) { _language.value = value }

    fun setNotificationsEnabled(value: Boolean) { _notifications.value = value }

    // TODO(permisos): cambiar el permiso real del sistema.
    fun setLocation(value: LocationMode) { _location.value = value }

    fun setTextScale(value: TextScale) { _textScale.value = value }
}

class NotificationsRepository {
    private val _items = MutableStateFlow(
        listOf(
            AppNotification("n1", NotificationKind.Event, NotificationGroup.Today, "Mascletà a las 14:00", "Plaza del Ayuntamiento · llega 30 min antes para coger sitio.", "Hace 10 min", true, "fallas"),
            AppNotification("n2", NotificationKind.Booking, NotificationGroup.Today, "Reserva confirmada", "Paseo en barca por la Albufera · sáb 12 oct, 18:30.", "Hace 1 h", true),
            AppNotification("n3", NotificationKind.Download, NotificationGroup.ThisWeek, "Fichas actualizadas", "4 lugares guardados se han actualizado para usarlos sin conexión.", "Ayer", false),
            AppNotification("n4", NotificationKind.Eco, NotificationGroup.ThisWeek, "Ruta sostenible recomendada", "Recorre el Jardín del Turia en Valenbisi: 9 km sin coches.", "Lunes", false, "jardin-turia"),
            AppNotification("n5", NotificationKind.Bot, NotificationGroup.ThisWeek, "ValBot tiene novedades", "Ahora responde también en valenciano e inglés.", "Domingo", false),
        ),
    )
    val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    fun markRead(id: String) = _items.update { list -> list.map { if (it.id == id) it.copy(unread = false) else it } }

    fun markAllRead() = _items.update { list -> list.map { it.copy(unread = false) } }
}

/** Búsquedas recientes. */
class SearchHistoryRepository {
    private val _recent = MutableStateFlow(listOf("Mercado Central", "Fallas 2027"))
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    fun add(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        _recent.update { (listOf(clean) + it.filterNot { old -> old.equals(clean, ignoreCase = true) }).take(5) }
    }

    fun clear() { _recent.value = emptyList() }
}
