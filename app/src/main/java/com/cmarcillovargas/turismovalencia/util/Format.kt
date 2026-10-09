package com.cmarcillovargas.turismovalencia.util

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Spanish = Locale("es", "ES")

/** «450 m», «1,1 km», «11 km». */
fun formatDistance(meters: Int): String = when {
    meters < 1_000 -> "$meters m"
    meters < 10_000 -> String.format(Spanish, "%.1f km", meters / 1000.0)
    else -> "${(meters / 1000.0).roundToInt()} km"
}

/** Minutos andando a ~4,5 km/h. */
fun walkMinutes(meters: Int): Int = meters / 75

fun formatRating(rating: Double): String = String.format(Spanish, "%.1f", rating)

fun formatEuro(amount: Int): String = "$amount €"

/** «Sáb 12 oct». */
fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM", Spanish)).replace(".", "").replaceFirstChar { it.uppercase() }

/** «Octubre 2026». */
fun formatMonthYear(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("LLLL yyyy", Spanish)).replaceFirstChar { it.uppercase() }

/** Minúsculas y sin tildes, para comparar textos al buscar. */
fun normalizeText(text: String): String =
    Normalizer.normalize(text.lowercase(Spanish), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
