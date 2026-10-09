package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val SplashMillis = 1_500L

private enum class Paso { Splash, Onboarding, Permiso }

/**
 * Recorrido de bienvenida: splash → onboarding → permiso de ubicación. Al terminar llama a `onFinished`.
 * Es un host en memoria sin la librería de navegación: no guarda nada y arranca siempre desde el splash.
 */
// TODO(room): recordar que el onboarding ya se vio para no repetirlo en cada arranque.
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

        Paso.Onboarding -> OnboardingRoute(onFinished = { paso = Paso.Permiso })

        Paso.Permiso -> {
            BackHandler { paso = Paso.Onboarding }
            PermisoUbicacionRoute(onContinue = onFinished)
        }
    }
}
