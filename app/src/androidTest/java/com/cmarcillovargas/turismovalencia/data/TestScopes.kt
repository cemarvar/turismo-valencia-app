package com.cmarcillovargas.turismovalencia.data

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking

/**
 * Ámbito de las pruebas de Room, como el de la app (una sola corrutina a la vez, en orden). Al cerrar una prueba se cierra la base de datos
 * mientras alguna consulta de un `Flow` puede seguir en vuelo: ese fallo no es de la prueba y, sin manejador, tumbaría el proceso de
 * pruebas entero (y con él las pruebas que faltan).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun testScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1) + CoroutineExceptionHandler { _, _ -> })

/** Cancela todo lo que corre en el ámbito y espera a que acabe, para poder cerrar la base de datos sin consultas en vuelo. */
fun CoroutineScope.closeAndJoin() {
    // El `Job` del ámbito se toma fuera de `runBlocking`: dentro, `coroutineContext` sería el de la propia corrutina que espera.
    val job = coroutineContext[Job] ?: return
    runBlocking { job.cancelAndJoin() }
}
