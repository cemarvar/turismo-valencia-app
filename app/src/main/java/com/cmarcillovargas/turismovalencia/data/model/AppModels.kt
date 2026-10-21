package com.cmarcillovargas.turismovalencia.data.model

import java.time.LocalDate
import java.util.Locale

/** `Confirmed` y `Cancelled` los pone el servidor; `Completed` la deduce la app cuando la fecha ya pasó; `Pending` solo existe en los datos de demostración. */
enum class ReservationStatus { Confirmed, Pending, Completed, Cancelled }

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
    /** Total en céntimos (el que calculó el servidor). */
    val totalCents: Int,
    val status: ReservationStatus,
    val locator: String,
) {
    val people: Int get() = adults + children

    /** Una de las tres de demostración del primer arranque: solo existe en el móvil, el servidor no la conoce. */
    val isDemo: Boolean get() = id.startsWith(DemoIdPrefix)

    /**
     * La reserva con el título y el lugar de su oferta tal como se ven ahora, en el idioma de la app: la base de datos guarda los del
     * momento de reservar (los de las de demostración, siempre en español).
     */
    fun shownAs(offer: Offer?): Reservation = if (offer == null) this else copy(title = offer.title, place = offer.place)

    companion object {
        /** Principio del identificador de las reservas de demostración (`seed-1`…). Las del servidor llevan un UUID. */
        const val DemoIdPrefix = "seed-"
    }
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

/**
 * Idiomas de la app. [tag] es la etiqueta BCP 47 que se le da al sistema: el valenciano es catalán con la variante `valencia`
 * (`ca-ES-valencia`) y sus recursos viven en `values-ca`. El idioma que de verdad se está mostrando se lee de la cadena
 * `locale_code` de los recursos (ver [fromCode]), no de la configuración del móvil, porque Android elige los recursos con la lista
 * entera de idiomas del usuario y esa cadena es lo único que dice cuál ha ganado.
 */
enum class LanguageCode(val code: String, val tag: String) {
    ES("ES", "es"),
    VA("VA", "ca-ES-valencia"),
    EN("EN", "en"),
    FR("FR", "fr"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        /** El idioma de [code] (`ES`, `VA`, `EN`, `FR`), o `null` si no es ninguno. */
        fun fromCode(code: String?): LanguageCode? = entries.firstOrNull { it.code == code?.trim()?.uppercase() }
    }
}

enum class TextScale(val factor: Float) { System(1f), Small(0.9f), Large(1.15f), ExtraLarge(1.3f) }

enum class LocationMode { WhileUsing, OnlyOnce, Denied }

/** El modo claro u oscuro de la app: el del móvil (por defecto) o uno fijo. Se elige en Perfil → Tema; ver `data/AppTheme.kt`. */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    companion object {
        /** El modo guardado con ese nombre; cualquier otra cosa (nada guardado, un valor que no se conoce) es el del móvil. */
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: System
    }
}

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
    /** Respuesta de las reglas de la app porque el asistente en línea no ha respondido. */
    val basic: Boolean = false,
)
