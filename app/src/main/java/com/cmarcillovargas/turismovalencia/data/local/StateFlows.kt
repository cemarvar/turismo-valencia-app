package com.cmarcillovargas.turismovalencia.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * Convierte una consulta de Room en el estado que lee la interfaz. Lee el valor guardado al crearse (así la primera
 * pantalla ya sale con los datos y no parpadea vacía) y después sigue los cambios de la tabla.
 *
 * Bloquea el hilo que lo llama solo lo que tarda esa primera lectura; `TurismoApp` crea el contenedor en un hilo
 * aparte mientras arranca la actividad para que no se note.
 */
fun <T> Flow<T>.asState(scope: CoroutineScope): StateFlow<T> {
    val initial = runBlocking { first() }
    return stateIn(scope, SharingStarted.Eagerly, initial)
}
