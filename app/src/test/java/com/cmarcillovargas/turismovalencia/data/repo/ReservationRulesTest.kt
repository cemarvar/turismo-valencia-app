package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.notify.ReminderPlanner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Cuándo se enseña «Cancelar reserva»: la misma regla que aplica el servidor, en la hora de Valencia. */
class ReservationRulesTest {
    private val valencia = ReminderPlanner.Valencia
    private val today = LocalDate.of(2026, 10, 21)

    private fun at(hour: Int, minute: Int, day: LocalDate = today) = ZonedDateTime.of(day.year, day.monthValue, day.dayOfMonth, hour, minute, 0, 0, valencia)

    private fun reservation(date: LocalDate, time: String = "18:30", status: ReservationStatus = ReservationStatus.Confirmed) = Reservation(
        id = "r", offerId = "o", title = "t", place = "p", date = date, time = time, adults = 1, children = 0,
        email = "a@example.com", totalCents = 100, status = status, locator = "TV-2026-1",
    )

    @Test
    fun `un dia futuro se puede cancelar a cualquier hora`() {
        assertTrue(reservation(today.plusDays(1), "00:01").canCancel(at(23, 59)))
        assertTrue(reservation(today.plusDays(30)).canCancel(at(12, 0)))
    }

    @Test
    fun `un dia pasado no`() {
        assertFalse(reservation(today.minusDays(1), "23:59").canCancel(at(0, 5)))
    }

    @Test
    fun `hoy se puede hasta que empieza la actividad`() {
        assertTrue(reservation(today, "18:30").canCancel(at(10, 0)))
        assertTrue(reservation(today, "18:30").canCancel(at(18, 29)))
        assertFalse("a la hora en punto ya ha empezado", reservation(today, "18:30").canCancel(at(18, 30)))
        assertFalse(reservation(today, "10:00").canCancel(at(18, 0)))
    }

    @Test
    fun `una actividad sin hora vale todo el dia`() {
        assertTrue(reservation(today, "Cualquier hora").canCancel(at(23, 59)))
        assertFalse(reservation(today.minusDays(1), "Cualquier hora").canCancel(at(0, 1)))
    }

    @Test
    fun `una reserva cancelada o completada no se cancela otra vez`() {
        assertFalse(reservation(today.plusDays(5), status = ReservationStatus.Cancelled).canCancel(at(10, 0)))
        assertFalse(reservation(today.plusDays(5), status = ReservationStatus.Completed).canCancel(at(10, 0)))
        assertTrue(reservation(today.plusDays(5), status = ReservationStatus.Pending).canCancel(at(10, 0)))
    }

    @Test
    fun `cuenta en la hora de Valencia aunque el movil tenga otra`() {
        // 22:30 UTC del 21 de octubre ya es la 00:30 del 22 en Valencia (UTC+2): la actividad del 21 pasó y la del 22 a las 10:00 no.
        val late = ZonedDateTime.of(2026, 10, 21, 22, 30, 0, 0, ZoneId.of("UTC"))
        assertFalse(reservation(today, "Cualquier hora").canCancel(late))
        assertTrue(reservation(today.plusDays(1), "10:00").canCancel(late))
        // La misma hora vista desde Nueva York (18:30 del 21) da lo mismo.
        val newYork = ZonedDateTime.of(2026, 10, 21, 18, 30, 0, 0, ZoneId.of("America/New_York"))
        assertFalse(reservation(today, "Cualquier hora").canCancel(newYork))
        assertTrue(reservation(today.plusDays(1), "10:00").canCancel(newYork))
    }

    @Test
    fun `las tres reservas de demostracion se reconocen por su identificador`() {
        assertTrue(reservation(today).copy(id = "seed-1").isDemo)
        assertFalse(reservation(today).copy(id = "12b229f7-e8d4-475b-9637-2a80c1739f2d").isDemo)
    }
}
