package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import pe.edu.esan.chambaready.core.domain.reglas.DeclaracionRequisito
import pe.edu.esan.chambaready.core.domain.reglas.ReglasNivel
import pe.edu.esan.chambaready.core.domain.reglas.ReglasPostulacion

/** Reglas 4 y 7: nivel demostrado y bloqueo de postulación. */
class ReglasPostulacionYNivelTest {

    @Test
    fun postular_soloSeBloqueaPorUnFiltroNoCumplido() {
        val faltaDeseable = listOf(
            DeclaracionRequisito(TipoRequisito.OBLIGATORIO, loCumple = false),
            DeclaracionRequisito(TipoRequisito.DESEABLE, loCumple = false),
        )
        assertTrue(ReglasPostulacion.puedePostular(faltaDeseable))

        val faltaFiltro = listOf(DeclaracionRequisito(TipoRequisito.FILTRO, loCumple = false))
        assertFalse(ReglasPostulacion.puedePostular(faltaFiltro))
    }

    @Test
    fun sello_segunEvidenciaYDeclaracion() {
        assertEquals(TipoSello.CONFIRMADO, ReglasPostulacion.sello(DeclaracionRequisito(TipoRequisito.FILTRO, true, tieneEvidencia = true)))
        assertEquals(TipoSello.DECLARADO, ReglasPostulacion.sello(DeclaracionRequisito(TipoRequisito.FILTRO, true)))
        assertEquals(TipoSello.NO_CUMPLE, ReglasPostulacion.sello(DeclaracionRequisito(TipoRequisito.DESEABLE, false, tieneEvidencia = true)))
    }

    @Test
    fun miniTest_apruebaConDosDeTres() {
        assertFalse(ReglasNivel.miniTestAprobado(1))
        assertTrue(ReglasNivel.miniTestAprobado(2))
        assertTrue(ReglasNivel.miniTestAprobado(3))
    }

    @Test
    fun miniTest_subeNivelSoloSiAprueba() {
        assertEquals(NivelHabilidad.INTERMEDIO, ReglasNivel.trasMiniTest(NivelHabilidad.BASICO, NivelHabilidad.INTERMEDIO, correctas = 2))
        assertEquals(NivelHabilidad.BASICO, ReglasNivel.trasMiniTest(NivelHabilidad.BASICO, NivelHabilidad.INTERMEDIO, correctas = 1))
    }

    @Test
    fun nivel_nuncaBaja() {
        assertEquals(NivelHabilidad.AVANZADO, ReglasNivel.trasMiniTest(NivelHabilidad.AVANZADO, NivelHabilidad.BASICO, correctas = 3))
    }

    @Test
    fun evidencia_soloValidadaSubeNivel() {
        assertEquals(NivelHabilidad.AVANZADO, ReglasNivel.trasEvidencia(NivelHabilidad.BASICO, NivelHabilidad.AVANZADO, EstadoEvidencia.VALIDADA))
        assertEquals(NivelHabilidad.BASICO, ReglasNivel.trasEvidencia(NivelHabilidad.BASICO, NivelHabilidad.AVANZADO, EstadoEvidencia.EN_REVISION))
        assertEquals(NivelHabilidad.BASICO, ReglasNivel.trasEvidencia(NivelHabilidad.BASICO, NivelHabilidad.AVANZADO, EstadoEvidencia.LEIDO))
    }

    @Test(expected = IllegalArgumentException::class)
    fun miniTest_rechazaResultadoImposible() {
        ReglasNivel.miniTestAprobado(4)
    }
}
