package com.cmarcillovargas.turismovalencia.data

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cmarcillovargas.turismovalencia.TurismoApp
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import com.cmarcillovargas.turismovalencia.data.repo.DownloadsRepository
import com.cmarcillovargas.turismovalencia.data.repo.FavoritesRepository
import com.cmarcillovargas.turismovalencia.data.repo.NetworkMonitor
import com.cmarcillovargas.turismovalencia.data.repo.NotificationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ReservationsRepository
import com.cmarcillovargas.turismovalencia.data.repo.SearchHistoryRepository
import com.cmarcillovargas.turismovalencia.data.repo.SettingsRepository
import com.cmarcillovargas.turismovalencia.data.repo.ValBotEngine
import com.cmarcillovargas.turismovalencia.data.repo.WebContentRepository

/** Dependencias de la app, creadas una vez en `TurismoApp`. */
class AppContainer(application: Application) {
    val web = WebContentRepository(application)
    val content = ContentRepository(web)
    val favorites = FavoritesRepository()
    val downloads = DownloadsRepository(content)
    val reservations = ReservationsRepository(content)
    val settings = SettingsRepository()
    val notifications = NotificationsRepository()
    val searchHistory = SearchHistoryRepository()
    val network = NetworkMonitor(application)
    val valbot = ValBotEngine(content, web)
}

/** Crea un `ViewModel` con acceso al `AppContainer`. */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { create(container()) } }

fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TurismoApp).container
