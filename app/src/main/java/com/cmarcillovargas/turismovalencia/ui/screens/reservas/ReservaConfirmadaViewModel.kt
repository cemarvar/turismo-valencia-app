package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReservaConfirmadaUiState(
    val reservation: Reservation? = null,
    val offer: Offer? = null,
)

class ReservaConfirmadaViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    val uiState: StateFlow<ReservaConfirmadaUiState> = container.reservations.items.map { list ->
        val reservation = list.firstOrNull { it.id == id }
        ReservaConfirmadaUiState(reservation, reservation?.let { container.content.offer(it.offerId) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservaConfirmadaUiState(container.reservations.get(id)))
}
