package com.cmarcillovargas.turismovalencia.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

private val Es = Locale("es", "ES")
private val Va = Locale.forLanguageTag("ca-ES-valencia")
private val En = Locale.UK
private val Fr = Locale.FRANCE

class FormatTest {
    @Test
    fun `los importes se enseñan en euros a la española`() {
        assertEquals("0 €", formatPrice(0, Es))
        assertEquals("18 €", formatPrice(1800, Es))
        assertEquals("1 €", formatPrice(100, Es))
        assertEquals("180 €", formatPrice(18_000, Es))
    }

    @Test
    fun `los centimos llevan coma y siempre dos cifras`() {
        assertEquals("18,50 €", formatPrice(1850, Es))
        assertEquals("0,05 €", formatPrice(5, Es))
        assertEquals("20,05 €", formatPrice(2005, Es))
        assertEquals("0,99 €", formatPrice(99, Es))
    }

    @Test
    fun `el valenciano formatea los importes como el espanol`() {
        assertEquals("18 €", formatPrice(1800, Va))
        assertEquals("18,50 €", formatPrice(1850, Va))
    }

    @Test
    fun `en ingles el simbolo va delante y los decimales llevan punto`() {
        assertEquals("€18", formatPrice(1800, En))
        assertEquals("€18.50", formatPrice(1850, En))
        assertEquals("€0.05", formatPrice(5, En))
    }

    @Test
    fun `en frances el simbolo va detras con un espacio de no separacion`() {
        assertEquals("18 €", formatPrice(1800, Fr))
        assertEquals("18,50 €", formatPrice(1850, Fr))
    }

    @Test
    fun `las distancias usan el separador decimal del idioma`() {
        assertEquals("450 m", formatDistance(450, Es))
        assertEquals("1,1 km", formatDistance(1_100, Es))
        assertEquals("1,1 km", formatDistance(1_100, Va))
        assertEquals("1.1 km", formatDistance(1_100, En))
        assertEquals("1,1 km", formatDistance(1_100, Fr))
        assertEquals("11 km", formatDistance(11_000, En))
    }

    @Test
    fun `las valoraciones llevan un decimal`() {
        assertEquals("4,8", formatRating(4.8, Es))
        assertEquals("4.8", formatRating(4.8, En))
    }

    // 12 de octubre de 2026: lunes.
    private val monday = LocalDate.of(2026, 10, 12)

    @Test
    fun `las fechas cortas salen en cada idioma`() {
        assertEquals("Lun 12 oct", formatShortDate(monday, Es))
        assertEquals("Dl 12 oct", formatShortDate(monday, Va))
        assertEquals("Mon 12 Oct", formatShortDate(monday, En))
        assertEquals("Lun 12 oct.", formatShortDate(monday, Fr))
        assertEquals("Sáb 5 sep", formatShortDate(LocalDate.of(2026, 9, 5), Es))
        assertEquals("Dg 1 març", formatShortDate(LocalDate.of(2026, 3, 1), Va))
    }

    @Test
    fun `el mes y el año salen en cada idioma`() {
        assertEquals("Octubre 2026", formatMonthYear(monday, Es))
        assertEquals("Octubre 2026", formatMonthYear(monday, Va))
        assertEquals("October 2026", formatMonthYear(monday, En))
        assertEquals("Octobre 2026", formatMonthYear(monday, Fr))
        assertEquals("Març 2027", formatMonthYear(LocalDate.of(2027, 3, 1), Va))
    }

    @Test
    fun `las iniciales de los dias de la semana`() {
        assertEquals("X", formatWeekdayNarrow(DayOfWeek.WEDNESDAY, Es))
        assertEquals("W", formatWeekdayNarrow(DayOfWeek.WEDNESDAY, En))
        assertEquals("Dc", formatWeekdayNarrow(DayOfWeek.WEDNESDAY, Va))
        assertEquals("M", formatWeekdayNarrow(DayOfWeek.WEDNESDAY, Fr))
    }

    @Test
    fun `un idioma que la app no ofrece cae al español`() {
        assertEquals("18 €", formatPrice(1800, Locale.GERMANY))
        assertEquals("Lun 12 oct", formatShortDate(monday, Locale.GERMANY))
    }

    @Test
    fun `normalizeText quita tildes y mayusculas en cualquier idioma`() {
        assertEquals("catedral y santo caliz", normalizeText("Catedral y Santo Cáliz"))
        assertEquals("ruzafa", normalizeText("Ruzafa"))
        assertEquals("cite des arts et des sciences", normalizeText("Cité des Arts et des Sciences"))
        assertEquals("oceanografic", normalizeText("Oceanogràfic"))
    }
}
