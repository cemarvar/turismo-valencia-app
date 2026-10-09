package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.Eyebrow
import com.cmarcillovargas.turismovalencia.ui.components.PageIndicator
import com.cmarcillovargas.turismovalencia.ui.components.SystemBarIcons
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme

// Medidas del diseño (A2–A4): la foto ocupa 470 de 844dp.
private const val PhotoFraction = 470f / 844f
private const val SkipAlpha = 0.5f

// Degradado negro sobre la parte alta de la foto para que se lean la barra de estado y «Saltar».
private const val TopShadeAlpha = 0.4f
private const val TopShadeEnd = 0.5f

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.page > 0, onBack = viewModel::onPrevious)
    OnboardingScreen(
        uiState = uiState,
        onPageChanged = viewModel::onPageChanged,
        onNext = { if (uiState.isLastPage) onFinished() else viewModel.onNext() },
        onSkip = onFinished,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPageChanged: (Int) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarIcons(lightStatusBarIcons = true, lightNavigationBarIcons = false)
    val pagerState = rememberPagerState(initialPage = uiState.page) { uiState.pages.size }
    // El estado manda (botón «Siguiente»); el gesto de deslizar lo notifica de vuelta.
    LaunchedEffect(uiState.page) {
        if (pagerState.currentPage != uiState.page) pagerState.animateScrollToPage(uiState.page)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(onPageChanged)
    }

    Box(modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            OnboardingPage(page = uiState.pages[index], index = index, count = uiState.pages.size)
        }
        SkipButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Spacing.m),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageIndicator(pageCount = uiState.pages.size, currentPage = uiState.page)
            AppButton(
                text = stringResource(if (uiState.isLastPage) R.string.onboarding_empezar else R.string.onboarding_siguiente),
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
            )
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPageUi, index: Int, count: Int) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(PhotoFraction)) {
            Image(
                painter = painterResource(page.image),
                contentDescription = stringResource(page.imageDescription),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to MaterialTheme.colorScheme.scrim.copy(alpha = TopShadeAlpha),
                                TopShadeEnd to Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        Column(Modifier.padding(start = Spacing.l, top = Spacing.xl, end = Spacing.l)) {
            Eyebrow(text = stringResource(R.string.onboarding_paso, index + 1, count))
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(page.title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(page.body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Píldora translúcida sobre la foto. La zona táctil de 48dp la garantiza Compose para cualquier clicable. */
@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.onboarding_saltar),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SkipAlpha))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
    )
}

@Preview(name = "A2 · Onboarding 1", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPage1Preview() = OnboardingPreview(page = 0)

@Preview(name = "A3 · Onboarding 2", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPage2Preview() = OnboardingPreview(page = 1)

@Preview(name = "A4 · Onboarding 3", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPage3Preview() = OnboardingPreview(page = 2)

@Composable
private fun OnboardingPreview(page: Int) {
    TurismoValenciaTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(page = page, pages = OnboardingPages),
            onPageChanged = {},
            onNext = {},
            onSkip = {},
        )
    }
}
