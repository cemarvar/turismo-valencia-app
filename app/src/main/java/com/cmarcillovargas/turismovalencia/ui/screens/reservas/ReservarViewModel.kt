package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.repo.ReservationFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class TimeSlot(val time: String, val available: Boolean)

data class ReservarUiState(
    val offer: Offer? = null,
    val weekDays: List<LocalDate> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val date: LocalDate = LocalDate.now(),
    val slots: List<TimeSlot> = emptyList(),
    val time: String? = null,
    val adults: Int = 2,
    val children: Int = 0,
    val email: String = "",
    /** La reserva se está enviando al servidor. */
    val submitting: Boolean = false,
    /** Por qué falló el último intento; se borra al cambiar cualquier dato o al reintentar. */
    val failure: ReservationFailure? = null,
    /** Hay conexión: reservar la exige (ver las reservas ya guardadas, no). */
    val online: Boolean = true,
) {
    /** El total que se enseña; el que vale es el que calcula el servidor (precio por adulto × adultos). */
    val totalCents: Int get() = (offer?.priceFromCents ?: 0) * adults
    val emailValid: Boolean get() = email.contains("@") && email.substringAfter("@").contains(".")
    val canConfirm: Boolean get() = offer != null && time != null && adults >= 1 && emailValid && online && !submitting
    val canGoBackWeek: Boolean get() = weekDays.firstOrNull()?.let { it.isAfter(today) } == true
}

class ReservarViewModel(private val container: AppContainer, private val offerId: String) : ViewModel() {
    private val offer = container.content.offer(offerId)
    private val today = LocalDate.now()
    private val date = MutableStateFlow(today.plusDays(1))
    private val weekStart = MutableStateFlow(date.value.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    private val time = MutableStateFlow<String?>(offer?.times?.getOrNull(2) ?: offer?.times?.firstOrNull())
    private val adults = MutableStateFlow(2)
    private val children = MutableStateFlow(0)

    // El correo de la última reserva (la primera vez, el de las de demostración): quien reserva de nuevo no lo vuelve a escribir.
    private val email = MutableStateFlow(container.reservations.items.value.firstOrNull()?.email.orEmpty())
    private val submitting = MutableStateFlow(false)
    private val failure = MutableStateFlow<ReservationFailure?>(null)

    private val party = combine(adults, children, email) { a, c, e -> Triple(a, c, e) }
    private val sending = combine(submitting, failure, container.network.isOnline) { s, f, o -> Triple(s, f, o) }

    val uiState: StateFlow<ReservarUiState> = combine(date, weekStart, time, party, sending) { selectedDate, start, selectedTime, (a, c, e), (s, f, o) ->
        ReservarUiState(
            offer = offer,
            weekDays = (0..6).map { start.plusDays(it.toLong()) },
            today = today,
            date = selectedDate,
            slots = slotsOf(offer),
            time = selectedTime,
            adults = a,
            children = c,
            email = e,
            submitting = s,
            failure = f,
            online = o,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservarUiState(offer = offer, email = email.value, online = container.network.isOnline.value))

    fun onSelectDate(value: LocalDate) {
        if (value.isBefore(today)) return
        date.value = value
        failure.value = null
    }

    fun onChangeWeek(delta: Int) {
        weekStart.update { it.plusWeeks(delta.toLong()) }
        val start = weekStart.value
        val end = start.plusDays(6)
        // Mantiene una fecha válida dentro de la semana mostrada.
        if (date.value.isBefore(start) || date.value.isAfter(end)) date.value = maxOf(start, today)
    }

    fun onSelectTime(value: String) {
        time.value = value
        failure.value = null
    }

    fun onAdults(delta: Int) {
        adults.update { (it + delta).coerceIn(1, MaxPeople) }
        failure.value = null
    }

    fun onChildren(delta: Int) {
        children.update { (it + delta).coerceIn(0, MaxPeople) }
        failure.value = null
    }

    fun onEmailChange(value: String) {
        email.value = value
        failure.value = null
    }

    /**
     * Reserva en el servidor y, cuando él la confirma y queda guardada, llama a `onConfirmed` con su id para abrir la confirmación.
     * Si falla, deja el motivo en el estado ([ReservarUiState.failure]); reintentar con los mismos datos manda la misma clave de
     * idempotencia, así que un corte de red a mitad no duplica la reserva.
     */
    fun confirm(onConfirmed: (String) -> Unit) {
        val state = uiState.value
        val chosenTime = state.time ?: return
        // Un segundo toque mientras se envía no crea otra reserva.
        if (!state.canConfirm || submitting.value) return
        submitting.value = true
        failure.value = null
        viewModelScope.launch {
            try {
                val saved = container.reservations.create(offerId, state.date, chosenTime, state.adults, state.children, state.email.trim())
                onConfirmed(saved.id)
            } catch (e: ReservationFailure) {
                failure.value = e
            } finally {
                submitting.value = false
            }
        }
    }

    /** El último horario de la lista está completo, para mostrar un hueco no disponible. */
    private fun slotsOf(offer: Offer?): List<TimeSlot> =
        offer?.times?.mapIndexed { index, value -> TimeSlot(value, !(offer.times.size > 3 && index == offer.times.lastIndex)) }.orEmpty()

    private companion object {
        const val MaxPeople = 12
    }
}
