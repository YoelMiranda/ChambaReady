package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.CursoExterno
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.RequisitoVacante
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import java.time.LocalDate

/** Algo que le falta al practicante para un requisito (P10). */
data class Brecha(
    val requisito: RequisitoVacante,
    val nivelActual: NivelHabilidad,
    val horas: Double,
    val curso: CursoExterno?,
)

/** Resultado de P10. El resumen (brechas, horas, viabilidad, probabilidades) es gratis; el detalle es Premium. */
data class Diagnostico(
    val vacanteId: String,
    val brechas: List<Brecha>,
    val viabilidad: ResultadoViabilidad,
    val probabilidadActual: Int,
    /** Probabilidad si cierra todas las brechas. */
    val probabilidadMeta: Int,
) {
    val horasNecesarias: Double get() = viabilidad.horasNecesarias
}

/** Una tarea marcable del plan (P12): una sesión de estudio de un curso externo. */
data class TareaPlan(
    val id: String,
    /** El módulo es la habilidad que se trabaja; cada módulo tiene su mini-test. */
    val moduloId: String,
    val titulo: String,
    val horas: Double,
    val curso: CursoExterno?,
)

data class SemanaPlan(
    val numero: Int,
    val inicio: LocalDate,
    val tareas: List<TareaPlan>,
) {
    val horas: Double get() = tareas.sumOf { it.horas }
}

data class PlanPreparacion(
    val vacanteId: String,
    val semanas: List<SemanaPlan>,
) {
    val tareas: List<TareaPlan> get() = semanas.flatMap { it.tareas }

    /** Avance de 0 a 1 según las horas de las tareas marcadas. */
    fun avance(tareasCompletadas: Set<String>): Double {
        val total = tareas.sumOf { it.horas }
        if (total == 0.0) return 0.0
        return tareas.filter { it.id in tareasCompletadas }.sumOf { it.horas } / total
    }

    /** "Hoy toca": la primera tarea sin marcar, o null si el plan está terminado. */
    fun siguienteTarea(tareasCompletadas: Set<String>): TareaPlan? = tareas.firstOrNull { it.id !in tareasCompletadas }
}

/**
 * Motor del diagnóstico y del plan (P09, P10, P12, P13). Funciones puras: reciben la vacante, los
 * niveles del practicante, los cursos y sus horas libres, y devuelven el resultado.
 */
object MotorDiagnostico {

    /** Horas por nivel si un requisito no tiene curso en el catálogo. */
    const val HORAS_POR_NIVEL_SIN_CURSO = 8.0

    /** Las tareas del plan se parten en sesiones de 2 horas como máximo. */
    const val HORAS_MAXIMAS_POR_SESION = 2.0

    /**
     * Cumple si su nivel efectivo llega al exigido; Parcial si le falta un nivel; Falta en otro caso.
     * Un requisito básico sin ninguna experiencia es "Falta" (no "Parcial").
     */
    fun cumplimiento(requisito: RequisitoVacante, niveles: NivelesUsuario): CumplimientoRequisito {
        val actual = niveles.efectivo(requisito.habilidadId).valor
        val exigido = requisito.nivelExigido.valor
        return when {
            actual >= exigido -> CumplimientoRequisito.CUMPLE
            actual > 0 && exigido - actual == 1 -> CumplimientoRequisito.PARCIAL
            else -> CumplimientoRequisito.FALTA
        }
    }

    fun probabilidad(vacante: VacanteDetalle, niveles: NivelesUsuario): Int =
        ReglasProbabilidad.calcular(
            vacante.requisitos.map { RequisitoEvaluado(it.tipo, cumplimiento(it, niveles)) },
        )

    /** Brechas ordenadas por importancia (filtro, obligatorio, deseable). */
    fun brechas(vacante: VacanteDetalle, niveles: NivelesUsuario, cursos: Map<String, CursoExterno>): List<Brecha> =
        vacante.requisitos
            .filter { cumplimiento(it, niveles) != CumplimientoRequisito.CUMPLE }
            .sortedBy { it.tipo.ordinal }
            .map { requisito ->
                val actual = niveles.efectivo(requisito.habilidadId)
                val curso = cursos[requisito.habilidadId]
                val nivelesQueFaltan = (requisito.nivelExigido.valor - actual.valor).coerceAtLeast(1)
                Brecha(
                    requisito = requisito,
                    nivelActual = actual,
                    horas = nivelesQueFaltan * (curso?.horasPorNivel ?: HORAS_POR_NIVEL_SIN_CURSO),
                    curso = curso,
                )
            }

    fun diagnosticar(
        vacante: VacanteDetalle,
        niveles: NivelesUsuario,
        cursos: Map<String, CursoExterno>,
        horasLibres: HorasLibres,
        hoy: LocalDate,
    ): Diagnostico {
        val brechas = brechas(vacante, niveles, cursos)
        return Diagnostico(
            vacanteId = vacante.resumen.id,
            brechas = brechas,
            viabilidad = ReglasViabilidad.evaluar(
                horasNecesarias = brechas.sumOf { it.horas },
                hoy = hoy,
                cierre = vacante.resumen.fechaCierre,
                horasLibres = horasLibres,
            ),
            probabilidadActual = probabilidad(vacante, niveles),
            probabilidadMeta = ReglasProbabilidad.calcular(
                vacante.requisitos.map { RequisitoEvaluado(it.tipo, CumplimientoRequisito.CUMPLE) },
            ),
        )
    }

    /**
     * Arma el plan por semanas. Cada brecha se parte en sesiones de hasta 2 h y se reparten en
     * orden, sin pasar las horas libres de cada semana real (desde [hoy]). Si una sesión no entra
     * entera, se parte entre dos semanas. Pasado el cierre se sigue con el total semanal, para las
     * vacantes guardadas como meta. Sin horas libres no hay plan.
     */
    fun generarPlan(diagnostico: Diagnostico, horasLibres: HorasLibres, hoy: LocalDate, cierre: LocalDate): PlanPreparacion {
        if (horasLibres.totalSemanal == 0.0) return PlanPreparacion(diagnostico.vacanteId, emptyList())

        // Sesiones pendientes, en orden de importancia.
        val pendientes = ArrayDeque<Pair<Brecha, Double>>()
        diagnostico.brechas.forEach { brecha ->
            var resto = brecha.horas
            while (resto > 0) {
                val horas = minOf(resto, HORAS_MAXIMAS_POR_SESION)
                pendientes.addLast(brecha to horas)
                resto -= horas
            }
        }

        val semanas = mutableListOf<SemanaPlan>()
        val contadorPorModulo = mutableMapOf<String, Int>()
        var numero = 1
        while (pendientes.isNotEmpty()) {
            val inicio = hoy.plusWeeks((numero - 1).toLong())
            val fin = inicio.plusDays(7)
            var capacidad = if (inicio.isBefore(cierre)) {
                ReglasViabilidad.horasDisponibles(inicio, minOf(fin, cierre), horasLibres)
            } else {
                horasLibres.totalSemanal
            }
            val tareas = mutableListOf<TareaPlan>()
            while (capacidad > 0 && pendientes.isNotEmpty()) {
                val (brecha, horas) = pendientes.removeFirst()
                val asignadas = minOf(horas, capacidad)
                if (asignadas < horas) pendientes.addFirst(brecha to horas - asignadas)
                capacidad -= asignadas
                val modulo = brecha.requisito.habilidadId
                val n = (contadorPorModulo[modulo] ?: 0) + 1
                contadorPorModulo[modulo] = n
                tareas += TareaPlan(
                    id = "${diagnostico.vacanteId}-$modulo-$n",
                    moduloId = modulo,
                    titulo = "${brecha.requisito.nombre}: sesión $n",
                    horas = asignadas,
                    curso = brecha.curso,
                )
            }
            semanas += SemanaPlan(numero, inicio, tareas)
            numero++
        }
        // Las semanas sin capacidad (por ejemplo, la última antes del cierre) quedan vacías y se quitan.
        return PlanPreparacion(diagnostico.vacanteId, semanas.filter { it.tareas.isNotEmpty() }.mapIndexed { i, s -> s.copy(numero = i + 1) })
    }

    /** Requisitos filtro que el practicante no cumple: bloquean la postulación si declara no cumplirlos (regla 7). */
    fun filtrosNoCumplidos(vacante: VacanteDetalle, niveles: NivelesUsuario): List<RequisitoVacante> =
        vacante.requisitos.filter { it.tipo == TipoRequisito.FILTRO && cumplimiento(it, niveles) != CumplimientoRequisito.CUMPLE }
}
