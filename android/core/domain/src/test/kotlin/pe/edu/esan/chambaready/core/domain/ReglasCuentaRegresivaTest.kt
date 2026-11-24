package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.reglas.ReglasCuentaRegresiva
import pe.edu.esan.chambaready.core.domain.reglas.UrgenciaCierre
import java.time.LocalDate

/** Regla 2: ≤ 3 días rojo, 4–7 ámbar, > 7 verde. */
class ReglasCuentaRegresivaTest {

    private val hoy = LocalDate.of(2026, 11, 18)

    @Test
    fun diasHastaCierre_cuentaDiasCalendario() {
        assertEquals(0, ReglasCuentaRegresiva.diasHastaCierre(hoy, hoy))
        assertEquals(3, ReglasCuentaRegresiva.diasHastaCierre(hoy, LocalDate.of(2026, 11, 21)))
        assertEquals(-1, ReglasCuentaRegresiva.diasHastaCierre(hoy, LocalDate.of(2026, 11, 17)))
    }

    @Test
    fun diasHastaCierre_cruzaFinDeMesYAnio() {
        assertEquals(14, ReglasCuentaRegresiva.diasHastaCierre(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 8)))
    }

    @Test
    fun urgencia_limitesDeCadaColor() {
        assertEquals(UrgenciaCierre.ROJA, ReglasCuentaRegresiva.urgencia(0))
        assertEquals(UrgenciaCierre.ROJA, ReglasCuentaRegresiva.urgencia(3))
        assertEquals(UrgenciaCierre.AMBAR, ReglasCuentaRegresiva.urgencia(4))
        assertEquals(UrgenciaCierre.AMBAR, ReglasCuentaRegresiva.urgencia(7))
        assertEquals(UrgenciaCierre.VERDE, ReglasCuentaRegresiva.urgencia(8))
        assertEquals(UrgenciaCierre.VERDE, ReglasCuentaRegresiva.urgencia(60))
    }

    @Test
    fun urgencia_negativoEsCerrada() {
        assertEquals(UrgenciaCierre.CERRADA, ReglasCuentaRegresiva.urgencia(-1))
    }

    @Test
    fun texto_casosEspeciales() {
        assertEquals("Cierra hoy", ReglasCuentaRegresiva.texto(0))
        assertEquals("Cierra mañana", ReglasCuentaRegresiva.texto(1))
        assertEquals("Quedan 5 días", ReglasCuentaRegresiva.texto(5))
        assertEquals("Cerrada", ReglasCuentaRegresiva.texto(-2))
    }
}
