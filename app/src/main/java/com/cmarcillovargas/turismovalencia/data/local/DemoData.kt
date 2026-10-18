package com.cmarcillovargas.turismovalencia.data.local

import androidx.room.withTransaction
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.NotificationGroup
import com.cmarcillovargas.turismovalencia.data.model.NotificationKind
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.data.repo.ContentRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Datos de demostración del primer arranque: la app se abre como en el diseño (favoritos, descargas, tres reservas,
 * avisos…) y, desde ahí, todo lo que el usuario cambia queda guardado. Solo se insertan cuando la base de datos
 * se crea; no vuelven a aparecer después de borrarlos.
 */
class DemoData(private val content: ContentRepository) {

    /** Primer arranque = todavía no hay fila de ajustes. */
    suspend fun seedIfFirstRun(db: AppDatabase) {
        if (db.settings().current() == null) populate(db)
    }

    /**
     * Deja la base de datos como en el primer arranque (también lo usa el gancho de depuración `resetData`). El idioma no es parte de
     * la demostración: se conserva el que se esté usando.
     */
    suspend fun populate(
        db: AppDatabase,
        today: LocalDate = LocalDate.now(),
        now: Long = System.currentTimeMillis(),
        language: LanguageCode = LanguageCode.ES,
    ) = db.withTransaction {
        db.favorites().run {
            clear()
            insertAll(Favorites.map { FavoriteEntity(itemId = it) })
        }
        db.downloads().run {
            clear()
            insertAll(Downloads.map { DownloadEntity(itemId = it) })
        }
        db.reservations().run {
            clear()
            insertAll(reservations(today, now))
        }
        db.reservationAttempts().clear()
        db.recentSearches().run {
            clear()
            insertAll(RecentSearches.mapIndexed { index, text -> RecentSearchEntity(text, now - index) })
        }
        db.notifications().run {
            clear()
            insertAll(notifications())
        }
        db.reminders().run {
            clear()
            insertAll(Reminders.map { ReminderEntity(it) })
        }
        // Como en el primer arranque: sin catálogo del servidor recibido (se vuelve a pedir cuando haya conexión).
        db.offers().clearCache()
        db.settings().save(SettingsEntity(language = language))
    }

    private fun reservations(today: LocalDate, now: Long): List<ReservationEntity> = listOf(
        seedReservation(0, "albufera-barca", today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)), "18:30", 2, ReservationStatus.Confirmed, null, now),
        seedReservation(1, "taller-paella", today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusDays(7), "13:00", 4, ReservationStatus.Pending, 18_000, now),
        seedReservation(2, "ruta-ciutat-vella", today.minusDays(5), "10:00", 1, ReservationStatus.Completed, null, now),
    )

    private fun seedReservation(
        index: Int,
        offerId: String,
        date: LocalDate,
        time: String,
        adults: Int,
        status: ReservationStatus,
        totalCents: Int?,
        now: Long,
    ): ReservationEntity {
        val offer = checkNotNull(content.offer(offerId))
        return ReservationEntity(
            id = "seed-${index + 1}",
            offerId = offerId,
            title = offer.title,
            place = offer.place,
            epochDay = date.toEpochDay(),
            time = time,
            adults = adults,
            children = 0,
            email = DemoEmail,
            totalCents = totalCents ?: (offer.priceFromCents * adults),
            status = status,
            locator = "TV-${date.year}-${(FirstSeedNumber + index).toString().padStart(4, '0')}",
            createdAt = now - index * MinuteMillis,
        )
    }

    private fun notifications(): List<NotificationEntity> = listOf(
        NotificationEntity("n1", 0, NotificationKind.Event, NotificationGroup.Today, "Mascletà a las 14:00", "Plaza del Ayuntamiento · llega 30 min antes para coger sitio.", "Hace 10 min", true, "fallas"),
        NotificationEntity("n2", 1, NotificationKind.Booking, NotificationGroup.Today, "Reserva confirmada", "Paseo en barca por la Albufera · sáb 12 oct, 18:30.", "Hace 1 h", true, null),
        NotificationEntity("n3", 2, NotificationKind.Download, NotificationGroup.ThisWeek, "Fichas actualizadas", "4 lugares guardados se han actualizado para usarlos sin conexión.", "Ayer", false, null),
        NotificationEntity("n4", 3, NotificationKind.Eco, NotificationGroup.ThisWeek, "Ruta sostenible recomendada", "Recorre el Jardín del Turia en Valenbisi: 9 km sin coches.", "Lunes", false, "jardin-turia"),
        NotificationEntity("n5", 4, NotificationKind.Bot, NotificationGroup.ThisWeek, "ValBot tiene novedades", "Ahora responde también en valenciano e inglés.", "Domingo", false, null),
    )

    private companion object {
        val Favorites = listOf("mercado-central", "albufera-barca", "fallas", "paella", "lonja-seda", "catedral-caliz")
        val Downloads = listOf("mercado-central", "fallas", "paella", "lonja-seda")
        val RecentSearches = listOf("Mercado Central", "Fallas 2027")

        /** Como en el diseño, el aviso de la mascletà sale activado. */
        val Reminders = listOf("fallas")

        /** Dominio reservado `example.com`: el correo de demostración nunca es el buzón de nadie (la reserva de verdad lo guarda el servidor). */
        const val DemoEmail = "laura.garcia@example.com"
        const val FirstSeedNumber = 413
        const val MinuteMillis = 60_000L
    }
}
