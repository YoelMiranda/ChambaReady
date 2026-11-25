package pe.edu.esan.chambaready.core.data.remoto

import kotlinx.serialization.Serializable

/*
 * JSON que viaja por la API /v1 (copia del contrato de backend/.../dto/Dtos.kt).
 * Solo están los objetos que usa la app del practicante. Si el backend cambia un nombre de campo,
 * hay que cambiarlo aquí también. Fechas en "AAAA-MM-DD".
 */

// ---------- Sesión ----------
@Serializable data class SesionDto(val registrado: Boolean, val rol: String? = null)

/**
 * POST /v1/registro. La app siempre manda PRACTICANTE; el rol no se puede cambiar después.
 * Sin valor por defecto a propósito: kotlinx.serialization no envía los campos que tienen su valor
 * por defecto, y el backend necesita el rol.
 */
@Serializable data class RegistroDto(val nombre: String, val rol: String)

/** Cuerpo de error del backend: {"codigo": "...", "mensaje": "..."}. */
@Serializable data class ErrorDto(val codigo: String, val mensaje: String)

// ---------- Perfil ----------
@Serializable
data class PerfilPracticanteDto(
    val nombre: String,
    val universidad: String,
    val carrera: String,
    val ciclo: Int? = null,
    val area: String? = null,
    val onboardingCompleto: Boolean,
    val premium: Boolean,
    val nivelesDeclarados: Map<String, String> = emptyMap(),
    val nivelesDemostrados: Map<String, String> = emptyMap(),
    /** MONDAY … SUNDAY → horas. */
    val horasLibres: Map<String, Double> = emptyMap(),
    val cvArchivoId: String? = null,
)

/** PUT /v1/yo/perfil. Los campos null no se cambian. */
@Serializable
data class ActualizarPerfilDto(
    val nombre: String? = null,
    val universidad: String? = null,
    val carrera: String? = null,
    val ciclo: Int? = null,
    val area: String? = null,
    val onboardingCompleto: Boolean? = null,
    val nivelesDeclarados: Map<String, String>? = null,
)

@Serializable data class HorasLibresDto(val horas: Map<String, Double>)

// ---------- Vacantes ----------
@Serializable
data class RequisitoDto(val id: String, val habilidadId: String, val habilidad: String, val nivel: String, val tipo: String)

@Serializable
data class VacanteDto(
    val id: String,
    val puesto: String,
    val empresa: String,
    val area: String,
    val modalidad: String,
    val sueldo: String? = null,
    val fechaCierre: String,
    val diasRestantes: Int,
    val descripcion: String,
    val queHara: List<String>,
    val requisitos: List<RequisitoDto>,
    val evaluacionActiva: Boolean,
    /** Probabilidad del usuario que consulta (0–100), calculada por el backend. */
    val probabilidad: Int,
    /** true si al postular se responden preguntas filtro (F6). */
    val conPreguntasFiltro: Boolean = false,
)

// ---------- Postulaciones ----------
@Serializable data class RespuestaFiltroDto(val preguntaId: String, val indice: Int? = null)

@Serializable
data class PostularDto(
    val vacanteId: String,
    /** id de requisito → true si declara que lo cumple. */
    val declaraciones: Map<String, Boolean> = emptyMap(),
    val respuestasFiltro: List<RespuestaFiltroDto> = emptyList(),
    val declaracionJurada: Boolean,
)

@Serializable
data class PostulacionDto(
    val id: String,
    val vacanteId: String,
    val puesto: String,
    val empresa: String,
    /** Estado tal como lo maneja la empresa: NUEVA, REVISION, ENTREVISTA, ELEGIDA o DESCARTADA. */
    val estado: String,
    val fecha: String,
    val evaluada: Boolean,
    val motivoDescarte: String? = null,
    val sellos: Map<String, String> = emptyMap(),
    /** Regla 13: la empresa pidió responder las preguntas filtro. */
    val preguntasPendientes: Boolean = false,
)

// ---------- F6: IA (CV, mini-test, ranking, preguntas filtro) ----------
/** Pregunta filtro SIN la respuesta correcta (el backend corrige). */
@Serializable data class PreguntaFiltroDto(val id: String, val enunciado: String, val opciones: List<String>, val segundos: Int = 45)

@Serializable data class ResponderFiltroDto(val respuestas: List<RespuestaFiltroDto>)

@Serializable data class PreguntaDto(val enunciado: String, val opciones: List<String>)

@Serializable
data class MiniTestDto(val id: String, val habilidadId: String, val habilidad: String, val nivel: String, val preguntas: List<PreguntaDto>)

@Serializable data class ResponderMiniTestDto(val respuestas: List<Int?>, val vacanteId: String? = null)

@Serializable
data class ResultadoMiniTestDto(
    val correctas: Int,
    val total: Int,
    val aprobado: Boolean,
    val nivelAntes: String,
    val nivelDespues: String,
    val indicesCorrectos: List<Int> = emptyList(),
)

@Serializable data class AutoevaluacionDto(val area: String, val correctas: Int, val total: Int)

/** Ranking PRIVADO (regla 5). */
@Serializable data class RankingDto(val area: String, val topPorcentaje: Int, val topAnterior: Int? = null, val estudiantesEnArea: Int = 1)

@Serializable data class AnalizarCvDto(val archivoId: String)

@Serializable data class HabilidadDetectadaDto(val habilidadId: String, val nombre: String, val nivel: String)

@Serializable
data class CvAnalizadoDto(val archivoId: String, val habilidades: List<HabilidadDetectadaDto>, val proyectos: List<String>, val conIa: Boolean)

// ---------- F7: archivos ----------
@Serializable data class ArchivoDto(val id: String, val tipo: String, val nombre: String, val mime: String, val tamano: Int)

// ---------- F8: pagos simulados ----------
@Serializable data class PagoDto(val id: String, val concepto: String, val montoCentimos: Int, val fecha: String, val venceEn: String)

@Serializable data class EstadoPlanDto(val activo: Boolean, val venceEn: String? = null, val precioCentimos: Long, val pagos: List<PagoDto> = emptyList())

// ---------- F9: notificaciones ----------
@Serializable
data class NotificacionDto(
    val id: String,
    val tipo: String,
    val titulo: String,
    val cuerpo: String,
    val vacanteId: String? = null,
    val leida: Boolean,
    /** Instante ISO-8601 (por ejemplo 2026-11-24T13:00:00Z). */
    val fecha: String,
)

// ---------- Evaluación de habilidades y asistente ----------
@Serializable data class IniciarEvaluacionDto(val habilidades: List<String>)

@Serializable data class EvaluacionDto(val id: String, val pruebas: List<MiniTestDto>)

@Serializable data class EstadoEvaluacionDto(val puedeEvaluar: Boolean, val usadas: Int = 0, val premium: Boolean = false)

@Serializable data class MensajeAsistenteDto(val mensaje: String, val habilidades: Map<String, String> = emptyMap())

@Serializable
data class RespuestaAsistenteDto(val respuesta: String, val habilidades: List<HabilidadDetectadaDto> = emptyList(), val conIa: Boolean = false)
