package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import java.time.DayOfWeek

/** P04/P22: horas distintas por día, de 0 a 8 h en pasos de 0.5 h, con total semanal en vivo. */
class HorasLibresTest {

    @Test
    fun diasSinDatoValenCero() {
        assertEquals(0.0, HorasLibres().horas(DayOfWeek.FRIDAY), 0.0)
        assertEquals(0.0, HorasLibres().totalSemanal, 0.0)
    }

    @Test
    fun sumarYRestar_mediaHoraPorToque() {
        val horas = HorasLibres().sumar(DayOfWeek.MONDAY).sumar(DayOfWeek.MONDAY).sumar(DayOfWeek.TUESDAY)
        assertEquals(1.0, horas.horas(DayOfWeek.MONDAY), 0.0)
        assertEquals(0.5, horas.horas(DayOfWeek.TUESDAY), 0.0)
        assertEquals(0.5, horas.restar(DayOfWeek.MONDAY).horas(DayOfWeek.MONDAY), 0.0)
    }

    @Test
    fun limites_noBajaDeCeroNiPasaDeOcho() {
        val enCero = HorasLibres().restar(DayOfWeek.SUNDAY)
        assertEquals(0.0, enCero.horas(DayOfWeek.SUNDAY), 0.0)
        assertFalse(enCero.puedeRestar(DayOfWeek.SUNDAY))

        val enOcho = HorasLibres(mapOf(DayOfWeek.SUNDAY to 8.0)).sumar(DayOfWeek.SUNDAY)
        assertEquals(8.0, enOcho.horas(DayOfWeek.SUNDAY), 0.0)
        assertFalse(enOcho.puedeSumar(DayOfWeek.SUNDAY))
    }

    @Test
    fun totalSemanal_sumaLosSieteDias() {
        val horas = HorasLibres(DayOfWeek.entries.associateWith { 1.5 })
        assertEquals(10.5, horas.totalSemanal, 0.0)
    }

    @Test
    fun horasValidas_pasoYRango() {
        assertTrue(HorasLibres.horasValidas(0.0))
        assertTrue(HorasLibres.horasValidas(7.5))
        assertTrue(HorasLibres.horasValidas(8.0))
        assertFalse(HorasLibres.horasValidas(8.5))
        assertFalse(HorasLibres.horasValidas(1.25))
        assertFalse(HorasLibres.horasValidas(-0.5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_rechazaHorasInvalidas() {
        HorasLibres(mapOf(DayOfWeek.MONDAY to 9.0))
    }
}
