package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello

/** Lo que el practicante declara sobre un requisito en P17 ("Lo cumplo / No lo cumplo"). */
data class DeclaracionRequisito(
    val tipo: TipoRequisito,
    val loCumple: Boolean,
    /** True si hay evidencia validada o mini-test aprobado para esa habilidad. */
    val tieneEvidencia: Boolean = false,
)

/** Reglas 7 y 11: cuándo se puede postular y qué sello lleva cada requisito. */
object ReglasPostulacion {

    /**
     * Postular nunca se bloquea, salvo que el practicante declare no cumplir un requisito filtro.
     */
    fun puedePostular(declaraciones: List<DeclaracionRequisito>): Boolean =
        declaraciones.none { it.tipo == TipoRequisito.FILTRO && !it.loCumple }

    /**
     * Confirmado si hay evidencia o mini-test; Declarado si solo lo afirma; No cumple si dice que no.
     * Si la empresa no tiene evaluaciones (regla 11), las preguntas filtro no se envían y los
     * requisitos quedan como Declarado.
     */
    fun sello(declaracion: DeclaracionRequisito): TipoSello = when {
        !declaracion.loCumple -> TipoSello.NO_CUMPLE
        declaracion.tieneEvidencia -> TipoSello.CONFIRMADO
        else -> TipoSello.DECLARADO
    }
}
