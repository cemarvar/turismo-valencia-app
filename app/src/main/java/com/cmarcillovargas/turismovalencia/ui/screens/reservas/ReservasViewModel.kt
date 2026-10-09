package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class ReservationsTab { Upcoming, History }

/** Reserva con su oferta ya resuelta, lista para pintar. */
data class ReservationRow(val reservation: Reservation, val offer: Offer?)

data class ReservasUiState(
    val tab: ReservationsTab = ReservationsTab.Upcoming,
    val total: Int = 0,
    val pending: Int = 0,
    val thisWeek: List<ReservationRow> = emptyList(),
    val later: List<ReservationRow> = emptyList(),
    val completed: List<ReservationRow> = emptyList(),
)

class ReservasViewModel(private val container: AppContainer) : ViewModel() {
    private val tab = MutableStateFlow(ReservationsTab.Upcoming)

    val uiState: StateFlow<ReservasUiState> = combine(tab, container.reservations.items) { current, all ->
        val today = LocalDate.now()
        val rows = all.map { ReservationRow(it, container.content.offer(it.offerId)) }
        val upcoming = rows.filter { it.reservation.status != ReservationStatus.Completed }.sortedBy { it.reservation.date }
        val done = rows.filter { it.reservation.status == ReservationStatus.Completed }.sortedByDescending { it.reservation.date }
        ReservasUiState(
            tab = current,
            total = all.size,
            pending = all.count { it.status == ReservationStatus.Pending },
            thisWeek = if (current == ReservationsTab.Upcoming) upcoming.filter { it.reservation.date <= today.plusDays(7) } else emptyList(),
            later = if (current == ReservationsTab.Upcoming) upcoming.filter { it.reservation.date > today.plusDays(7) } else emptyList(),
            completed = done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservasUiState())

    fun onSelectTab(value: ReservationsTab) {
        tab.value = value
    }
}
