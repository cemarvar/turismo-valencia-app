package com.cmarcillovargas.turismovalencia.data.local

import com.cmarcillovargas.turismovalencia.data.model.NotificationGroup
import com.cmarcillovargas.turismovalencia.data.model.NotificationKind
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MappersTest {
    private val day = LocalDate.of(2026, 10, 17)

    private fun row(status: ReservationStatus, number: Int = 413) = ReservationEntity(
        id = "res-$number",
        offerId = "albufera-barca",
        title = "Paseo en barca por la Albufera",
        place = "El Palmar",
        epochDay = day.toEpochDay(),
        time = "18:30",
        adults = 2,
        children = 1,
        email = "a@b.es",
        total = 36,
        status = status,
        number = number,
        createdAt = 0,
    )

    @Test
    fun `convierte la fila en la reserva con su localizador`() {
        val reservation = row(ReservationStatus.Confirmed).toModel(today = day.minusDays(3))
        assertEquals("res-413", reservation.id)
        assertEquals(day, reservation.date)
        assertEquals("TV-2026-0413", reservation.locator)
        assertEquals(3, reservation.people)
        assertEquals(36, reservation.total)
        assertEquals(ReservationStatus.Confirmed, reservation.status)
    }

    @Test
    fun `el numero se rellena con ceros hasta cuatro cifras`() {
        assertEquals("TV-2026-0007", row(ReservationStatus.Confirmed, number = 7).toModel(day).locator)
        assertEquals("TV-2026-12345", row(ReservationStatus.Confirmed, number = 12345).toModel(day).locator)
    }

    @Test
    fun `una reserva confirmada cuya fecha ya pasó se muestra completada`() {
        assertEquals(ReservationStatus.Confirmed, row(ReservationStatus.Confirmed).toModel(today = day).status)
        assertEquals(ReservationStatus.Completed, row(ReservationStatus.Confirmed).toModel(today = day.plusDays(1)).status)
    }

    @Test
    fun `una pendiente o completada no cambia de estado`() {
        assertEquals(ReservationStatus.Pending, row(ReservationStatus.Pending).toModel(today = day.plusDays(30)).status)
        assertEquals(ReservationStatus.Completed, row(ReservationStatus.Completed).toModel(today = day.minusDays(30)).status)
    }

    @Test
    fun `convierte el aviso`() {
        val notification = NotificationEntity("n1", 0, NotificationKind.Event, NotificationGroup.Today, "Mascletà", "A las 14:00", "Hace 10 min", true, "fallas").toModel()
        assertEquals("n1", notification.id)
        assertEquals(NotificationGroup.Today, notification.group)
        assertEquals("Hace 10 min", notification.whenLabel)
        assertEquals("fallas", notification.itemId)
        assertNull(NotificationEntity("n2", 1, NotificationKind.Bot, NotificationGroup.ThisWeek, "t", "b", "Ayer", false, null).toModel().itemId)
    }
}
