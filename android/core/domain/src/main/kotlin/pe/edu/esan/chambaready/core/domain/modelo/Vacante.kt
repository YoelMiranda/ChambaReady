package pe.edu.esan.chambaready.core.domain.modelo

import java.time.LocalDate

/**
 * Datos de una vacante para listas y tarjetas (P07, P08). [probabilidad] es la del usuario actual,
 * de 0 a 100. En F1–F3 la calcula la app con `MotorDiagnostico`; desde F4, el backend.
 */
data class VacanteResumen(
    val id: String,
    val puesto: String,
    val empresa: String,
    val modalidad: Modalidad,
    /** Sueldo mensual ya formateado (por ejemplo "S/ 1,200"), o null si no se publica. */
    val sueldo: String?,
    val fechaCierre: LocalDate,
    val probabilidad: Int,
)

/** Un requisito publicado por la empresa (E03). */
data class RequisitoVacante(
    val id: String,
    val habilidadId: String,
    val nombre: String,
    val nivelExigido: NivelHabilidad,
    val tipo: TipoRequisito,
)

/** Detalle completo de una vacante (P09). */
data class VacanteDetalle(
    val resumen: VacanteResumen,
    val area: AreaInteres,
    val descripcion: String,
    val queHaras: List<String>,
    val requisitos: List<RequisitoVacante>,
    /** La empresa activó la evaluación automática (preguntas filtro). */
    val evaluacionActiva: Boolean,
    /** La empresa tiene evaluaciones (gratis o plan). Si no, no se envían preguntas (regla 11). */
    val empresaConEvaluaciones: Boolean,
)

/** Curso externo recomendado para cerrar una brecha (regla 8: ChambaReady no imparte cursos). */
data class CursoExterno(
    val habilidadId: String,
    val titulo: String,
    val proveedor: String,
    val url: String,
    /** Horas estimadas para subir un nivel con este curso. */
    val horasPorNivel: Double,
)

/** Pregunta de opción múltiple: autoevaluación (P05), mini-test (P15) y preguntas filtro (P24). */
data class PreguntaOpcion(
    val id: String,
    val enunciado: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    /** Fragmento de código opcional (se muestra en JetBrains Mono). */
    val codigo: String? = null,
    /** En las preguntas filtro: el requisito que confirma si se responde bien. */
    val requisitoId: String? = null,
    /** En la autoevaluación: la habilidad que mide. */
    val habilidadId: String? = null,
) {
    fun esCorrecta(indiceElegido: Int?): Boolean = indiceElegido == indiceCorrecto
}
