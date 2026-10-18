package com.cmarcillovargas.turismovalencia.util

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

// Formato de distancias, importes y fechas en el idioma de la app. `AppContainer.applyLanguage` fija `Locale.getDefault()` al idioma
// elegido, así que las funciones lo toman de ahí; las pruebas (y quien quiera otro idioma) pasan el suyo. Los nombres de los días y
// los meses están escritos aquí en vez de pedírselos a `DateTimeFormatter`: así no dependen de la versión de ICU del móvil (el catalán
// abrevia los meses de forma distinta según la versión) y las pruebas dan lo mismo en el ordenador y en el emulador.

/** El idioma de la app que corresponde a [locale]: el valenciano es catalán (`ca`); cualquier otro idioma cae al español. */
fun languageOf(locale: Locale): LanguageCode = when (locale.language) {
    "ca" -> LanguageCode.VA
    "en" -> LanguageCode.EN
    "fr" -> LanguageCode.FR
    else -> LanguageCode.ES
}

/** «450 m», «1,1 km», «11 km» (en inglés, «1.1 km»). */
fun formatDistance(meters: Int, locale: Locale = Locale.getDefault()): String = when {
    meters < 1_000 -> "$meters m"
    meters < 10_000 -> String.format(locale, "%.1f km", meters / 1000.0)
    else -> "${(meters / 1000.0).roundToInt()} km"
}

/** Minutos andando a ~4,5 km/h. */
fun walkMinutes(meters: Int): Int = meters / 75

fun formatRating(rating: Double, locale: Locale = Locale.getDefault()): String = String.format(locale, "%.1f", rating)

/**
 * Un importe en céntimos (como lo manda la API) en euros: «18 €», «18,50 €»; en francés con espacio de no separación y en inglés con
 * el símbolo delante («€18.50»).
 */
fun formatPrice(cents: Int, locale: Locale = Locale.getDefault()): String {
    val euros = cents / 100
    val rest = cents % 100
    val language = languageOf(locale)
    val separator = if (language == LanguageCode.EN) '.' else ','
    val amount = if (rest == 0) "$euros" else "$euros$separator${rest.toString().padStart(2, '0')}"
    return when (language) {
        LanguageCode.EN -> "€$amount"
        LanguageCode.FR -> "$amount €"
        else -> "$amount €"
    }
}

private val WeekdaysShort = mapOf(
    LanguageCode.ES to listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"),
    LanguageCode.VA to listOf("Dl", "Dt", "Dc", "Dj", "Dv", "Ds", "Dg"),
    LanguageCode.EN to listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    LanguageCode.FR to listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"),
)

private val WeekdaysNarrow = mapOf(
    LanguageCode.ES to listOf("L", "M", "X", "J", "V", "S", "D"),
    LanguageCode.VA to listOf("Dl", "Dt", "Dc", "Dj", "Dv", "Ds", "Dg"),
    LanguageCode.EN to listOf("M", "T", "W", "T", "F", "S", "S"),
    LanguageCode.FR to listOf("L", "M", "M", "J", "V", "S", "D"),
)

private val MonthsShort = mapOf(
    LanguageCode.ES to listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"),
    LanguageCode.VA to listOf("gen", "febr", "març", "abr", "maig", "juny", "jul", "ag", "set", "oct", "nov", "des"),
    LanguageCode.EN to listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
    LanguageCode.FR to listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."),
)

private val MonthsLong = mapOf(
    LanguageCode.ES to listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"),
    LanguageCode.VA to listOf("Gener", "Febrer", "Març", "Abril", "Maig", "Juny", "Juliol", "Agost", "Setembre", "Octubre", "Novembre", "Desembre"),
    LanguageCode.EN to listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    LanguageCode.FR to listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"),
)

/** «Sáb 12 oct» («Sat 12 Oct», «Ds 12 oct», «Sam 12 oct.»). */
fun formatShortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
    val language = languageOf(locale)
    return "${WeekdaysShort.getValue(language)[date.dayOfWeek.ordinal]} ${date.dayOfMonth} ${MonthsShort.getValue(language)[date.monthValue - 1]}"
}

/** Inicial (o dos letras, en valenciano) del día de la semana para la cabecera de la semana al reservar: «L», «M», «X»… */
fun formatWeekdayNarrow(day: DayOfWeek, locale: Locale = Locale.getDefault()): String =
    WeekdaysNarrow.getValue(languageOf(locale))[day.ordinal]

/** «Octubre 2026» («October 2026»). */
fun formatMonthYear(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    "${MonthsLong.getValue(languageOf(locale))[date.monthValue - 1]} ${date.year}"

private val Diacritics = Regex("\\p{M}+")

/** Minúsculas y sin tildes, para comparar textos al buscar. */
fun normalizeText(text: String): String =
    Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Diacritics, "")
