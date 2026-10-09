package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.cmarcillovargas.turismovalencia.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingPageUi(
    @param:DrawableRes val image: Int,
    @param:StringRes val imageDescription: Int,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
)

data class OnboardingUiState(
    val page: Int = 0,
    val pages: List<OnboardingPageUi>,
) {
    val isLastPage: Boolean get() = page == pages.lastIndex
}

// Contenido fijo del onboarding (datos fake: no vienen de ninguna API).
internal val OnboardingPages = listOf(
    OnboardingPageUi(
        image = R.drawable.onboarding_1,
        imageDescription = R.string.onboarding_1_imagen,
        title = R.string.onboarding_1_titulo,
        body = R.string.onboarding_1_texto,
    ),
    OnboardingPageUi(
        image = R.drawable.onboarding_2,
        imageDescription = R.string.onboarding_2_imagen,
        title = R.string.onboarding_2_titulo,
        body = R.string.onboarding_2_texto,
    ),
    OnboardingPageUi(
        image = R.drawable.onboarding_3,
        imageDescription = R.string.onboarding_3_imagen,
        title = R.string.onboarding_3_titulo,
        body = R.string.onboarding_3_texto,
    ),
)

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState(pages = OnboardingPages))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onPageChanged(page: Int) = _uiState.update { it.copy(page = page.coerceIn(0, it.pages.lastIndex)) }

    fun onNext() = onPageChanged(_uiState.value.page + 1)

    fun onPrevious() = onPageChanged(_uiState.value.page - 1)
}
