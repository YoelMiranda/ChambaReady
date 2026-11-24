package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import java.time.LocalDate

/** Resultado de comparar las horas que pide un plan con las que el practicante tiene libres. */
data class ResultadoViabilidad(
    val horasNecesarias: Double,
    val horasDisponibles: Double,
) {
    /** Regla 1: es VIABLE si horasNecesarias ≤ horasDisponibles. */
    val esViable: Boolean get() = horasNecesarias <= horasDisponibles

    /** Horas que faltan para llegar a tiempo (0 si es viable). */
    val horasFaltantes: Double get() = (horasNecesarias - horasDisponibles).coerceAtLeast(0.0)
}

/**
 * Regla 1 (viabilidad). Se recorre el calendario día por día desde [hoy] (incluido) hasta el día
 * anterior al cierre y se suman las horas libres de cada día de la semana. El día del cierre no
 * se cuenta: se reserva para postular.
 */
object ReglasViabilidad {

    fun horasDisponibles(hoy: LocalDate, cierre: LocalDate, horasLibres: HorasLibres): Double {
        var total = 0.0
        var fecha = hoy
        while (fecha.isBefore(cierre)) {
            total += horasLibres.horas(fecha.dayOfWeek)
            fecha = fecha.plusDays(1)
        }
        return total
    }

    fun evaluar(
        horasNecesarias: Double,
        hoy: LocalDate,
        cierre: LocalDate,
        horasLibres: HorasLibres,
    ): ResultadoViabilidad = ResultadoViabilidad(
        horasNecesarias = horasNecesarias,
        horasDisponibles = horasDisponibles(hoy, cierre, horasLibres),
    )
}
