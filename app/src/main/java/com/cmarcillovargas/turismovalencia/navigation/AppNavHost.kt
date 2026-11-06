package com.cmarcillovargas.turismovalencia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.ui.components.TabScope
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.BusquedaRoute
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.CategoriaRoute
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.DetalleRoute
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.EntradasRoute
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.InicioRoute
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.OfferTab
import com.cmarcillovargas.turismovalencia.ui.screens.explorar.SostenibleRoute
import com.cmarcillovargas.turismovalencia.ui.screens.favoritos.DescargasRoute
import com.cmarcillovargas.turismovalencia.ui.screens.favoritos.FavoritosRoute
import com.cmarcillovargas.turismovalencia.ui.screens.mapa.MapaRoute
import com.cmarcillovargas.turismovalencia.ui.screens.mapa.RutaRoute
import com.cmarcillovargas.turismovalencia.ui.screens.perfil.NotificacionesRoute
import com.cmarcillovargas.turismovalencia.ui.screens.perfil.PerfilRoute
import com.cmarcillovargas.turismovalencia.ui.screens.reservas.ReservaConfirmadaRoute
import com.cmarcillovargas.turismovalencia.ui.screens.reservas.ReservarRoute
import com.cmarcillovargas.turismovalencia.ui.screens.reservas.ReservasRoute
import com.cmarcillovargas.turismovalencia.ui.screens.valbot.ValBotRoute
import com.cmarcillovargas.turismovalencia.ui.screens.web.ContactoRoute
import com.cmarcillovargas.turismovalencia.ui.screens.web.WebPageRoute

private fun stringArg(name: String) = navArgument(name) {
    type = NavType.StringType
    defaultValue = ""
}

/** Una pantalla de partida de la barra inferior: sabe de qué pestaña es, para atender el aviso de pulsarla (`OnTabReselect`). */
private fun NavGraphBuilder.tabComposable(
    route: String,
    tab: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) = composable(route, arguments) { entry -> TabScope(tab) { content(entry) } }

/** Grafo de navegación de la app principal. */
@Composable
fun AppNavHost(
    navController: NavHostController,
    onTab: (String) -> Unit,
    onLogout: () -> Unit,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    fun open(route: String) = navController.navigate(route)
    val back: () -> Unit = { navController.popBackStack() }
    val openItem: (String) -> Unit = { open(Routes.detalle(it)) }

    NavHost(navController, startDestination = startDestination, modifier = modifier) {
        tabComposable(Routes.Inicio, Routes.Inicio) {
            InicioRoute(
                onOpenItem = openItem,
                onOpenSearch = { open(Routes.Busqueda) },
                onOpenNotifications = { open(Routes.Notificaciones) },
                onOpenCategory = { category ->
                    if (category == Category.Sostenible) open(Routes.Sostenible) else open(Routes.categoria(category.key))
                },
                onOpenTickets = { open(Routes.entradas()) },
                onOpenSustainable = { open(Routes.Sostenible) },
                onOpenPage = { open(Routes.pagina(it)) },
                onOpenContact = { open(Routes.Contacto) },
            )
        }
        tabComposable(Routes.MapaPattern, Routes.Mapa, arguments = listOf(stringArg("focus"), stringArg("q"))) { entry ->
            MapaRoute(
                focus = entry.arguments?.getString("focus")?.takeIf { it.isNotBlank() },
                initialQuery = entry.arguments?.getString("q")?.takeIf { it.isNotBlank() },
                onOpenItem = openItem,
                onOpenRoute = { open(Routes.ruta(it)) },
            )
        }
        tabComposable(Routes.ValbotPattern, Routes.Valbot, arguments = listOf(stringArg("q"))) { entry ->
            ValBotRoute(
                question = entry.arguments?.getString("q")?.takeIf { it.isNotBlank() },
                onBack = { onTab(Routes.Inicio) },
                onOpenItem = openItem,
                onOpenPage = { open(Routes.pagina(it)) },
                onOpenFavorites = { onTab(Routes.Favoritos) },
                onOpenMap = { onTab(Routes.Mapa) },
            )
        }
        tabComposable(Routes.Favoritos, Routes.Favoritos) {
            FavoritosRoute(
                onOpenItem = openItem,
                onOpenProfile = { open(Routes.Perfil) },
                onOpenDownloads = { open(Routes.Descargas) },
                onExplore = { onTab(Routes.Inicio) },
            )
        }
        tabComposable(Routes.Reservas, Routes.Reservas) {
            ReservasRoute(onOpenReservation = { open(Routes.reserva(it)) }, onOpenProfile = { open(Routes.Perfil) })
        }
        composable(Routes.Busqueda) {
            BusquedaRoute(
                onBack = back,
                onOpenItem = openItem,
                onOpenOffer = { open(Routes.reservar(it)) },
                onOpenMap = { open(Routes.mapa(query = it)) },
                onAskValBot = { open(Routes.valbot(it)) },
                onOpenGuide = { open(Routes.pagina(it)) },
            )
        }
        composable(Routes.CategoriaPattern, arguments = listOf(navArgument("key") { type = NavType.StringType })) { entry ->
            val category = Category.fromKey(entry.arguments?.getString("key"))
            CategoriaRoute(category = category, onBack = back, onOpenItem = openItem, onOpenMap = { onTab(Routes.Mapa) })
        }
        composable(Routes.DetallePattern, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            DetalleRoute(
                id = id,
                onBack = back,
                onOpenRoute = { open(Routes.ruta(it)) },
                onOpenMap = { open(Routes.mapa(focus = it)) },
                onOpenContact = { open(Routes.Contacto) },
            )
        }
        composable(Routes.EntradasPattern, arguments = listOf(stringArg("tab"))) { entry ->
            val tab = OfferTab.entries.firstOrNull { it.name.equals(entry.arguments?.getString("tab"), ignoreCase = true) } ?: OfferTab.Experiencias
            EntradasRoute(initialTab = tab, onBack = back, onReserve = { open(Routes.reservar(it)) }, onOpenPage = { open(Routes.pagina(it)) })
        }
        composable(Routes.Sostenible) { SostenibleRoute(onBack = back, onOpenItem = openItem) }
        composable(Routes.RutaPattern, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            RutaRoute(id = entry.arguments?.getString("id").orEmpty(), onBack = back)
        }
        composable(Routes.ReservarPattern, arguments = listOf(navArgument("offerId") { type = NavType.StringType })) { entry ->
            val offerId = entry.arguments?.getString("offerId").orEmpty()
            ReservarRoute(
                offerId = offerId,
                onBack = back,
                onConfirmed = { reservationId ->
                    navController.navigate(Routes.reserva(reservationId)) {
                        popUpTo(Routes.ReservarPattern) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ReservaPattern, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            ReservaConfirmadaRoute(
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = back,
                onOpenRoute = { open(Routes.ruta(it)) },
                onOpenMap = { onTab(Routes.Mapa) },
                onBookAgain = { open(Routes.reservar(it)) },
            )
        }
        composable(Routes.Descargas) { DescargasRoute(onBack = back, onOpenItem = openItem) }
        composable(Routes.Perfil) {
            PerfilRoute(
                onBack = back,
                onOpenDownloads = { open(Routes.Descargas) },
                onOpenContact = { open(Routes.Contacto) },
                onOpenPage = { open(Routes.pagina(it)) },
                onLogout = onLogout,
            )
        }
        composable(Routes.PaginaPattern, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            WebPageRoute(id = entry.arguments?.getString("id").orEmpty(), onBack = back, onOpenContact = { open(Routes.Contacto) })
        }
        composable(Routes.Contacto) { ContactoRoute(onBack = back, onOpenPage = { open(Routes.pagina(it)) }) }
        composable(Routes.Notificaciones) {
            NotificacionesRoute(
                onBack = back,
                onOpenSettings = { open(Routes.Perfil) },
                onOpenItem = openItem,
                onOpenReservations = { onTab(Routes.Reservas) },
                onOpenDownloads = { open(Routes.Descargas) },
                onOpenValBot = { onTab(Routes.Valbot) },
            )
        }
    }
}
