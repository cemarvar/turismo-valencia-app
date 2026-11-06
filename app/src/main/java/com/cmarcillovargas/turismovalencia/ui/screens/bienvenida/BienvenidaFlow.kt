package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val SplashMillis = 1_500L

private enum class Paso { Splash, Onboarding }

/**
 * Recorrido de bienvenida: splash → onboarding. Al terminar llama a `onFinished`. El permiso de ubicación no se pide aquí: se pide en
 * contexto, cuando hace falta (el botón «Centrar en mi ubicación» del mapa y «Cómo llegar»), y así el diálogo del sistema sale una sola vez.
 * Es un host en memoria sin la librería de navegación y arranca siempre desde el splash; que ya se haya hecho
 * se guarda aparte (`SettingsRepository.onboardingDone`) y `AppRoot` decide si se muestra.
 */
@Composable
fun BienvenidaFlow(onFinished: () -> Unit) {
    var paso by rememberSaveable { mutableStateOf(Paso.Splash) }

    when (paso) {
        Paso.Splash -> {
            SplashRoute()
            LaunchedEffect(Unit) {
                delay(SplashMillis)
                paso = Paso.Onboarding
            }
        }

        Paso.Onboarding -> OnboardingRoute(onFinished = onFinished)
    }
}
