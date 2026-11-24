package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.reglas.ReglasViabilidad
import java.time.DayOfWeek
import java.time.LocalDate

/** Regla 1: se suman las horas de cada día real desde hoy hasta el día anterior al cierre. */
class ReglasViabilidadTest {

    // Miércoles 18/11/2026.
    private val hoy = LocalDate.of(2026, 11, 18)

    // Lun 2 h, mié 1.5 h, sáb 4 h; el resto 0 h → 7.5 h por semana.
    private val horas = HorasLibres(
        mapOf(DayOfWeek.MONDAY to 2.0, DayOfWeek.WEDNESDAY to 1.5, DayOfWeek.SATURDAY to 4.0),
    )

    @Test
    fun horasDisponibles_sumaCadaDiaSegunSuDiaDeLaSemana() {
        // Del mié 18 al mar 1/12 (excluido): mié 18 y 25 (3 h), sáb 21 y 28 (8 h), lun 23 y 30 (4 h).
        val cierre = LocalDate.of(2026, 12, 1)
        assertEquals(15.0, ReglasViabilidad.horasDisponibles(hoy, cierre, horas), 0.0)
    }

    @Test
    fun horasDisponibles_hoyCuentaYElDiaDelCierreNo() {
        // Cierre el jueves 19: solo cuenta hoy miércoles.
        assertEquals(1.5, ReglasViabilidad.horasDisponibles(hoy, hoy.plusDays(1), horas), 0.0)
    }

    @Test
    fun horasDisponibles_cierreHoyOPasadoEsCero() {
        assertEquals(0.0, ReglasViabilidad.horasDisponibles(hoy, hoy, horas), 0.0)
        assertEquals(0.0, ReglasViabilidad.horasDisponibles(hoy, hoy.minusDays(3), horas), 0.0)
    }

    @Test
    fun evaluar_viableCuandoLasHorasAlcanzanJusto() {
        val resultado = ReglasViabilidad.evaluar(15.0, hoy, LocalDate.of(2026, 12, 1), horas)
        assertTrue(resultado.esViable)
        assertEquals(0.0, resultado.horasFaltantes, 0.0)
    }

    @Test
    fun evaluar_noViableIndicaHorasFaltantes() {
        val resultado = ReglasViabilidad.evaluar(20.0, hoy, LocalDate.of(2026, 12, 1), horas)
        assertFalse(resultado.esViable)
        assertEquals(5.0, resultado.horasFaltantes, 0.0)
    }

    @Test
    fun evaluar_sinHorasLibresNadaEsViableSalvoCero() {
        assertFalse(ReglasViabilidad.evaluar(0.5, hoy, hoy.plusDays(30), HorasLibres()).esViable)
        assertTrue(ReglasViabilidad.evaluar(0.0, hoy, hoy.plusDays(30), HorasLibres()).esViable)
    }
}
