package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.local.DownloadDao
import com.cmarcillovargas.turismovalencia.data.local.FavoriteDao
import com.cmarcillovargas.turismovalencia.data.local.NotificationDao
import com.cmarcillovargas.turismovalencia.data.local.RecentSearchDao
import com.cmarcillovargas.turismovalencia.data.local.ReminderDao
import com.cmarcillovargas.turismovalencia.data.local.ReminderEntity
import com.cmarcillovargas.turismovalencia.data.local.ReservationDao
import com.cmarcillovargas.turismovalencia.data.local.ReservationEntity
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.local.asState
import com.cmarcillovargas.turismovalencia.data.local.toModel
import com.cmarcillovargas.turismovalencia.data.model.AppNotification
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import com.cmarcillovargas.turismovalencia.data.notify.NotificationAccess
import com.cmarcillovargas.turismovalencia.data.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// Lo del usuario vive en la base de datos local (Room, `data/local`). Cada repositorio expone el estado como
// `StateFlow` (ya cargado al crearlo) y las escrituras van a la base de datos: la interfaz se actualiza cuando
// la tabla cambia, así que no hay dos copias que puedan desincronizarse.

/** Favoritos del usuario, en orden de guardado. */
class FavoritesRepository(private val dao: FavoriteDao, private val scope: CoroutineScope) {
    val ids: StateFlow<List<String>> = dao.observeIds().asState(scope)

    fun toggle(id: String) {
        scope.launch { dao.toggle(id) }
    }

    fun clear() {
        scope.launch { dao.clear() }
    }
}

/** Fichas y mapa guardados para usar sin conexión. */
class DownloadsRepository(
    private val content: ContentRepository,
    private val dao: DownloadDao,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    val ids: StateFlow<List<String>> = dao.observeIds().asState(scope)

    val mapDownloaded: StateFlow<Boolean> = settings.select { it.mapDownloaded }

    val wifiOnly: StateFlow<Boolean> = settings.select { it.wifiOnly }

    val autoUpdate: StateFlow<Boolean> = settings.select { it.autoUpdate }

    /** Megas que ocupan las fichas guardadas. */
    fun cardsMb(ids: List<String>): Int = ids.sumOf { content.item(it)?.offlineMb ?: 0 }

    fun toggle(id: String) {
        scope.launch { dao.toggle(id) }
    }

    fun remove(id: String) {
        scope.launch { dao.delete(id) }
    }

    fun removeAll() {
        scope.launch { dao.clear() }
    }

    fun setMapDownloaded(value: Boolean) = settings.update { it.copy(mapDownloaded = value) }

    fun setWifiOnly(value: Boolean) = settings.update { it.copy(wifiOnly = value) }

    fun setAutoUpdate(value: Boolean) = settings.update { it.copy(autoUpdate = value) }

    companion object {
        /** Peso del mapa del centro guardado. */
        const val MAP_MB = 32
        const val QUOTA_MB = 500
    }
}

class ReservationsRepository(
    private val offers: ContentRepository,
    private val dao: ReservationDao,
    private val scope: CoroutineScope,
) {
    /** Las más recientes primero. */
    val items: StateFlow<List<Reservation>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }.asState(scope)

    fun get(id: String): Reservation? = items.value.firstOrNull { it.id == id }

    /**
     * Guarda la reserva con el siguiente número correlativo y devuelve cuando ya aparece en [items], de modo que la
     * pantalla de confirmación la encuentra. Se guarda aunque quien la pide se cancele (por ejemplo, si se cierra la pantalla).
     */
    // TODO(api): enviar la reserva al backend (tabla Reserva) y esperar su confirmación y el pago.
    suspend fun create(offerId: String, date: LocalDate, time: String, adults: Int, children: Int, email: String): Reservation =
        scope.async {
            val offer = checkNotNull(offers.offer(offerId))
            val saved = dao.insertNext { number ->
                ReservationEntity(
                    id = "res-$number",
                    offerId = offerId,
                    title = offer.title,
                    place = offer.place,
                    epochDay = date.toEpochDay(),
                    time = time,
                    adults = adults,
                    children = children,
                    email = email,
                    total = offer.priceFrom * adults,
                    status = ReservationStatus.Confirmed,
                    number = number,
                    createdAt = System.currentTimeMillis(),
                )
            }
            items.first { list -> list.any { it.id == saved.id } }
            saved.toModel()
        }.await()
}

/** Ajustes del usuario y estado de la app (si la bienvenida ya se hizo). */
class SettingsRepository(private val store: SettingsStore) {
    val language: StateFlow<LanguageCode> = store.select { it.language }

    val notificationsEnabled: StateFlow<Boolean> = store.select { it.notificationsEnabled }

    val location: StateFlow<LocationMode> = store.select { it.location }

    val textScale: StateFlow<TextScale> = store.select { it.textScale }

    /** La bienvenida (splash, onboarding y permiso) ya se hizo: no se repite al abrir la app. */
    val onboardingDone: StateFlow<Boolean> = store.select { it.onboardingDone }

    // TODO(i18n): aplicar el idioma al contenido y al locale de la app.
    fun setLanguage(value: LanguageCode) = store.update { it.copy(language = value) }

    fun setNotificationsEnabled(value: Boolean) = store.update { it.copy(notificationsEnabled = value) }

    // El permiso real del sistema no se puede quitar desde la app: las pantallas lo piden al elegir «Mientras se usa» y, si el
    // sistema ya no pregunta, llevan a Ajustes.
    fun setLocation(value: LocationMode) = store.update { it.copy(location = value) }

    fun setTextScale(value: TextScale) = store.update { it.copy(textScale = value) }

    fun setOnboardingDone(value: Boolean) = store.update { it.copy(onboardingDone = value) }
}

/**
 * Avisos de eventos que el usuario ha pedido (la mascletà de las Fallas) y su programación. Un aviso llega solo si el usuario
 * lo ha pedido, el ajuste general de Notificaciones está activo y el sistema deja notificar ([active]); cuando cualquiera de
 * las tres cosas cambia se programan o cancelan las alarmas.
 */
class RemindersRepository(
    private val dao: ReminderDao,
    settings: SettingsRepository,
    access: NotificationAccess,
    private val scheduler: ReminderScheduler,
    private val scope: CoroutineScope,
) {
    /** Eventos de los que el usuario quiere el aviso. */
    val ids: StateFlow<Set<String>> = dao.observeIds().map { it.toSet() }.asState(scope)

    /** Las notificaciones pueden llegar: el ajuste general está activo y el sistema las permite. */
    val active: StateFlow<Boolean> = combine(settings.notificationsEnabled, access.enabled) { enabled, allowed -> enabled && allowed }
        .stateIn(scope, SharingStarted.Eagerly, settings.notificationsEnabled.value && access.enabled.value)

    init {
        scope.launch {
            combine(ids, active) { wanted, active -> if (active) wanted else emptySet() }
                .distinctUntilChanged()
                .collect { scheduler.sync(it) }
        }
    }

    fun set(itemId: String, on: Boolean) {
        scope.launch { if (on) dao.insert(ReminderEntity(itemId)) else dao.delete(itemId) }
    }

    /** Programa ya, en el hilo que llama, los avisos que corresponden (al reiniciar el móvil no hay ninguna pantalla que lo haga). */
    fun syncNow() = scheduler.sync(if (active.value) ids.value else emptySet())
}

class NotificationsRepository(private val dao: NotificationDao, private val scope: CoroutineScope) {
    val items: StateFlow<List<AppNotification>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }.asState(scope)

    fun markRead(id: String) {
        scope.launch { dao.markRead(id) }
    }

    fun markAllRead() {
        scope.launch { dao.markAllRead() }
    }
}

/** Búsquedas recientes (las últimas 5, la más reciente primero). */
class SearchHistoryRepository(private val dao: RecentSearchDao, private val scope: CoroutineScope) {
    val recent: StateFlow<List<String>> = dao.observe().asState(scope)

    fun add(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        scope.launch { dao.add(clean, System.currentTimeMillis()) }
    }

    fun clear() {
        scope.launch { dao.clear() }
    }
}
