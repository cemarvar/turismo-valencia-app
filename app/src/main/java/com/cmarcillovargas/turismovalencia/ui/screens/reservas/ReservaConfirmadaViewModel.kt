package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.repo.ReservationActionFailure
import com.cmarcillovargas.turismovalencia.data.repo.canCancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReservaConfirmadaUiState(
    val reservation: Reservation? = null,
    val offer: Offer? = null,
    /** Se ofrece «Cancelar reserva»: no está cancelada ni completada y la actividad no ha empezado (ni el servidor ha dicho que es tarde). */
    val canCancel: Boolean = false,
    /** Hay conexión: cancelar una reserva del servidor la exige (las de demostración se cancelan solo en el móvil). */
    val online: Boolean = true,
    /** La cancelación se está enviando al servidor. */
    val cancelling: Boolean = false,
    /** Por qué falló el último intento de cancelar; se borra al volver a intentarlo. */
    val cancelFailure: ReservationActionFailure? = null,
) {
    /** La cancelación puede salir ya: hay conexión o la reserva no depende del servidor. */
    val canCancelNow: Boolean get() = canCancel && !cancelling && (online || reservation?.isDemo == true)
}

class ReservaConfirmadaViewModel(private val container: AppContainer, private val id: String) : ViewModel() {
    private val cancelling = MutableStateFlow(false)
    private val failure = MutableStateFlow<ReservationActionFailure?>(null)

    private fun stateOf(reservation: Reservation?, online: Boolean, busy: Boolean, fail: ReservationActionFailure?): ReservaConfirmadaUiState {
        val offer = reservation?.let { container.content.offer(it.offerId) }
        return ReservaConfirmadaUiState(
            reservation = reservation?.shownAs(offer),
            offer = offer,
            canCancel = reservation?.canCancel() == true && fail?.reason != ReservationActionFailure.Reason.TooLate,
            online = online,
            cancelling = busy,
            cancelFailure = fail,
        )
    }

    val uiState: StateFlow<ReservaConfirmadaUiState> = combine(container.reservations.items, container.network.isOnline, cancelling, failure) { list, online, busy, fail ->
        stateOf(list.firstOrNull { it.id == id }, online, busy, fail)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        stateOf(container.reservations.get(id), container.network.isOnline.value, false, null),
    )

    /**
     * Cancela la reserva (en el servidor, que libera las plazas; las de demostración, solo aquí). Si falla, deja el motivo en el estado;
     * repetirlo es seguro. La cancelación sigue aunque se cierre la pantalla: la hace el repositorio, no esta pantalla.
     */
    fun onCancel() {
        if (!uiState.value.canCancelNow || cancelling.value) return
        cancelling.value = true
        failure.value = null
        viewModelScope.launch {
            try {
                container.reservations.cancel(id)
            } catch (e: ReservationActionFailure) {
                failure.value = e
            } finally {
                cancelling.value = false
            }
        }
    }
}
