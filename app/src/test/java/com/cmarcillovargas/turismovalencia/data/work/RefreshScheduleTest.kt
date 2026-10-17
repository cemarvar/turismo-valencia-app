package com.cmarcillovargas.turismovalencia.data.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class RefreshScheduleTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun delayFrom(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, zone: ZoneId = madrid) =
        RefreshSchedule.delayUntilNextNight(ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone))

    @Test
    fun delDiaSeEsperaALaNocheSiguiente() {
        assertEquals(Duration.ofHours(13), delayFrom(2026, 10, 17, 14, 0))
    }

    @Test
    fun deMadrugadaSeEsperaAEsaMismaNoche() {
        assertEquals(Duration.ofMinutes(90), delayFrom(2026, 10, 17, 1, 30))
    }

    @Test
    fun alasTresEnPuntoYaNoCuentaEsaNoche() {
        assertEquals(Duration.ofHours(24), delayFrom(2026, 10, 17, 3, 0))
        assertEquals(Duration.ofHours(24).minusSeconds(1), delayFrom(2026, 10, 17, 3, 0, 1))
        assertEquals(Duration.ofSeconds(1), delayFrom(2026, 10, 17, 2, 59, 59))
    }

    @Test
    fun alCambiarAlHorarioDeVeranoLaNocheEsUnaHoraMasCorta() {
        // El 28 de marzo de 2027 a las 2:00 los relojes de Madrid pasan a las 3:00: de las 23:00 a las 3:00 solo pasan 3 horas.
        assertEquals(Duration.ofHours(3), delayFrom(2027, 3, 27, 23, 0))
        assertEquals(Duration.ofMinutes(30), delayFrom(2027, 3, 28, 1, 30))
    }

    @Test
    fun alVolverAlHorarioDeInviernoLaNocheEsUnaHoraMasLarga() {
        // El 25 de octubre de 2026 a las 3:00 los relojes de Madrid vuelven a las 2:00: de las 23:00 a las 3:00 pasan 5 horas.
        assertEquals(Duration.ofHours(5), delayFrom(2026, 10, 24, 23, 0))
    }

    @Test
    fun usaLaHoraDeLaZonaDelMovil() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        assertEquals(Duration.ofHours(7), delayFrom(2026, 10, 17, 20, 0, zone = losAngeles))
        // El mismo instante, visto desde Madrid (5:00 del día 18), es de día para el reloj de Madrid: faltan 22 horas.
        val sameInstant = ZonedDateTime.of(2026, 10, 17, 20, 0, 0, 0, losAngeles).withZoneSameInstant(madrid)
        assertEquals(Duration.ofHours(22), RefreshSchedule.delayUntilNextNight(sameInstant))
    }

    @Test
    fun laEsperaNuncaPasaDeUnDiaNiEsNegativa() {
        var moment = ZonedDateTime.of(2026, 10, 17, 0, 0, 0, 0, madrid)
        repeat(24 * 4) {
            val delay = RefreshSchedule.delayUntilNextNight(moment)
            assertTrue("a las $moment la espera es $delay", !delay.isNegative && !delay.isZero && delay <= Duration.ofHours(24))
            moment = moment.plusMinutes(15)
        }
    }

    @Test
    fun seReintentaHastaAgotarLosIntentosSoloSiFallo() {
        assertFalse(RefreshSchedule.shouldRetry(refreshed = true, runAttemptCount = 0))
        assertTrue(RefreshSchedule.shouldRetry(refreshed = false, runAttemptCount = 0))
        assertTrue(RefreshSchedule.shouldRetry(refreshed = false, runAttemptCount = RefreshSchedule.MaxAttempts - 2))
        assertFalse(RefreshSchedule.shouldRetry(refreshed = false, runAttemptCount = RefreshSchedule.MaxAttempts - 1))
        assertFalse(RefreshSchedule.shouldRetry(refreshed = false, runAttemptCount = RefreshSchedule.MaxAttempts + 5))
    }
}
