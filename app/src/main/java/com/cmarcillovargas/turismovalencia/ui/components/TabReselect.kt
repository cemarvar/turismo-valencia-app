package com.cmarcillovargas.turismovalencia.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.filter

/**
 * Pulsar una pestaña de la barra inferior lleva siempre al principio de su sección, también si ya se estaba en ella: las listas suben
 * hasta arriba del todo y el mapa vuelve a su vista de partida. `MainApp` avisa con [fire] y la pantalla de partida de la pestaña lo
 * atiende con [OnTabReselect].
 *
 * El aviso queda pendiente hasta que la pantalla lo recoge, así que llega aunque ésta aún no esté en pantalla cuando se pulsa (al venir
 * de otra pestaña o de una pantalla de la misma, que se cierra antes).
 */
@Stable
class TabReselect {
    private val pending = mutableStateListOf<String>()

    fun fire(tab: String) {
        if (tab !in pending) pending.add(tab)
    }

    internal fun isPending(tab: String) = tab in pending

    internal fun consume(tab: String) {
        pending.remove(tab)
    }
}

val LocalTabReselect = staticCompositionLocalOf { TabReselect() }

/** La pestaña de la barra inferior a la que pertenece la pantalla (la pone `AppNavHost`); `null` en las que no son de una pestaña. */
val LocalTab = staticCompositionLocalOf<String?> { null }

@Composable
fun TabScope(tab: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTab provides tab, content = content)
}

/** Ejecuta [action] cada vez que el usuario pulsa la pestaña de esta pantalla (la que da [LocalTab]). Sin pestaña, no hace nada. */
@Composable
fun OnTabReselect(action: suspend () -> Unit) {
    val tab = LocalTab.current ?: return
    val reselect = LocalTabReselect.current
    val current by rememberUpdatedState(action)
    LaunchedEffect(reselect, tab) {
        snapshotFlow { reselect.isPending(tab) }.filter { it }.collect {
            reselect.consume(tab)
            current()
        }
    }
}
