package pe.edu.esan.chambaready.core.domain.reglas

import kotlin.math.ceil

/**
 * Regla 5: ranking PRIVADO dentro del área. Solo lo ve el propio practicante; nunca se muestra a
 * empresas. Se expresa como "Top X %": X pequeño = mejor.
 */
object ReglasRanking {

    /**
     * Posición del puntaje propio entre todos los del área (incluido el propio).
     *
     * Ejemplo: 10 estudiantes y 2 sacaron más que tú → eres el 3.º → Top 30 %.
     * Los empates cuentan a tu favor (no te ganan). Con un solo estudiante el resultado es 100.
     *
     * @param propio puntaje de 0.0 a 1.0 (correctas ÷ total).
     * @param otros puntajes de los demás estudiantes del área (el último de cada uno).
     */
    fun topPorcentaje(propio: Double, otros: List<Double>): Int {
        require(propio in 0.0..1.0) { "Puntaje fuera de rango: $propio" }
        val total = otros.size + 1
        val mejores = otros.count { it > propio }
        return ceil(100.0 * (mejores + 1) / total).toInt().coerceIn(1, 100)
    }

    /** Puntaje de una autoevaluación. */
    fun puntaje(correctas: Int, total: Int): Double {
        require(total > 0 && correctas in 0..total) { "Resultado inválido: $correctas de $total" }
        return correctas.toDouble() / total
    }
}
