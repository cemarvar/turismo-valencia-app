package com.cmarcillovargas.turismovalencia.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.ui.components.BottomNavBar
import com.cmarcillovargas.turismovalencia.ui.components.LocalTabReselect
import com.cmarcillovargas.turismovalencia.ui.components.NavBarItem
import com.cmarcillovargas.turismovalencia.ui.components.TabReselect

/**
 * App principal: grafo de navegación y barra inferior (solo en las pantallas que la llevan en el diseño). `openItem` es la
 * ficha que hay que abrir porque el usuario tocó un aviso: se abre encima de la pantalla en la que esté.
 */
@Composable
fun MainApp(onLogout: () -> Unit, startRoute: String? = null, openItem: String? = null, onOpenItemHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val reselect = remember { TabReselect() }
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(openItem) {
        if (openItem != null) {
            navController.navigate(Routes.detalle(openItem)) { launchSingleTop = true }
            onOpenItemHandled()
        }
    }
    val tab = Routes.tabOf(entry?.destination?.route)
    // Con el teclado abierto se oculta la barra inferior y el contenido sube sobre él.
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val items = listOf(
        NavBarItem(Routes.Inicio, stringResource(R.string.nav_inicio), Icons.Outlined.Home, Icons.Outlined.Home),
        NavBarItem(Routes.Mapa, stringResource(R.string.nav_mapa), Icons.Outlined.Map, Icons.Outlined.Map),
        NavBarItem(Routes.Valbot, stringResource(R.string.nav_valbot), Icons.Outlined.SmartToy, Icons.Outlined.SmartToy),
        NavBarItem(Routes.Favoritos, stringResource(R.string.nav_favoritos), Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
        NavBarItem(Routes.Reservas, stringResource(R.string.nav_reservas), Icons.Outlined.EventAvailable, Icons.Outlined.EventAvailable),
    )
    val navigateToTab: (String) -> Unit = { route ->
        if (route == Routes.Inicio) {
            // Inicio es el destino de partida y está siempre en el fondo de la pila: se vuelve a él sacando lo que haya encima, sin
            // guardar ni restaurar estado. Con `restoreState`, la navegación asocia al destino de partida lo último que se sacó de la
            // pila (una ficha y el mapa abierto desde ella, por ejemplo) y, al pulsar Inicio, lo volvía a poner: no dejaba salir del mapa.
            // Si ya se está en Inicio no se navega: volver a abrirlo (aunque sea `launchSingleTop`) recrea la pantalla y corta el desplazamiento
            // hacia arriba que se pide a continuación.
            if (navController.currentDestination?.route != Routes.Inicio && !navController.popBackStack(Routes.Inicio, inclusive = false)) {
                navController.navigate(Routes.Inicio) { launchSingleTop = true }
            }
        } else if (Routes.tabOf(navController.currentDestination?.route) != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        // Cada pulsación, también en la pestaña en la que ya se está, lleva al principio de la sección: las listas suben hasta arriba
        // del todo y el mapa vuelve a su vista de partida (lo atiende la pantalla de partida con `OnTabReselect`).
        reselect.fire(route)
    }

    CompositionLocalProvider(LocalTabReselect provides reselect) {
        Scaffold(
            bottomBar = {
                if (tab != null && !keyboardOpen) BottomNavBar(items = items, selectedKey = tab, onSelect = navigateToTab)
            },
            contentWindowInsets = WindowInsets(0),
        ) { padding ->
            AppNavHost(
                navController = navController,
                onTab = navigateToTab,
                onLogout = onLogout,
                startDestination = remember { startRoute ?: Routes.Inicio },
                modifier = Modifier.padding(padding).imePadding(),
            )
        }
    }
}
