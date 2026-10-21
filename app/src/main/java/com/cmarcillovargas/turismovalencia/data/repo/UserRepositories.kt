package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.LanguageApplier
import com.cmarcillovargas.turismovalencia.data.NoLanguageApplier
import com.cmarcillovargas.turismovalencia.data.NoThemeApplier
import com.cmarcillovargas.turismovalencia.data.ThemeApplier
import com.cmarcillovargas.turismovalencia.data.local.DownloadDao
import com.cmarcillovargas.turismovalencia.data.local.FavoriteDao
import com.cmarcillovargas.turismovalencia.data.local.NotificationDao
import com.cmarcillovargas.turismovalencia.data.local.RecentSearchDao
import com.cmarcillovargas.turismovalencia.data.local.ReminderDao
import com.cmarcillovargas.turismovalencia.data.local.ReminderEntity
import com.cmarcillovargas.turismovalencia.data.local.ReservationAttemptDao
import com.cmarcillovargas.turismovalencia.data.local.ReservationAttemptEntity
import com.cmarcillovargas.turismovalencia.data.local.ReservationDao
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.local.asState
import com.cmarcillovargas.turismovalencia.data.local.toModel
import com.cmarcillovargas.turismovalencia.data.model.AppNotification
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LocationMode
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.model.TextScale
import com.cmarcillovargas.turismovalencia.data.model.ThemeMode
import com.cmarcillovargas.turismovalencia.data.notify.NotificationAccess
import com.cmarcillovargas.turismovalencia.data.notify.ReminderScheduler
import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.ReservationRequest
import com.cmarcillovargas.turismovalencia.data.remote.ReservationsService
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
import java.util.UUID

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

/** Resultado de poner las reservas al día con el servidor. */
sealed interface ReservationsRefresh {
    data class Updated(val count: Int) : ReservationsRefresh

    /** Este móvil nunca ha reservado (no tiene identificador): no hay nada que pedir y no se manda nada al servidor. */
    data object NothingToAsk : ReservationsRefresh

    /** No se pudo (sin red o error del servidor): se sigue con las reservas guardadas. */
    data class Failed(val reason: Throwable) : ReservationsRefresh
}

/**
 * Reservas del usuario. Las crea el servidor (el precio y el localizador los pone él) y la app las guarda en Room: se ven sin
 * conexión, pero **reservar exige conexión**. Las tres de demostración del primer arranque solo existen en el móvil.
 *
 * Sin cuentas, el móvil se identifica con un UUID propio (`deviceId`, en los ajustes de Room). Cada intento de reservar lleva una
 * clave de idempotencia que se guarda en Room hasta tener una respuesta definitiva: si se corta la red antes de recibirla, al
 * reintentar con los mismos datos se manda la misma clave y el servidor devuelve la misma reserva en vez de crear otra (aunque la
 * app se haya cerrado entre medias).
 */
class ReservationsRepository(
    private val dao: ReservationDao,
    private val attempts: ReservationAttemptDao,
    private val settings: SettingsStore,
    private val service: ReservationsService,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val newKey: () -> String = { UUID.randomUUID().toString() },
) {
    /** Las más recientes primero. */
    val items: StateFlow<List<Reservation>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }.asState(scope)

    fun get(id: String): Reservation? = items.value.firstOrNull { it.id == id }

    /**
     * Reserva en el servidor y guarda lo que él confirma; devuelve cuando ya aparece en [items], de modo que la pantalla de
     * confirmación la encuentra. Lanza [ReservationFailure] si no se pudo. Sigue aunque quien la pide se cancele (por ejemplo, si
     * se cierra la pantalla): una reserva que el servidor ya confirmó no se pierde.
     */
    suspend fun create(offerId: String, date: LocalDate, time: String, adults: Int, children: Int, email: String): Reservation =
        scope.async {
            val deviceId = settings.deviceId()
            val attempt = attempts.findOrStart(ReservationAttemptEntity(newKey(), offerId, date.toEpochDay(), time, adults, children, email, now()))
            val request = ReservationRequest(offerId, date.toString(), time, adults, children, email)
            val created = try {
                service.create(deviceId, attempt.idempotencyKey, request)
            } catch (e: ApiException) {
                val failure = e.toReservationFailure()
                // Con una respuesta definitiva el intento se acaba; si no (sin red, 5xx, 429), se conserva la clave para reintentar.
                if (failure.reason.definitive) attempts.delete(attempt.idempotencyKey)
                throw failure
            }
            val saved = created.reservation.toEntity(now())
                ?: throw ReservationFailure(ReservationFailure.Reason.Server, cause = ApiException("El servidor devolvió una reserva que la app no entiende"))
            dao.upsert(saved)
            attempts.delete(attempt.idempotencyKey)
            items.first { list -> list.any { it.id == saved.id } }
            saved.toModel()
        }.await()

    /**
     * Cancela la reserva y guarda cómo queda; devuelve cuando ya aparece cancelada en [items]. Una reserva del servidor se cancela allí
     * (la hizo este móvil o se recuperó con su localizador y su correo) y sus plazas vuelven a estar libres; las de demostración solo
     * existen en el móvil y se cancelan aquí. Cancelar una ya cancelada no falla. Lanza [ReservationActionFailure] si no se pudo.
     * Sigue aunque quien la pide se vaya: una cancelación que el servidor ya hizo no se pierde.
     */
    suspend fun cancel(id: String): Reservation =
        scope.async {
            val current = get(id) ?: throw ReservationActionFailure(ReservationActionFailure.Reason.NotFound)
            val savedId = if (current.isDemo) {
                dao.setStatus(id, ReservationStatus.Cancelled)
                id
            } else {
                val updated = try {
                    service.cancel(settings.deviceId(), current.locator, current.email)
                } catch (e: ApiException) {
                    throw e.toActionFailure()
                }
                val saved = updated.toEntity(now()) ?: throw unreadableReservation()
                dao.upsert(saved)
                saved.id
            }
            items.first { list -> list.any { it.id == savedId && it.status == ReservationStatus.Cancelled } }.first { it.id == savedId }
        }.await()

    /**
     * Recupera una reserva con su localizador y el correo con el que se hizo (la de otro móvil, o la de antes de borrar los datos de la
     * app) y la guarda aquí; devuelve cuando ya aparece en [items]. Lanza [ReservationActionFailure]: `NotFound` si el localizador y
     * el correo no coinciden con ninguna (el servidor no dice cuál de los dos falla).
     */
    suspend fun recover(locator: String, email: String): Reservation =
        scope.async {
            val found = try {
                service.find(locator.trim().uppercase(), email.trim())
            } catch (e: ApiException) {
                throw e.toActionFailure()
            }
            val saved = found.toEntity(now()) ?: throw unreadableReservation()
            dao.upsert(saved)
            items.first { list -> list.any { it.id == saved.id } }.first { it.id == saved.id }
        }.await()

    private fun unreadableReservation() =
        ReservationActionFailure(ReservationActionFailure.Reason.Server, cause = ApiException("El servidor devolvió una reserva que la app no entiende"))

    /**
     * Pone las reservas de este móvil al día con el servidor (por ejemplo, la que se confirmó pero cuya respuesta se perdió). Un móvil
     * que nunca ha reservado no tiene identificador y no tiene nada que pedir: no se crea uno ni se manda nada al servidor.
     */
    suspend fun refresh(): ReservationsRefresh {
        attempts.deleteOlderThan(now() - AttemptLifetimeMillis)
        val deviceId = settings.deviceIdOrNull() ?: return ReservationsRefresh.NothingToAsk
        val listed = try {
            service.list(deviceId)
        } catch (e: ApiException) {
            return ReservationsRefresh.Failed(e)
        }
        val entities = listed.mapNotNull { it.toEntity(now()) }
        dao.upsertAll(entities)
        return ReservationsRefresh.Updated(entities.size)
    }

    private companion object {
        /** Un intento sin respuesta se olvida pasado un día (el servidor ya no lo daría por el mismo si cambió el aforo o la fecha). */
        const val AttemptLifetimeMillis = 24L * 60 * 60 * 1000
    }
}

/**
 * Ajustes del usuario y estado de la app (si la bienvenida ya se hizo). [languageApplier] es quien cambia de verdad el idioma de
 * los recursos al elegir uno y [themeApplier] quien pone el modo claro u oscuro; las pruebas les pasan [NoLanguageApplier] y
 * [NoThemeApplier] para no cambiar el idioma ni el modo del móvil en el que corren.
 */
class SettingsRepository(
    private val store: SettingsStore,
    private val languageApplier: LanguageApplier = NoLanguageApplier,
    private val themeApplier: ThemeApplier = NoThemeApplier,
) {
    /** Idioma de la app: el último que se eligió o con el que arrancó `MainActivity` (ver [syncLanguage]). */
    val language: StateFlow<LanguageCode> = store.select { it.language }

    val notificationsEnabled: StateFlow<Boolean> = store.select { it.notificationsEnabled }

    val location: StateFlow<LocationMode> = store.select { it.location }

    val textScale: StateFlow<TextScale> = store.select { it.textScale }

    /** El modo claro u oscuro elegido en Perfil → Tema (por defecto, el del móvil). */
    val themeMode: StateFlow<ThemeMode> = store.select { it.themeMode }

    /** La bienvenida (splash, onboarding y permiso) ya se hizo: no se repite al abrir la app. */
    val onboardingDone: StateFlow<Boolean> = store.select { it.onboardingDone }

    /** El usuario elige un idioma: se guarda y se le pide al sistema, que recrea la actividad con los recursos del idioma nuevo. */
    fun setLanguage(value: LanguageCode) {
        store.update { it.copy(language = value) }
        languageApplier.apply(value)
    }

    /**
     * Anota el idioma con el que Android ha arrancado la actividad (el elegido en la app, el que se eligió en los ajustes del móvil o,
     * la primera vez, el del móvil si es uno de los cuatro). No se lo pide al sistema: ya lo está usando.
     */
    fun syncLanguage(value: LanguageCode) {
        if (language.value != value) store.update { it.copy(language = value) }
    }

    fun setNotificationsEnabled(value: Boolean) = store.update { it.copy(notificationsEnabled = value) }

    // El permiso real del sistema no se puede quitar desde la app: las pantallas lo piden al elegir «Mientras se usa» y, si el
    // sistema ya no pregunta, llevan a Ajustes.
    fun setLocation(value: LocationMode) = store.update { it.copy(location = value) }

    fun setTextScale(value: TextScale) = store.update { it.copy(textScale = value) }

    /** El usuario elige el modo claro, el oscuro o el del móvil: se guarda y se le pide al sistema, que recrea la actividad con él. */
    fun setThemeMode(value: ThemeMode) {
        store.update { it.copy(themeMode = value) }
        themeApplier.apply(value)
    }

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
