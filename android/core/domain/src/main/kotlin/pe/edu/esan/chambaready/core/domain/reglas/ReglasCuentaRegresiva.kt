package pe.edu.esan.chambaready.core.domain.reglas

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Color de la cuenta regresiva de una vacante (regla de negocio 2). */
enum class UrgenciaCierre { ROJA, AMBAR, VERDE, CERRADA }

/**
 * Regla 2: ≤ 3 días en rojo, 4–7 días en ámbar y más de 7 en verde. Función pura para poder
 * probarla sin Android.
 */
object ReglasCuentaRegresiva {

    /** Días calendario desde [hoy] hasta [cierre]. Es negativo si la vacante ya cerró. */
    fun diasHastaCierre(hoy: LocalDate, cierre: LocalDate): Int =
        ChronoUnit.DAYS.between(hoy, cierre).toInt()

    fun urgencia(diasRestantes: Int): UrgenciaCierre = when {
        diasRestantes < 0 -> UrgenciaCierre.CERRADA
        diasRestantes <= 3 -> UrgenciaCierre.ROJA
        diasRestantes <= 7 -> UrgenciaCierre.AMBAR
        else -> UrgenciaCierre.VERDE
    }

    /** Texto corto para el chip: "Cierra hoy", "Cierra mañana", "Quedan 5 días" o "Cerrada". */
    fun texto(diasRestantes: Int): String = when {
        diasRestantes < 0 -> "Cerrada"
        diasRestantes == 0 -> "Cierra hoy"
        diasRestantes == 1 -> "Cierra mañana"
        else -> "Quedan $diasRestantes días"
    }
}
