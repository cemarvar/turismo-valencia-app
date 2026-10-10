package com.cmarcillovargas.turismovalencia.ui.screens.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WebPageUiState(
    val page: WebPage? = null,
    val loading: Boolean = true,
)

/** Una página de la web (lo esencial, planes, guías de entradas, textos legales…). */
class WebPageViewModel(container: AppContainer, id: String) : ViewModel() {
    private val _uiState = MutableStateFlow(WebPageUiState())
    val uiState: StateFlow<WebPageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.value = WebPageUiState(page = container.web.page(id), loading = false) }
    }
}
