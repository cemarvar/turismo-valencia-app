package com.cmarcillovargas.turismovalencia.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cmarcillovargas.turismovalencia.BuildConfig
import com.cmarcillovargas.turismovalencia.TurismoApp
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.DemoData
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.ThemeMode
import com.cmarcillovargas.turismovalencia.data.notify.NotificationAccess
import com.cmarcillovargas.turismovalencia.data.notify.ReminderScheduler
import com.cmarcillovargas.turismovalencia.data.remote.ChatApi
import com.cmarcillovargas.turismovalencia.data.remote.OffersService
import com.cmarcillovargas.turismovalencia.data.remote.OsrmApi
import com.cmarcillovargas.turismovalencia.data.remote.ReservationsService
import com.cmarcillovargas.turismovalencia.data.remote.RetrofitOffersService
import com.cmarcillovargas.turismovalencia.data.remote.RetrofitReservationsService
import com.cmarcillovargas.turismovalencia.data.remote.RouteService
import com.cmarcillovargas.turismovalencia.data.remote.TurismoApiClient
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import com.cmarcillovargas.turismovalencia.data.repo.DownloadsRepository
import com.cmarcillovargas.turismovalencia.data.repo.FavoritesRepository
import com.cmarcillovargas.turismovalencia.data.repo.LocationRepository
import com.cmarcillovargas.turismovalencia.data.repo.NetworkMonitor
import com.cmarcillovargas.turismovalencia.data.repo.NotificationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.OffersRefresh
import com.cmarcillovargas.turismovalencia.data.repo.OffersRepository
import com.cmarcillovargas.turismovalencia.data.repo.RemindersRepository
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRefresh
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.SearchHistoryRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ValBotEngine
import com.cmarcillovargas.turismovalencia.data.repo.ValBotRepository
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository
import com.cmarcillovargas.turismovalencia.data.work.BackgroundRefresher
import com.cmarcillovargas.turismovalencia.data.work.RefreshPlan
import com.cmarcillovargas.turismovalencia.data.work.RefreshScheduler
import com.cmarcillovargas.turismovalencia.data.work.WorkRefreshScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.Locale

/**
 * Dependencias de la app, creadas una vez en `TurismoApp`: el contenido (assets), el catálogo de ofertas y las reservas, que
 * pasan por la API propia, la base de datos local con lo del usuario (Room) y los repositorios que las unen. `database`, los
 * servicios de la API y quien programa el refresco nocturno se pueden cambiar para probar con una base en memoria y sin tocar la
 * red ni WorkManager.
 */
class AppContainer(
    private val application: Application,
    private val database: AppDatabase = AppDatabase.create(application),
    offersService: OffersService? = null,
    reservationsService: ReservationsService? = null,
    refreshScheduler: RefreshScheduler = WorkRefreshScheduler(application),
    languageApplier: LanguageApplier = AppCompatLanguageApplier(),
    private val themeApplier: ThemeApplier = AppCompatThemeApplier(application),
) {
    /**
     * Ámbito de las escrituras y de los estados que siguen a Room: vive tanto como la app. La paralelización limitada a 1
     * mantiene las escrituras en el orden en que se piden; un fallo al guardar se registra en vez de cerrar la app.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1) +
            CoroutineExceptionHandler { _, error -> Log.e(Tag, "No se pudo guardar en la base de datos", error) },
    )

    private val http = OkHttpClient()
    val web = WebContentRepository(application)

    /** Textos en el idioma de la app para quien no tiene una pantalla detrás (ValBot, buscador, avisos). */
    val strings = AppStrings(application)

    /** La API propia (ofertas y reservas); solo se crea si alguno de los dos servicios no viene dado. */
    private val api by lazy { TurismoApiClient.create(BuildConfig.API_BASE_URL, TurismoApiClient.defaultClient(http)) }

    /** Catálogo de ofertas: el del servidor, guardado en Room (el incluido en la app mientras no haya llegado ninguno). */
    val offers = OffersRepository(database.offers(), offersService ?: RetrofitOffersService(api), FakeOfertas, web::offerImage, scope)
    val content = ContentRepository(web, offers.offers, strings)
    private val demo = DemoData(content)

    init {
        // Primer arranque: datos de demostración. Después se lee lo guardado.
        runBlocking { demo.seedIfFirstRun(database) }
    }

    private val settingsStore = SettingsStore(database.settings(), scope)

    val favorites = FavoritesRepository(database.favorites(), scope)
    val downloads = DownloadsRepository(content, database.downloads(), settingsStore, scope)
    val reservations = ReservationsRepository(
        database.reservations(),
        database.reservationAttempts(),
        settingsStore,
        reservationsService ?: RetrofitReservationsService(api),
        scope,
    )
    val settings = SettingsRepository(settingsStore, languageApplier, themeApplier)
    val location = LocationRepository(application, settings, scope)

    /** Si el sistema deja mostrar notificaciones; `MainActivity` lo vuelve a mirar al volver a la app. */
    val notificationAccess = NotificationAccess.of(application)
    val reminders = RemindersRepository(database.reminders(), settings, notificationAccess, ReminderScheduler(application), scope)

    init {
        // Las distancias de las fichas se miden desde donde esté el usuario (o el centro de Valencia).
        scope.launch { location.origin.collect(content::updateOrigin) }
    }
    val notifications = NotificationsRepository(database.notifications(), scope)
    val searchHistory = SearchHistoryRepository(database.recentSearches(), scope)
    val network = NetworkMonitor(application)

    init {
        // El catálogo de ofertas y las reservas se ponen al día al abrir la app y cada vez que vuelve la conexión, sin depender de los
        // ajustes de Descargas (es una petición pequeña: si el catálogo no ha cambiado, el servidor responde 304 sin cuerpo; y sin ella,
        // con «Actualizar automáticamente» apagado no se pondría al día nunca). Si algo falla, la app sigue con lo guardado (o con el
        // catálogo incluido en la app) y no se entera nadie.
        scope.launch {
            network.isOnline.filter { it }.collect { refreshAll() }
        }
        // Lo mismo, cada noche y con la app cerrada (WorkManager), si «Actualizar automáticamente» está activado; «Descargar solo con
        // Wi-Fi» decide si basta con datos móviles. Se vuelve a programar cada vez que cambia uno de los dos ajustes.
        scope.launch {
            combine(downloads.autoUpdate, downloads.wifiOnly) { auto, wifiOnly -> RefreshPlan(enabled = auto, unmeteredOnly = wifiOnly) }
                .distinctUntilChanged()
                .collect { plan ->
                    try {
                        refreshScheduler.apply(plan)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(Tag, "No se pudo programar el refresco nocturno", e)
                    }
                }
        }
    }

    val valbot = ValBotRepository(ChatApi(ChatApi.defaultClient(http)), ValBotEngine(content, web, strings), language = { strings.language })
    val routes: RouteService = OsrmApi(OsrmApi.defaultClient(http))

    /** Lo que ejecuta el trabajo nocturno de WorkManager (`TurismoApp` lo pasa a su `WorkerFactory`). */
    val refresher = BackgroundRefresher { refreshAll() }

    /** Pone al día el catálogo y las reservas; devuelve si las dos quedaron al día (nunca lanza). */
    private suspend fun refreshAll(): Boolean {
        val offersOk = refreshOffers()
        val reservationsOk = refreshReservations()
        return offersOk && reservationsOk
    }

    /** Pide el catálogo de ofertas y anota el resultado en el registro; nunca lanza (la app sigue con lo que ya tiene). */
    private suspend fun refreshOffers(): Boolean {
        try {
            return when (val result = offers.refresh()) {
                is OffersRefresh.Updated -> true.also { Log.i(Tag, "Catálogo de ofertas actualizado: ${result.count} ofertas") }
                OffersRefresh.NotModified -> true.also { Log.i(Tag, "Catálogo de ofertas sin cambios (304)") }
                is OffersRefresh.Failed -> false.also { Log.w(Tag, "No se pudo actualizar el catálogo de ofertas", result.reason) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(Tag, "Error al guardar el catálogo de ofertas", e)
            return false
        }
    }

    /** Pone las reservas al día con el servidor y anota el resultado en el registro; nunca lanza. */
    private suspend fun refreshReservations(): Boolean {
        try {
            return when (val result = reservations.refresh()) {
                is ReservationsRefresh.Updated -> true.also { Log.i(Tag, "Reservas al día con el servidor: ${result.count}") }
                ReservationsRefresh.NothingToAsk -> true.also { Log.i(Tag, "Reservas: este móvil nunca ha reservado, no se pide nada") }
                is ReservationsRefresh.Failed -> false.also { Log.w(Tag, "No se pudieron actualizar las reservas", result.reason) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(Tag, "Error al guardar las reservas", e)
            return false
        }
    }

    init {
        // El modo claro u oscuro lo pone `TurismoApp` al arrancar con la copia rápida de `AppThemePrefs`, antes de que exista ninguna
        // actividad; esto lo da por bueno con lo que hay en Room (si coinciden no hace nada) y lo corrige si no (una copia de seguridad
        // restaurada a medias, por ejemplo).
        themeApplier.apply(settings.themeMode.value)
    }

    init {
        // Hasta que arranque una actividad (que sabe con qué idioma ha resuelto Android los recursos), vale el último que se guardó: es
        // el que necesitan, por ejemplo, la alarma de un aviso con la app cerrada.
        applyLanguage(settings.language.value)
    }

    /**
     * Cambia el idioma de todo lo que no son recursos de pantalla: los textos de [strings], el contenido traducido y el formato de
     * fechas y números. `MainActivity` lo llama al crearse con el idioma que dieron los recursos, y eso también lo anota en Room.
     */
    fun applyLanguage(language: LanguageCode) {
        Locale.setDefault(language.locale)
        strings.language = language
        content.setLanguage(language)
        AppLanguagePrefs.write(application, language)
        settings.syncLanguage(language)
    }

    /**
     * «Cerrar sesión». Sin cuentas, es borrar lo del usuario de este móvil y dejar la app **vacía**: sin favoritos, descargas,
     * reservas, avisos ni búsquedas (no vuelve la demostración del primer arranque), con los ajustes de fábrica (también el modo
     * de pantalla del móvil y el identificador del móvil, que se crea de nuevo si hace falta) y la bienvenida otra vez; solo se
     * conserva el idioma. Los avisos de eventos se cancelan solos (siguen a su tabla) y el refresco nocturno vuelve a sus ajustes
     * de fábrica. Las reservas ya hechas siguen en el servidor: se recuperan con su localizador y su correo. La escritura va por el
     * ámbito de los repositorios, así que queda detrás de lo que estuviera pendiente.
     */
    suspend fun signOut() {
        scope.async { demo.clearUserData(database, language = strings.language) }.await()
        themeApplier.apply(ThemeMode.System)
        if (network.isOnline.value) scope.launch { refreshOffers() }
    }

    /**
     * Solo depuración (`--ez resetData true`): vuelve a los datos de demostración, como en el primer arranque. Sin catálogo de
     * ofertas guardado, así que se vuelve a pedir al servidor si hay conexión (el arranque ya lo hizo antes de borrarlo).
     */
    fun resetToDemo() {
        runBlocking { demo.populate(database, language = strings.language) }
        themeApplier.apply(ThemeMode.System)
        if (network.isOnline.value) scope.launch { refreshOffers() }
    }

    private companion object {
        const val Tag = "TurismoDb"
    }
}

/** Crea un `ViewModel` con acceso al `AppContainer`. */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { create(container()) } }

fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TurismoApp).container
