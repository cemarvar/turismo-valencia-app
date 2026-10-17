package com.cmarcillovargas.turismovalencia.data.notify

import com.cmarcillovargas.turismovalencia.data.fake.FakeEventos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReminderPlannerTest {
    private val madrid = ReminderPlanner.Valencia
    private val mascleta = EventReminders.Mascleta

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, zone: ZoneId = madrid): Instant =
        ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone).toInstant()

    /** La ventana de la mascletà empieza a las 13:20 y acaba a las 13:30 (14:00 − 30 min de antelación). */
    private fun windowOf(year: Int, month: Int, day: Int) = at(year, month, day, 13, 20)

    @Test
    fun antesDeLasFallasElAvisoEsElPrimeroDeMarzo() {
        assertEquals(windowOf(2027, 3, 1), ReminderPlanner.next(mascleta, at(2026, 10, 14, 12, 0)))
        // Pasadas las Fallas, el del año siguiente (2028 es bisiesto: el rango no depende de febrero).
        assertEquals(windowOf(2028, 3, 1), ReminderPlanner.next(mascleta, at(2027, 12, 31, 23, 59)))
    }

    @Test
    fun duranteLasFallasEsElDeHoySiLaVentanaNoHaEmpezado() {
        assertEquals(windowOf(2027, 3, 5), ReminderPlanner.next(mascleta, at(2027, 3, 5, 10, 0)))
        assertEquals(windowOf(2027, 3, 19), ReminderPlanner.next(mascleta, at(2027, 3, 19, 0, 0)))
    }

    @Test
    fun conLaVentanaYaEmpezadaEsElDelDiaSiguiente() {
        assertEquals(windowOf(2027, 3, 6), ReminderPlanner.next(mascleta, at(2027, 3, 5, 13, 21)))
        assertEquals(windowOf(2027, 3, 6), ReminderPlanner.next(mascleta, at(2027, 3, 5, 18, 0)))
    }

    @Test
    fun laVentanaQueEstaSaltandoNuncaSeRepite() {
        val start = windowOf(2027, 3, 5)
        // Justo en el inicio ya cuenta como pasada (el aviso que acaba de saltar no se programa otra vez)…
        assertEquals(windowOf(2027, 3, 6), ReminderPlanner.next(mascleta, start))
        // …y un instante antes todavía es la de hoy.
        assertEquals(start, ReminderPlanner.next(mascleta, start.minusMillis(1)))
    }

    @Test
    fun elUltimoDiaDeLasFallasDaPasoAlAnoSiguiente() {
        assertEquals(windowOf(2027, 3, 19), ReminderPlanner.next(mascleta, at(2027, 3, 19, 9, 0)))
        assertEquals(windowOf(2028, 3, 1), ReminderPlanner.next(mascleta, at(2027, 3, 19, 13, 30)))
    }

    @Test
    fun laHoraEsLaDeValenciaSeaCualSeaLaZonaDelMovil() {
        // Las 12:00 del 5 de marzo en California son las 21:00 en Valencia: la ventana de ese día (13:20) ya pasó.
        val inCalifornia = at(2027, 3, 5, 12, 0, zone = ZoneId.of("America/Los_Angeles"))
        assertEquals(windowOf(2027, 3, 6), ReminderPlanner.next(mascleta, inCalifornia))
        // El mismo instante expresado en otra zona da el mismo resultado, y en UTC la ventana empieza a las 12:20 (CET = UTC+1).
        assertEquals(ReminderPlanner.next(mascleta, inCalifornia), ReminderPlanner.next(mascleta, inCalifornia.atZone(ZoneOffset.UTC).toInstant()))
        assertEquals(LocalDateTime.of(2027, 3, 6, 12, 20), LocalDateTime.ofInstant(windowOf(2027, 3, 6), ZoneOffset.UTC))
    }

    @Test
    fun lasHorasLocalesSeMantienenAlCambiarAlHorarioDeVerano() {
        // El 28 de marzo de 2027 se pasa a horario de verano: la ventana sigue siendo a las 13:20 locales (UTC+1 → UTC+2).
        val reminder = mascleta.copy(firstDay = MonthDay.of(3, 27), lastDay = MonthDay.of(3, 29))
        val before = ReminderPlanner.windowStart(reminder, LocalDate.of(2027, 3, 27))
        val after = ReminderPlanner.windowStart(reminder, LocalDate.of(2027, 3, 29))
        assertEquals(LocalDateTime.of(2027, 3, 27, 12, 20), LocalDateTime.ofInstant(before, ZoneOffset.UTC))
        assertEquals(LocalDateTime.of(2027, 3, 29, 11, 20), LocalDateTime.ofInstant(after, ZoneOffset.UTC))
        assertEquals(LocalTime.of(13, 20), before.atZone(madrid).toLocalTime())
        assertEquals(LocalTime.of(13, 20), after.atZone(madrid).toLocalTime())
    }

    @Test
    fun unActoDeMadrugadaAvisaElDiaAnterior() {
        // Acto a las 00:10: la ventana empieza a las 23:30 de la víspera.
        val night = mascleta.copy(time = LocalTime.of(0, 10), firstDay = MonthDay.of(3, 1), lastDay = MonthDay.of(3, 3))
        assertEquals(at(2027, 2, 28, 23, 30), ReminderPlanner.next(night, at(2027, 2, 28, 23, 0)))
        assertEquals(at(2027, 3, 1, 23, 30), ReminderPlanner.next(night, at(2027, 2, 28, 23, 40)))
    }

    @Test
    fun enCualquierMomentoDelAnoElSiguienteAvisoEstaEnElRangoYEsPosterior() {
        var now = at(2026, 1, 1, 0, 0)
        val end = at(2028, 12, 31, 0, 0)
        while (now.isBefore(end)) {
            val next = ReminderPlanner.next(mascleta, now)
            val local = next.atZone(madrid)
            assertTrue("$now → $next", next.isAfter(now))
            assertTrue("$now → $next: fuera de las Fallas", mascleta.occursOn(MonthDay.from(local)))
            assertEquals(LocalTime.of(13, 20), local.toLocalTime())
            assertTrue("$now → $next: tarda más de un año", Duration.between(now, next) <= Duration.ofDays(366))
            now = now.plus(Duration.ofHours(7).plusMinutes(11))
        }
    }

    @Test
    fun laVentanaDuraDiezMinutosYAcabaALosTreintaMinutosAntesDelActo() {
        assertEquals(10L, ReminderPlanner.WindowMinutes)
        val start = ReminderPlanner.next(mascleta, at(2027, 3, 1, 0, 0)).atZone(madrid)
        val end = start.plusMinutes(ReminderPlanner.WindowMinutes)
        // Android suele entregar la alarma al final de la ventana: el aviso nunca llega con menos de 30 min de antelación.
        assertEquals(LocalTime.of(13, 20), start.toLocalTime())
        assertEquals(LocalTime.of(14, 0).minusMinutes(mascleta.leadMinutes.toLong()), end.toLocalTime())
    }

    @Test
    fun elCatalogoCoincideConLasFichas() {
        assertEquals(mascleta, EventReminders.of("fallas"))
        assertNull(EventReminders.of("mercado-central"))
        // El aviso pertenece a una ficha que existe y cuyo primer acto es la mascletà (el texto de la ficha lo dice).
        val fallas = FakeEventos.firstOrNull { it.id == mascleta.itemId }
        assertNotNull(fallas)
        assertEquals("Mascletà", fallas!!.acts.first().title)
        assertEquals("1–19 mar · 14:00", fallas.acts.first().whenLabel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unRangoAlReves_noSeAcepta() {
        mascleta.copy(firstDay = MonthDay.of(3, 19), lastDay = MonthDay.of(3, 1))
    }
}
