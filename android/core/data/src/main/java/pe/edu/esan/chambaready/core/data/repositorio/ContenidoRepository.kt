package pe.edu.esan.chambaready.core.data.repositorio

import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.CursoExterno
import pe.edu.esan.chambaready.core.domain.modelo.Habilidad
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import java.time.LocalDateTime

/**
 * Contenido que en F1–F3 viene de datos de prueba y desde F4 de la API (y de Gemini en F6):
 * catálogos, preguntas, cursos y notificaciones.
 */
interface ContenidoRepository {
    fun universidades(): List<String>

    fun habilidad(id: String): Habilidad?

    /** P02: habilidades que la IA "leyó" en el CV (en F6 las devuelve Gemini). */
    fun habilidadesDetectadasEnCv(): List<Habilidad>

    fun proyectosDetectadosEnCv(): List<String>

    /** Curso externo recomendado por habilidad (id de habilidad → curso). */
    fun cursos(): Map<String, CursoExterno>

    /** P15: 3 preguntas del módulo (= habilidad). */
    fun miniTest(moduloId: String): List<PreguntaOpcion>

    /** P05: preguntas del área elegida. */
    fun preguntasAutoevaluacion(area: AreaInteres): List<PreguntaOpcion>

    /** P24: las 2 preguntas filtro de la vacante. */
    fun preguntasFiltro(vacanteId: String): List<PreguntaOpcion>

    fun notificaciones(ahora: LocalDateTime): List<Notificacion>

    /** Precio de Premium en céntimos (tabla de configuración). */
    fun precioPremiumCentimos(): Long

    /** Ranking privado estimado: "Top X %" dentro del área. En F4 lo calcula el backend con datos reales. */
    fun topPorcentaje(area: AreaInteres, correctas: Int, total: Int): Int
}
