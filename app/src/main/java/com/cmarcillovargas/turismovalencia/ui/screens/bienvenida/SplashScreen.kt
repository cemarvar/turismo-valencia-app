package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.ui.components.PageIndicator
import com.cmarcillovargas.turismovalencia.ui.components.SystemBarIcons
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors

// Medidas del diseño (A1).
private val LogoCircleSize = 112.dp
private const val IndicatorPages = 3

// TODO(splash): integrar la SplashScreen API del sistema; esta pantalla es solo el composable fiel al diseño.
@Composable
fun SplashRoute(viewModel: SplashViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SplashScreen(uiState)
}

@Composable
fun SplashScreen(uiState: SplashUiState, modifier: Modifier = Modifier) {
    SystemBarIcons(lightStatusBarIcons = true, lightNavigationBarIcons = true)
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.appColors
    Column(modifier.fillMaxSize().background(app.action)) {
        Column(
            // El grupo del diseño queda algo por debajo del centro del hueco libre.
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = app.paper, shadowElevation = Spacing.m) {
                Image(
                    painter = painterResource(R.drawable.logo_turismo_valencia),
                    contentDescription = stringResource(R.string.splash_logo_descripcion),
                    modifier = Modifier.size(LogoCircleSize).padding(Spacing.m),
                )
            }
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = stringResource(R.string.splash_titulo),
                style = MaterialTheme.typography.headlineLarge,
                color = app.onAction,
            )
            Text(
                text = stringResource(R.string.splash_lema),
                style = MaterialTheme.typography.titleMedium,
                color = app.onAction,
            )
        }
        // El diseño deja 48dp bajo la versión; el indicador de gestos cae dentro de ese hueco.
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = maxOf(bottomInset, Spacing.xxl)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            PageIndicator(
                pageCount = IndicatorPages,
                currentPage = 0,
                activeColor = app.onAction,
                inactiveColor = app.onAction,
                stretchActive = false,
                fadeInactive = true,
            )
            Text(
                text = stringResource(R.string.splash_version, uiState.appVersion),
                style = MaterialTheme.typography.labelSmall,
                color = app.onAction,
            )
        }
    }
}

@Preview(name = "A1 · Splash", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SplashPreview() {
    TurismoValenciaTheme { SplashScreen(SplashUiState(appVersion = "1.0")) }
}
