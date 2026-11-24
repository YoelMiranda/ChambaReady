package pe.edu.esan.chambaready.core.domain.reglas

/** Datos mínimos de un plan activo para ordenar Mis pendientes (P14). */
data class PlanActivo(
    val vacanteId: String,
    val diasRestantes: Int,
    /** Avance de 0 a 1. */
    val avance: Double,
)

/**
 * P14: los planes se ordenan por prioridad. Primero el cierre más cercano; con el mismo cierre,
 * primero el que lleva menos avance. Las vacantes ya cerradas van al final.
 */
object ReglasPrioridad {

    fun ordenar(planes: List<PlanActivo>): List<PlanActivo> =
        planes.sortedWith(
            compareBy<PlanActivo> { it.diasRestantes < 0 }
                .thenBy { it.diasRestantes }
                .thenBy { it.avance },
        )
}
