package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.local.ReservationEntity
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.remote.ApiException
import com.cmarcillovargas.turismovalencia.data.remote.ReservationDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

// De la reserva del servidor a la de Room. Es Kotlin puro (sin Android) para probarlo en la JVM.

/** Por qué no se pudo reservar. Lo que se le dice al usuario depende de esto, no del texto del servidor. */
class ReservationFailure(val reason: Reason, val retryAfterSeconds: Int? = null, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason(
        /** El servidor dio una respuesta definitiva: el intento se acabó (si no, se reintenta con la misma clave). */
        val definitive: Boolean,
    ) {
        /** Sin red, tiempo agotado o respuesta ilegible: puede que el servidor sí la guardara. */
        Unreachable(false),

        /** Error 5xx del servidor. */
        Server(false),

        /** Demasiadas peticiones (429): se rechazó antes de mirar nada. */
        RateLimited(false),
        SoldOut(true),

        /** La oferta ya no existe o no está disponible. */
        OfferGone(true),
        InvalidDate(true),
        InvalidSlot(true),
        InvalidParty(true),
        InvalidEmail(true),

        /** Cualquier otro rechazo del servidor (datos con forma incorrecta, clave de idempotencia reutilizada…). */
        Rejected(true),
    }
}

/** El fallo de la API como fallo de reserva, por el estado y el código de error del contrato (`docs/api.md`, «Errores»). */
internal fun ApiException.toReservationFailure(): ReservationFailure {
    val reason = when {
        status == null -> ReservationFailure.Reason.Unreachable
        status == 429 || code == "rate_limited" -> ReservationFailure.Reason.RateLimited
        status >= 500 -> ReservationFailure.Reason.Server
        code == "sold_out" -> ReservationFailure.Reason.SoldOut
        code == "offer_not_found" -> ReservationFailure.Reason.OfferGone
        code == "invalid_date" -> ReservationFailure.Reason.InvalidDate
        code == "invalid_slot" -> ReservationFailure.Reason.InvalidSlot
        code == "invalid_party" -> ReservationFailure.Reason.InvalidParty
        code == "invalid_email" -> ReservationFailure.Reason.InvalidEmail
        else -> ReservationFailure.Reason.Rejected
    }
    return ReservationFailure(reason, retryAfterSeconds, this)
}

/** Por qué no se pudo cancelar o recuperar una reserva. Lo que se le dice al usuario depende de esto, no del texto del servidor. */
class ReservationActionFailure(val reason: Reason, val retryAfterSeconds: Int? = null, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** Sin red, tiempo agotado o respuesta ilegible. */
        Unreachable,

        /** Error 5xx del servidor. */
        Server,

        /** Demasiadas peticiones (429). */
        RateLimited,

        /** El servidor no la conoce o no es de quien lo pide; al recuperar, el localizador y el correo no coinciden. */
        NotFound,

        /** La actividad ya empezó o pasó: no se puede cancelar. */
        TooLate,

        /** Cualquier otro rechazo del servidor (datos con forma incorrecta…). */
        Rejected,
    }
}

/** El fallo de la API al cancelar o recuperar, por el estado y el código de error del contrato (`docs/api.md`, «Errores»). */
internal fun ApiException.toActionFailure(): ReservationActionFailure {
    val reason = when {
        status == null -> ReservationActionFailure.Reason.Unreachable
        status == 429 || code == "rate_limited" -> ReservationActionFailure.Reason.RateLimited
        status >= 500 -> ReservationActionFailure.Reason.Server
        code == "reservation_not_found" -> ReservationActionFailure.Reason.NotFound
        code == "reservation_past" -> ReservationActionFailure.Reason.TooLate
        else -> ReservationActionFailure.Reason.Rejected
    }
    return ReservationActionFailure(reason, retryAfterSeconds, this)
}

/** Estado del servidor a estado de la app: `confirmed`, `cancelled`; uno desconocido se enseña como pendiente, nunca como confirmado. */
internal fun reservationStatusOf(raw: String): ReservationStatus = when (raw.trim().lowercase()) {
    "confirmed" -> ReservationStatus.Confirmed
    "cancelled", "canceled" -> ReservationStatus.Cancelled
    else -> ReservationStatus.Pending
}

/**
 * La reserva lista para guardar, o `null` si no se entiende (sin id o localizador, o una fecha que no es `AAAA-MM-DD`). Si
 * `createdAt` no se puede leer se usa [fallbackCreatedAt].
 */
internal fun ReservationDto.toEntity(fallbackCreatedAt: Long): ReservationEntity? {
    if (id.isBlank() || locator.isBlank()) return null
    val day = try {
        LocalDate.parse(date)
    } catch (_: DateTimeParseException) {
        return null
    }
    val created = try {
        Instant.parse(createdAt).toEpochMilli()
    } catch (_: DateTimeParseException) {
        fallbackCreatedAt
    }
    return ReservationEntity(
        id = id,
        offerId = offerId,
        title = title,
        place = place,
        epochDay = day.toEpochDay(),
        time = time,
        adults = adults,
        children = children,
        email = email,
        totalCents = totalCents,
        status = reservationStatusOf(status),
        locator = locator,
        createdAt = created,
    )
}
