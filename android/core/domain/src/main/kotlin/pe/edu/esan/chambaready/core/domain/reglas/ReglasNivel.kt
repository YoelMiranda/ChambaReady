package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad

/**
 * Regla 4: el nivel demostrado sube SOLO con un mini-test aprobado o una evidencia validada.
 * Terminar un curso no cambia el nivel, y por eso aquí no existe una función para eso.
 * El nivel demostrado nunca baja por estas reglas.
 */
object ReglasNivel {

    const val PREGUNTAS_MINI_TEST = 3
    const val MINIMO_CORRECTAS = 2

    /** P15: se aprueba con 2 de 3 respuestas correctas o más. */
    fun miniTestAprobado(correctas: Int, total: Int = PREGUNTAS_MINI_TEST): Boolean {
        require(total > 0 && correctas in 0..total) { "Resultado inválido: $correctas de $total" }
        return correctas >= MINIMO_CORRECTAS
    }

    fun trasMiniTest(actual: NivelHabilidad, nivelDelModulo: NivelHabilidad, correctas: Int): NivelHabilidad =
        if (miniTestAprobado(correctas)) mayor(actual, nivelDelModulo) else actual

    fun trasEvidencia(actual: NivelHabilidad, nivelDeLaEvidencia: NivelHabilidad, estado: EstadoEvidencia): NivelHabilidad =
        if (estado == EstadoEvidencia.VALIDADA) mayor(actual, nivelDeLaEvidencia) else actual

    private fun mayor(a: NivelHabilidad, b: NivelHabilidad): NivelHabilidad = if (b.valor > a.valor) b else a
}
