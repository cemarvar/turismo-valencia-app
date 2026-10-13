package com.cmarcillovargas.turismovalencia.data.local

import com.cmarcillovargas.turismovalencia.data.model.AppNotification
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import java.time.LocalDate

/**
 * Una reserva confirmada cuya fecha ya pasó se muestra como completada: la fecha se guarda una sola vez y la
 * reserva «envejece» sola, sin tener que actualizar la fila.
 */
fun ReservationEntity.toModel(today: LocalDate = LocalDate.now()): Reservation {
    val date = LocalDate.ofEpochDay(epochDay)
    return Reservation(
        id = id,
        offerId = offerId,
        title = title,
        place = place,
        date = date,
        time = time,
        adults = adults,
        children = children,
        email = email,
        total = total,
        status = if (status == ReservationStatus.Confirmed && date.isBefore(today)) ReservationStatus.Completed else status,
        locator = "TV-${date.year}-${number.toString().padStart(4, '0')}",
    )
}

fun NotificationEntity.toModel() = AppNotification(
    id = id,
    kind = kind,
    group = period,
    title = title,
    body = body,
    whenLabel = whenLabel,
    unread = unread,
    itemId = itemId,
)
