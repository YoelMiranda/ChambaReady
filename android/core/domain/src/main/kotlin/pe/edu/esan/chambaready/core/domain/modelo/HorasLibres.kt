package pe.edu.esan.chambaready.core.domain.modelo

import java.time.DayOfWeek

/**
 * Horas libres del practicante para cada día de la semana (P04 y P22). Es inmutable: [sumar],
 * [restar] y [con] devuelven una copia nueva. Los días que no aparecen en [porDia] valen 0 h.
 */
data class HorasLibres(val porDia: Map<DayOfWeek, Double> = emptyMap()) {

    init {
        porDia.forEach { (dia, horas) ->
            require(horasValidas(horas)) { "Horas inválidas para $dia: $horas (0 a 8, en pasos de 0.5)" }
        }
    }

    fun horas(dia: DayOfWeek): Double = porDia[dia] ?: 0.0

    /** Total semanal que se muestra en vivo en P04 y P22. */
    val totalSemanal: Double get() = DayOfWeek.entries.sumOf { horas(it) }

    /** Botón "+": suma media hora sin pasar de 8 h. */
    fun sumar(dia: DayOfWeek): HorasLibres = con(dia, (horas(dia) + PASO).coerceAtMost(HORAS_MAXIMAS_POR_DIA))

    /** Botón "−": resta media hora sin bajar de 0 h. */
    fun restar(dia: DayOfWeek): HorasLibres = con(dia, (horas(dia) - PASO).coerceAtLeast(0.0))

    fun con(dia: DayOfWeek, horas: Double): HorasLibres = HorasLibres(porDia + (dia to horas))

    fun puedeSumar(dia: DayOfWeek): Boolean = horas(dia) < HORAS_MAXIMAS_POR_DIA
    fun puedeRestar(dia: DayOfWeek): Boolean = horas(dia) > 0.0

    companion object {
        const val HORAS_MAXIMAS_POR_DIA = 8.0
        const val PASO = 0.5

        /** True si [horas] está entre 0 y 8 y es múltiplo de 0.5. */
        fun horasValidas(horas: Double): Boolean =
            horas in 0.0..HORAS_MAXIMAS_POR_DIA && (horas / PASO) % 1.0 == 0.0
    }
}
