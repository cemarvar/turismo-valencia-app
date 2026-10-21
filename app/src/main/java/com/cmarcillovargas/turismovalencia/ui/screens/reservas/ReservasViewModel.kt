package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.repo.ReservationActionFailure
import com.cmarcillovargas.turismovalencia.data.repo.isPlausibleEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ReservationsTab { Upcoming, History }

/** Reserva con su oferta ya resuelta, lista para pintar. */
data class ReservationRow(val reservation: Reservation, val offer: Offer?)

/** El diálogo «Recuperar una reserva» (la de otro móvil, con su localizador y su correo). */
data class RecoverState(
    val locator: String = "",
    val email: String = "",
    /** Se está buscando en el servidor. */
    val working: Boolean = false,
    /** Por qué falló la última búsqueda; se borra al cambiar cualquier dato. */
    val failure: ReservationActionFailure? = null,
) {
    val canSubmit: Boolean get() = locator.isNotBlank() && isPlausibleEmail(email) && !working
}

data class ReservasUiState(
    val tab: ReservationsTab = ReservationsTab.Upcoming,
    val total: Int = 0,
    val pending: Int = 0,
    val thisWeek: List<ReservationRow> = emptyList(),
    val later: List<ReservationRow> = emptyList(),
    val completed: List<ReservationRow> = emptyList(),
    val cancelled: List<ReservationRow> = emptyList(),
    /** Hay conexión: recuperar una reserva la exige. */
    val online: Boolean = true,
    /** El diálogo de recuperar, abierto; `null` si está cerrado. */
    val recover: RecoverState? = null,
)

class ReservasViewModel(private val container: AppContainer) : ViewModel() {
    private val tab = MutableStateFlow(ReservationsTab.Upcoming)
    private val recover = MutableStateFlow<RecoverState?>(null)

    val uiState: StateFlow<ReservasUiState> = combine(tab, container.reservations.items, recover, container.network.isOnline) { current, all, dialog, online ->
        val today = LocalDate.now()
        val rows = all.map { reservation ->
            val offer = container.content.offer(reservation.offerId)
            ReservationRow(reservation.shownAs(offer), offer)
        }
        val upcoming = rows.filter { it.reservation.status != ReservationStatus.Completed && it.reservation.status != ReservationStatus.Cancelled }.sortedBy { it.reservation.date }
        val done = rows.filter { it.reservation.status == ReservationStatus.Completed }.sortedByDescending { it.reservation.date }
        val cancelled = rows.filter { it.reservation.status == ReservationStatus.Cancelled }.sortedByDescending { it.reservation.date }
        ReservasUiState(
            tab = current,
            total = all.size,
            pending = all.count { it.status == ReservationStatus.Pending },
            thisWeek = if (current == ReservationsTab.Upcoming) upcoming.filter { it.reservation.date <= today.plusDays(7) } else emptyList(),
            later = if (current == ReservationsTab.Upcoming) upcoming.filter { it.reservation.date > today.plusDays(7) } else emptyList(),
            completed = done,
            cancelled = cancelled,
            online = online,
            recover = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservasUiState(online = container.network.isOnline.value))

    fun onSelectTab(value: ReservationsTab) {
        tab.value = value
    }

    fun onOpenRecover() {
        recover.value = RecoverState()
    }

    /** Cierra el diálogo, salvo mientras busca (el resultado llegaría a una pantalla que ya no lo espera). */
    fun onDismissRecover() {
        if (recover.value?.working != true) recover.value = null
    }

    fun onRecoverLocator(value: String) {
        recover.update { it?.copy(locator = value, failure = null) }
    }

    fun onRecoverEmail(value: String) {
        recover.update { it?.copy(email = value, failure = null) }
    }

    /**
     * Busca la reserva en el servidor con el localizador y el correo y la guarda en el móvil; cuando ya está guardada, llama a
     * `onFound` con su id para abrirla. Si falla, deja el motivo en el diálogo. La búsqueda la hace el repositorio, así que una
     * reserva encontrada se guarda aunque se cierre la pantalla.
     */
    fun onRecover(onFound: (String) -> Unit) {
        val state = recover.value ?: return
        if (!state.canSubmit || !container.network.isOnline.value) return
        recover.value = state.copy(working = true, failure = null)
        viewModelScope.launch {
            try {
                val found = container.reservations.recover(state.locator, state.email)
                recover.value = null
                onFound(found.id)
            } catch (e: ReservationActionFailure) {
                recover.update { it?.copy(working = false, failure = e) }
            }
        }
    }
}
