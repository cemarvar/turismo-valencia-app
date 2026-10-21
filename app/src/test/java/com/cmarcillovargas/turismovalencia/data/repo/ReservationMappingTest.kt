package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.ReservationDto
import com.cmarcillovargas.turismovalencia.data.remote.ReservationsResponse
import com.cmarcillovargas.turismovalencia.data.remote.TurismoApiClient
import com.cmarcillovargas.turismovalencia.data.remote.contractExample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ReservationMappingTest {
    private val contract: ReservationDto =
        TurismoApiClient.Json.decodeFromString<ReservationDto>(contractExample("reservation.json"))

    @Test
    fun `la reserva del contrato se guarda con lo que dice el servidor`() {
        val entity = contract.toEntity(fallbackCreatedAt = 5)!!

        assertEquals("12b229f7-e8d4-475b-9637-2a80c1739f2d", entity.id)
        assertEquals("TV-2027-1100", entity.locator)
        assertEquals("albufera-barca", entity.offerId)
        assertEquals("Paseo en barca por la Albufera", entity.title)
        assertEquals("El Palmar", entity.place)
        assertEquals(LocalDate.of(2027, 2, 11).toEpochDay(), entity.epochDay)
        assertEquals("18:30", entity.time)
        assertEquals(2, entity.adults)
        assertEquals(1, entity.children)
        assertEquals("laura.garcia@email.com", entity.email)
        assertEquals(3600, entity.totalCents)
        assertEquals(ReservationStatus.Confirmed, entity.status)
        assertEquals(Instant.parse("2026-10-14T17:04:06Z").toEpochMilli(), entity.createdAt)
    }

    @Test
    fun `la reserva cancelada del contrato es la misma reserva con otro estado`() {
        val cancelled = TurismoApiClient.Json.decodeFromString<ReservationDto>(contractExample("reservation-cancelled.json"))

        assertEquals(contract.copy(status = "cancelled"), cancelled)
        assertEquals(ReservationStatus.Cancelled, cancelled.toEntity(0)!!.status)
        assertEquals(contract.toEntity(0)!!.id, cancelled.toEntity(0)!!.id)
    }

    @Test
    fun `la lista del contrato entera es legible`() {
        val list = TurismoApiClient.Json.decodeFromString<ReservationsResponse>(contractExample("reservations-list.json")).reservations

        assertEquals(listOf("TV-2027-1100"), list.mapNotNull { it.toEntity(0)?.locator })
    }

    @Test
    fun `el estado del servidor se traduce y uno desconocido no se enseña como confirmado`() {
        assertEquals(ReservationStatus.Confirmed, reservationStatusOf("confirmed"))
        assertEquals(ReservationStatus.Confirmed, reservationStatusOf(" Confirmed "))
        assertEquals(ReservationStatus.Cancelled, reservationStatusOf("cancelled"))
        assertEquals(ReservationStatus.Cancelled, reservationStatusOf("canceled"))
        assertEquals(ReservationStatus.Pending, reservationStatusOf("refunded"))
        assertEquals(ReservationStatus.Cancelled, contract.copy(status = "cancelled").toEntity(0)!!.status)
    }

    @Test
    fun `una reserva sin id, sin localizador o con una fecha ilegible no se guarda`() {
        assertNull(contract.copy(id = " ").toEntity(0))
        assertNull(contract.copy(locator = "").toEntity(0))
        assertNull(contract.copy(date = "11/02/2027").toEntity(0))
        assertNull(contract.copy(date = "").toEntity(0))
        assertNotNull(contract.copy(createdAt = "").toEntity(0))
    }

    @Test
    fun `sin hora de creacion legible se usa la de respaldo`() {
        assertEquals(42L, contract.copy(createdAt = "ayer").toEntity(fallbackCreatedAt = 42)!!.createdAt)
        assertEquals(42L, contract.copy(createdAt = "").toEntity(fallbackCreatedAt = 42)!!.createdAt)
    }

    private fun failureOf(status: Int?, code: String? = null, retryAfter: Int? = null) =
        ApiException("x", status = status, code = code, retryAfterSeconds = retryAfter).toReservationFailure()

    @Test
    fun `cada error de la API se convierte en el motivo que se le cuenta al usuario`() {
        val reasons = ReservationFailure.Reason.entries
        assertEquals(ReservationFailure.Reason.Unreachable, failureOf(null).reason)
        assertEquals(ReservationFailure.Reason.Server, failureOf(500, "internal_error").reason)
        assertEquals(ReservationFailure.Reason.Server, failureOf(502).reason)
        assertEquals(ReservationFailure.Reason.RateLimited, failureOf(429, "rate_limited", 90).reason)
        assertEquals(ReservationFailure.Reason.SoldOut, failureOf(409, "sold_out").reason)
        assertEquals(ReservationFailure.Reason.OfferGone, failureOf(404, "offer_not_found").reason)
        assertEquals(ReservationFailure.Reason.InvalidDate, failureOf(422, "invalid_date").reason)
        assertEquals(ReservationFailure.Reason.InvalidSlot, failureOf(422, "invalid_slot").reason)
        assertEquals(ReservationFailure.Reason.InvalidParty, failureOf(422, "invalid_party").reason)
        assertEquals(ReservationFailure.Reason.InvalidEmail, failureOf(422, "invalid_email").reason)
        assertEquals(ReservationFailure.Reason.Rejected, failureOf(400, "invalid_request").reason)
        assertEquals(ReservationFailure.Reason.Rejected, failureOf(409, "idempotency_key_reused").reason)
        assertEquals(ReservationFailure.Reason.Rejected, failureOf(413, "payload_too_large").reason)
        assertEquals(ReservationFailure.Reason.Rejected, failureOf(403, null).reason)
        assertEquals(90, failureOf(429, "rate_limited", 90).retryAfterSeconds)
        assertTrue("falta cubrir algún motivo en la prueba", reasons.size == 10)
    }

    private fun actionOf(status: Int?, code: String? = null, retryAfter: Int? = null) =
        ApiException("x", status = status, code = code, retryAfterSeconds = retryAfter).toActionFailure()

    @Test
    fun `cada error al cancelar o recuperar se convierte en el motivo que se le cuenta al usuario`() {
        val reasons = ReservationActionFailure.Reason.entries
        assertEquals(ReservationActionFailure.Reason.Unreachable, actionOf(null).reason)
        assertEquals(ReservationActionFailure.Reason.Server, actionOf(500, "internal_error").reason)
        assertEquals(ReservationActionFailure.Reason.Server, actionOf(502).reason)
        assertEquals(ReservationActionFailure.Reason.RateLimited, actionOf(429, "rate_limited", 300).reason)
        assertEquals(ReservationActionFailure.Reason.NotFound, actionOf(404, "reservation_not_found").reason)
        assertEquals(ReservationActionFailure.Reason.TooLate, actionOf(409, "reservation_past").reason)
        assertEquals(ReservationActionFailure.Reason.Rejected, actionOf(400, "invalid_request").reason)
        assertEquals(ReservationActionFailure.Reason.Rejected, actionOf(413, "payload_too_large").reason)
        // Una ruta que el servidor no tiene (una versión más antigua de la API) no es «esa reserva no existe».
        assertEquals(ReservationActionFailure.Reason.Rejected, actionOf(404, "not_found").reason)
        assertEquals(ReservationActionFailure.Reason.Rejected, actionOf(404, null).reason)
        assertEquals(300, actionOf(429, "rate_limited", 300).retryAfterSeconds)
        assertTrue("falta cubrir algún motivo en la prueba", reasons.size == 6)
    }

    @Test
    fun `solo se reintenta con la misma clave cuando el servidor no ha dado una respuesta definitiva`() {
        val retryable = ReservationFailure.Reason.entries.filter { !it.definitive }.toSet()

        assertEquals(setOf(ReservationFailure.Reason.Unreachable, ReservationFailure.Reason.Server, ReservationFailure.Reason.RateLimited), retryable)
        assertFalse(failureOf(409, "sold_out").reason in retryable)
    }
}
