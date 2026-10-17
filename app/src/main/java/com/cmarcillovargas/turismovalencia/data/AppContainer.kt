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
import com.cmarcillovargas.turismovalencia.data.notify.NotificationAccess
import com.cmarcillovargas.turismovalencia.data.notify.ReminderScheduler
import com.cmarcillovargas.turismovalencia.data.remote.ChatApi
import com.cmarcillovargas.turismovalencia.data.remote.OffersService
import com.cmarcillovargas.turismovalencia.data.remote.OsrmApi
import com.cmarcillovargas.turismovalencia.data.remote.RetrofitOffersService
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
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.SearchHistoryRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ValBotEngine
import com.cmarcillovargas.turismovalencia.data.repo.ValBotRepository
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * Dependencias de la app, creadas una vez en `TurismoApp`: el contenido (assets), el catálogo de ofertas del servidor, la
 * base de datos local con lo del usuario (Room) y los repositorios que las unen. `database` y `offersService` se pueden
 * cambiar para probar con una base en memoria y sin tocar la red.
 */
class AppContainer(
    application: Application,
    private val database: AppDatabase = AppDatabase.create(application),
    offersService: OffersService? = null,
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

    /** Catálogo de ofertas: el del servidor, guardado en Room (el incluido en la app mientras no haya llegado ninguno). */
    val offers = OffersRepository(
        database.offers(),
        offersService ?: RetrofitOffersService(TurismoApiClient.create(BuildConfig.API_BASE_URL, TurismoApiClient.defaultClient(http))),
        FakeOfertas,
        web::offerImage,
        scope,
    )
    val content = ContentRepository(web, offers.offers)
    private val demo = DemoData(content)

    init {
        // Primer arranque: datos de demostración. Después se lee lo guardado.
        runBlocking { demo.seedIfFirstRun(database) }
    }

    private val settingsStore = SettingsStore(database.settings(), scope)

    val favorites = FavoritesRepository(database.favorites(), scope)
    val downloads = DownloadsRepository(content, database.downloads(), settingsStore, scope)
    val reservations = ReservationsRepository(content, database.reservations(), scope)
    val settings = SettingsRepository(settingsStore)
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
        // El catálogo de ofertas se pide al abrir la app y cada vez que vuelve la conexión. Si no ha cambiado, el servidor responde
        // 304 sin cuerpo; si falla, la app sigue con lo guardado (o con el catálogo incluido en la app) y no se entera nadie.
        scope.launch { network.isOnline.filter { it }.collect { refreshOffers() } }
    }

    val valbot = ValBotRepository(ChatApi(ChatApi.defaultClient(http)), ValBotEngine(content, web))
    val routes: RouteService = OsrmApi(OsrmApi.defaultClient(http))

    /** Pide el catálogo de ofertas y anota el resultado en el registro; nunca lanza (la app sigue con lo que ya tiene). */
    private suspend fun refreshOffers() {
        try {
            when (val result = offers.refresh()) {
                is OffersRefresh.Updated -> Log.i(Tag, "Catálogo de ofertas actualizado: ${result.count} ofertas")
                OffersRefresh.NotModified -> Log.i(Tag, "Catálogo de ofertas sin cambios (304)")
                is OffersRefresh.Failed -> Log.w(Tag, "No se pudo actualizar el catálogo de ofertas", result.reason)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(Tag, "Error al guardar el catálogo de ofertas", e)
        }
    }

    /**
     * Solo depuración (`--ez resetData true`): vuelve a los datos de demostración, como en el primer arranque. Sin catálogo de
     * ofertas guardado, así que se vuelve a pedir al servidor si hay conexión (el arranque ya lo hizo antes de borrarlo).
     */
    fun resetToDemo() {
        runBlocking { demo.populate(database) }
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
