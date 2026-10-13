package com.cmarcillovargas.turismovalencia.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cmarcillovargas.turismovalencia.TurismoApp
import com.cmarcillovargas.turismovalencia.data.local.AppDatabase
import com.cmarcillovargas.turismovalencia.data.local.DemoData
import com.cmarcillovargas.turismovalencia.data.local.SettingsStore
import com.cmarcillovargas.turismovalencia.data.remote.ChatApi
import com.cmarcillovargas.turismovalencia.data.remote.OsrmApi
import com.cmarcillovargas.turismovalencia.data.remote.RouteService
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import com.cmarcillovargas.turismovalencia.data.repo.DownloadsRepository
import com.cmarcillovargas.turismovalencia.data.repo.FavoritesRepository
import com.cmarcillovargas.turismovalencia.data.repo.LocationRepository
import com.cmarcillovargas.turismovalencia.data.repo.NetworkMonitor
import com.cmarcillovargas.turismovalencia.data.repo.NotificationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.SearchHistoryRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ValBotEngine
import com.cmarcillovargas.turismovalencia.data.repo.ValBotRepository
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * Dependencias de la app, creadas una vez en `TurismoApp`: el contenido (assets), la base de datos local con lo del
 * usuario (Room) y los repositorios que las unen. `database` se puede cambiar para probar con una en memoria.
 */
class AppContainer(application: Application, private val database: AppDatabase = AppDatabase.create(application)) {
    /**
     * Ámbito de las escrituras y de los estados que siguen a Room: vive tanto como la app. La paralelización limitada a 1
     * mantiene las escrituras en el orden en que se piden; un fallo al guardar se registra en vez de cerrar la app.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1) +
            CoroutineExceptionHandler { _, error -> Log.e(Tag, "No se pudo guardar en la base de datos", error) },
    )

    val web = WebContentRepository(application)
    val content = ContentRepository(web)
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

    init {
        // Las distancias de las fichas se miden desde donde esté el usuario (o el centro de Valencia).
        scope.launch { location.origin.collect(content::updateOrigin) }
    }
    val notifications = NotificationsRepository(database.notifications(), scope)
    val searchHistory = SearchHistoryRepository(database.recentSearches(), scope)
    val network = NetworkMonitor(application)
    private val http = OkHttpClient()
    val valbot = ValBotRepository(ChatApi(ChatApi.defaultClient(http)), ValBotEngine(content, web))
    val routes: RouteService = OsrmApi(OsrmApi.defaultClient(http))

    /** Solo depuración (`--ez resetData true`): vuelve a los datos de demostración, como en el primer arranque. */
    fun resetToDemo() = runBlocking { demo.populate(database) }

    private companion object {
        const val Tag = "TurismoDb"
    }
}

/** Crea un `ViewModel` con acceso al `AppContainer`. */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { create(container()) } }

fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TurismoApp).container
