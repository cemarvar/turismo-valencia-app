package com.cmarcillovargas.turismovalencia.data.model

import java.time.LocalDate

enum class ReservationStatus { Confirmed, Pending, Completed }

data class Reservation(
    val id: String,
    val offerId: String,
    val title: String,
    val place: String,
    val date: LocalDate,
    val time: String,
    val adults: Int,
    val children: Int,
    val email: String,
    val total: Int,
    val status: ReservationStatus,
    val locator: String,
) {
    val people: Int get() = adults + children
}

enum class NotificationKind { Event, Booking, Download, Eco, Bot }

enum class NotificationGroup { Today, ThisWeek }

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val group: NotificationGroup,
    val title: String,
    val body: String,
    val whenLabel: String,
    val unread: Boolean,
    /** Ficha a la que lleva la notificación, si la hay. */
    val itemId: String? = null,
)

enum class LanguageCode(val code: String) { ES("ES"), VA("VA"), EN("EN"), FR("FR") }

enum class TextScale(val factor: Float) { System(1f), Small(0.9f), Large(1.15f), ExtraLarge(1.3f) }

enum class LocationMode { WhileUsing, OnlyOnce, Denied }

enum class SortMode { Distance, Rating, Name }

/** Sugerencia del buscador. */
data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val kind: Kind,
    /** Id de ficha, id de oferta o texto a buscar en el mapa, según `kind`. */
    val target: String,
) {
    enum class Kind { Dish, Place, Offer, Map, Guide }
}

/** Mensaje del chat de ValBot. */
data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val time: String,
    /** Fichas que acompañan a la respuesta del bot. */
    val cardIds: List<String> = emptyList(),
    /** Guías de la web que acompañan a la respuesta del bot. */
    val pageIds: List<String> = emptyList(),
    val isError: Boolean = false,
    val offlineLinks: Boolean = false,
)
