package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito.CUMPLE
import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito.FALTA
import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito.PARCIAL
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito.DESEABLE
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito.FILTRO
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito.OBLIGATORIO
import pe.edu.esan.chambaready.core.domain.reglas.ReglasProbabilidad
import pe.edu.esan.chambaready.core.domain.reglas.RequisitoEvaluado

/** Regla 3: 100 × Σ(peso × puntaje) / Σ(peso), con tope de 39 % si falta un filtro. */
class ReglasProbabilidadTest {

    @Test
    fun cumpleTodo_es100() {
        val requisitos = listOf(RequisitoEvaluado(FILTRO, CUMPLE), RequisitoEvaluado(DESEABLE, CUMPLE))
        assertEquals(100, ReglasProbabilidad.calcular(requisitos))
    }

    @Test
    fun ponderaPorTipo() {
        // Pesos 3 + 2 + 1 = 6. Obtiene 3 (filtro) + 1 (obligatorio parcial) + 0 = 4 → 67 %.
        val requisitos = listOf(
            RequisitoEvaluado(FILTRO, CUMPLE),
            RequisitoEvaluado(OBLIGATORIO, PARCIAL),
            RequisitoEvaluado(DESEABLE, FALTA),
        )
        assertEquals(67, ReglasProbabilidad.calcular(requisitos))
    }

    @Test
    fun faltaUnFiltro_seLimitaA39() {
        // Sin el tope serían (0 + 2 + 2 + 1) / 8 = 63 %.
        val requisitos = listOf(
            RequisitoEvaluado(FILTRO, FALTA),
            RequisitoEvaluado(OBLIGATORIO, CUMPLE),
            RequisitoEvaluado(OBLIGATORIO, CUMPLE),
            RequisitoEvaluado(DESEABLE, CUMPLE),
        )
        assertEquals(39, ReglasProbabilidad.calcular(requisitos))
    }

    @Test
    fun filtroParcial_noActivaElTope() {
        // (1.5 + 2) / 5 = 70 %.
        val requisitos = listOf(RequisitoEvaluado(FILTRO, PARCIAL), RequisitoEvaluado(OBLIGATORIO, CUMPLE))
        assertEquals(70, ReglasProbabilidad.calcular(requisitos))
    }

    @Test
    fun sinRequisitos_es100() {
        assertEquals(100, ReglasProbabilidad.calcular(emptyList()))
    }
}
