package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import kotlin.math.roundToInt

/** Un requisito de la vacante con lo que el practicante cumple de él. */
data class RequisitoEvaluado(
    val tipo: TipoRequisito,
    val cumplimiento: CumplimientoRequisito,
)

/**
 * Regla 3: probabilidad transparente (no es un modelo de ML). Se explica en `docs/formula-probabilidad.md`.
 *
 * probabilidad = 100 × Σ(peso × puntaje) / Σ(peso)
 * - peso: Filtro 3, Obligatorio 2, Deseable 1
 * - puntaje: Cumple 1, Parcial 0.5, Falta 0
 * - Si falta (no "parcial") algún requisito filtro, el resultado se limita a 39 % (zona Baja).
 * - Una vacante sin requisitos da 100 %.
 */
object ReglasProbabilidad {

    const val TOPE_SI_FALTA_FILTRO = 39

    fun peso(tipo: TipoRequisito): Int = when (tipo) {
        TipoRequisito.FILTRO -> 3
        TipoRequisito.OBLIGATORIO -> 2
        TipoRequisito.DESEABLE -> 1
    }

    fun puntaje(cumplimiento: CumplimientoRequisito): Double = when (cumplimiento) {
        CumplimientoRequisito.CUMPLE -> 1.0
        CumplimientoRequisito.PARCIAL -> 0.5
        CumplimientoRequisito.FALTA -> 0.0
    }

    fun calcular(requisitos: List<RequisitoEvaluado>): Int {
        if (requisitos.isEmpty()) return 100
        val pesoTotal = requisitos.sumOf { peso(it.tipo) }
        val obtenido = requisitos.sumOf { peso(it.tipo) * puntaje(it.cumplimiento) }
        val porcentaje = (100.0 * obtenido / pesoTotal).roundToInt()
        val faltaUnFiltro = requisitos.any {
            it.tipo == TipoRequisito.FILTRO && it.cumplimiento == CumplimientoRequisito.FALTA
        }
        return if (faltaUnFiltro) minOf(porcentaje, TOPE_SI_FALTA_FILTRO) else porcentaje
    }
}
