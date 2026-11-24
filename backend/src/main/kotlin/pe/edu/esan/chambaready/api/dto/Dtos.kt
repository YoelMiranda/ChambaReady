package pe.edu.esan.chambaready.api.dto

import kotlinx.serialization.Serializable

/*
 * Objetos que viajan en JSON por la API /v1. Los nombres de los campos son el contrato con la app y
 * la web: si cambias uno, cambia también el cliente. Fechas en "AAAA-MM-DD" y horas en "HH:MM".
 */

// ---------- Públicos ----------
@Serializable data class SaludDto(val estado: String, val baseDatos: String, val version: String)
@Serializable data class AreaDto(val codigo: String, val nombre: String)
@Serializable data class HabilidadDto(val id: String, val nombre: String, val tipo: String)
@Serializable data class PreciosDto(val premiumCentimos: Long, val planEmpresaCentimos: Long, val evaluacionesGratis: Int)

// ---------- Practicante ----------
@Serializable
data class PerfilPracticanteDto(
    val nombre: String,
    val universidad: String,
    val carrera: String,
    val ciclo: Int?,
    val area: String?,
    val onboardingCompleto: Boolean,
    val premium: Boolean,
    /** id de habilidad → nivel declarado. */
    val nivelesDeclarados: Map<String, String>,
    /** id de habilidad → nivel demostrado (solo sube con mini-test o evidencia). */
    val nivelesDemostrados: Map<String, String>,
    /** MONDAY … SUNDAY → horas. */
    val horasLibres: Map<String, Double>,
    /** CV subido (F7), o null. */
    val cvArchivoId: String? = null,
)

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

@Serializable
data class RequisitoDto(val id: String, val habilidadId: String, val habilidad: String, val nivel: String, val tipo: String)

@Serializable
data class VacanteDto(
    val id: String,
    val puesto: String,
    val empresa: String,
    val area: String,
    val modalidad: String,
    val sueldo: String?,
    val fechaCierre: String,
    val diasRestantes: Int,
    val descripcion: String,
    val queHara: List<String>,
    val requisitos: List<RequisitoDto>,
    val evaluacionActiva: Boolean,
    /** Probabilidad del usuario que consulta (0–100). */
    val probabilidad: Int,
    /** true si al postular se responden preguntas filtro (la vacante evalúa, tiene preguntas y la empresa tiene saldo). */
    val conPreguntasFiltro: Boolean = false,
)

@Serializable data class CursoDto(val titulo: String, val proveedor: String, val url: String)

@Serializable
data class BrechaDto(
    val requisitoId: String,
    val habilidad: String,
    val tipo: String,
    val nivelActual: String,
    val nivelExigido: String,
    val horas: Double,
    /** Solo con Premium (regla 6): el resumen es gratis, el detalle y los cursos no. */
    val curso: CursoDto?,
)

@Serializable
data class DiagnosticoDto(
    val vacanteId: String,
    val probabilidadActual: Int,
    val probabilidadMeta: Int,
    val horasNecesarias: Double,
    val horasDisponibles: Double,
    val viable: Boolean,
    val detalleBloqueado: Boolean,
    val brechas: List<BrechaDto>,
)

/** Pregunta filtro tal como la ve el practicante: SIN la respuesta correcta. */
@Serializable data class PreguntaFiltroDto(val id: String, val enunciado: String, val opciones: List<String>, val segundos: Int)

@Serializable data class RespuestaFiltroDto(val preguntaId: String, val indice: Int? = null)

@Serializable
data class PostularDto(
    val vacanteId: String,
    /** id de requisito → "lo cumplo" (true) / "no lo cumplo" (false). Si falta, se asume que lo cumple. */
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
    val estado: String,
    val fecha: String,
    val evaluada: Boolean,
    val motivoDescarte: String?,
    val sellos: Map<String, String>,
    /** Regla 13: la empresa pidió responder las preguntas filtro (entró "Sin evaluar"). */
    val preguntasPendientes: Boolean = false,
)

// ---------- Empresa ----------
@Serializable
data class EmpresaDto(
    val razonSocial: String,
    val ruc: String,
    val sector: String = "",
    val web: String = "",
    val tamano: String = "",
    val contactoNombre: String = "",
    val contactoCorreo: String = "",
    val contactoTelefono: String = "",
    val descripcion: String = "",
    val verificacion: String = "EN_REVISION",
    /** Logo subido (F7); lo pone PUT /v1/empresa/logo, no PUT /v1/empresa/perfil. */
    val logoArchivoId: String? = null,
)

@Serializable
data class SaldoDto(
    val gratisRestantes: Int,
    val gratisTotales: Int,
    val planActivo: Boolean,
    /** Hasta cuándo dura el plan pagado (F8, pago simulado), o null. */
    val planVenceEn: String? = null,
)

@Serializable
data class CrearRequisitoDto(val habilidad: String, val nivel: String, val tipo: String, val filtro: Boolean = false)

@Serializable
data class CrearVacanteDto(
    val puesto: String,
    val area: String,
    val modalidad: String,
    val sueldo: String? = null,
    val fechaCierre: String,
    val descripcion: String,
    val queHara: List<String>,
    val requisitos: List<CrearRequisitoDto>,
    val evaluacionActiva: Boolean = true,
    /** Preguntas filtro revisadas por la empresa (F6). Si evalúa y no manda ninguna, se generan solas. */
    val preguntasFiltro: List<PreguntaNuevaDto> = emptyList(),
)

@Serializable
data class VacanteEmpresaDto(
    val id: String,
    val puesto: String,
    val area: String,
    val modalidad: String,
    val sueldo: String?,
    val descripcion: String,
    val queHara: List<String>,
    val creada: String,
    val fechaCierre: String,
    val diasParaCierre: Int,
    val estado: String,
    val evaluacionActiva: Boolean,
    val requisitos: List<RequisitoDto>,
    val postulantes: Int,
    val evaluados: Int,
    val confirmados: Int,
)

@Serializable
data class PostulanteDto(
    val id: String,
    val vacanteId: String,
    val nombre: String,
    val carrera: String,
    val universidad: String,
    val ciclo: Int?,
    val fecha: String,
    val evaluado: Boolean,
    val preguntasCorrectas: Int?,
    val sellos: Map<String, String>,
    val estado: String,
    val motivoDescarte: String?,
    /** % de ajuste a ESTA vacante (no existe ranking global). */
    val ajuste: Int,
    /** CV del postulante (F7): la empresa lo descarga con GET /v1/archivos/{id}. */
    val cvArchivoId: String? = null,
    /** Regla 13: ya le pidió responder las preguntas filtro y aún no responde. */
    val preguntasSolicitadas: Boolean = false,
)

@Serializable data class CambiarEstadoDto(val estado: String, val motivo: String? = null)

@Serializable
data class AgendarEntrevistaDto(
    val postulacionId: String,
    val fecha: String,
    val hora: String,
    val duracionMin: Int,
    val modalidad: String,
    val mensaje: String = "",
)

@Serializable
data class EntrevistaDto(
    val id: String,
    val postulacionId: String,
    val fecha: String,
    val hora: String,
    val duracionMin: Int,
    val modalidad: String,
    val mensaje: String,
)

// ---------- Administración ----------
@Serializable
data class CostoIaDto(val operacion: String, val modelo: String, val llamadas: Long, val tokensEntrada: Long, val tokensSalida: Long, val costoUsd: Double)

// ---------- Sesión (F5) ----------
@Serializable data class RegistroDto(val rol: String, val nombre: String = "")
@Serializable data class SesionDto(val registrado: Boolean, val rol: String?)

// ---------- Empresa: vistas para la web (F5) ----------
@Serializable
data class EntrevistaEmpresaDto(
    val id: String,
    val postulacionId: String,
    val vacanteId: String,
    val nombrePostulante: String,
    val puesto: String,
    val fecha: String,
    val hora: String,
    val duracionMin: Int,
    val modalidad: String,
    val mensaje: String,
)

@Serializable data class ActividadDto(val id: String, val texto: String, val fecha: String)

// ---------- F6: IA ----------
@Serializable data class AnalizarCvDto(val archivoId: String)
@Serializable data class HabilidadDetectadaDto(val habilidadId: String, val nombre: String, val nivel: String)

/** P02. [conIa] = false si no hay Gemini configurado (las habilidades se buscaron sin IA). */
@Serializable
data class CvAnalizadoDto(val archivoId: String, val habilidades: List<HabilidadDetectadaDto>, val proyectos: List<String>, val conIa: Boolean)

/** Pregunta tal como la ve quien responde: SIN la respuesta correcta. */
@Serializable data class PreguntaDto(val enunciado: String, val opciones: List<String>)

@Serializable
data class MiniTestDto(val id: String, val habilidadId: String, val habilidad: String, val nivel: String, val preguntas: List<PreguntaDto>)

@Serializable data class ResponderMiniTestDto(val respuestas: List<Int?>, val vacanteId: String? = null)

/** P16. [indicesCorrectos] se muestran DESPUÉS de responder, para aprender del error. */
@Serializable
data class ResultadoMiniTestDto(
    val correctas: Int,
    val total: Int,
    val aprobado: Boolean,
    val nivelAntes: String,
    val nivelDespues: String,
    val indicesCorrectos: List<Int>,
)

@Serializable data class AutoevaluacionDto(val area: String, val correctas: Int, val total: Int)

/** Ranking PRIVADO (regla 5): solo lo recibe el propio practicante. */
@Serializable data class RankingDto(val area: String, val topPorcentaje: Int, val topAnterior: Int?, val estudiantesEnArea: Int)

/** Pregunta filtro nueva. [requisitoIndice] = posición del requisito en la lista de la vacante. */
@Serializable data class PreguntaNuevaDto(val requisitoIndice: Int, val enunciado: String, val opciones: List<String>, val indiceCorrecto: Int)

@Serializable data class SugerirPreguntasDto(val puesto: String, val requisitos: List<CrearRequisitoDto>)

@Serializable data class PreguntasSugeridasDto(val preguntas: List<PreguntaNuevaDto>, val conIa: Boolean)

/** Pregunta filtro de una vacante propia, CON la respuesta (solo la ve su empresa). */
@Serializable
data class PreguntaFiltroEmpresaDto(val id: String, val requisitoId: String, val enunciado: String, val opciones: List<String>, val indiceCorrecto: Int)

@Serializable data class ResponderFiltroDto(val respuestas: List<RespuestaFiltroDto>)

// ---------- F7: archivos (guardados en la base, sin R2) ----------
@Serializable data class ArchivoDto(val id: String, val tipo: String, val nombre: String, val mime: String, val tamano: Int)
@Serializable data class LogoDto(val archivoId: String)

@Serializable
data class CrearEvidenciaDto(
    val habilidadId: String,
    val titulo: String,
    /** CERTIFICADO (con archivo) o LINK (con url). */
    val tipo: String,
    val nivel: String,
    val url: String? = null,
    val archivoId: String? = null,
)

@Serializable
data class EvidenciaDto(
    val id: String,
    val habilidadId: String,
    val habilidad: String,
    val titulo: String,
    val tipo: String,
    val nivel: String,
    val estado: String,
    val url: String?,
    val archivoId: String?,
    val fecha: String,
)

// ---------- F8: pagos simulados ----------
@Serializable data class PagoDto(val id: String, val concepto: String, val montoCentimos: Int, val fecha: String, val venceEn: String)

/** Plan Empresa o Premium: si está activo, hasta cuándo, su precio y los pagos de prueba hechos. */
@Serializable data class EstadoPlanDto(val activo: Boolean, val venceEn: String?, val precioCentimos: Long, val pagos: List<PagoDto>)

// ---------- F9: notificaciones y métricas ----------
@Serializable
data class NotificacionDto(
    val id: String,
    val tipo: String,
    val titulo: String,
    val cuerpo: String,
    val vacanteId: String?,
    val leida: Boolean,
    val fecha: String,
)

@Serializable data class ResumenNotificacionesDto(val noLeidas: Int)

@Serializable
data class MetricasDto(
    /** Eventos de los últimos 30 días por tipo (POSTULACION, VACANTE_PUBLICADA…). */
    val eventos30Dias: Map<String, Int>,
    val practicantes: Int,
    val empresas: Int,
    val vacantesAbiertas: Int,
    val postulaciones: Int,
    /** % de postulaciones evaluadas con preguntas filtro. */
    val porcentajeEvaluadas: Int,
    /** Adherencia de mini-tests: aprobados ÷ intentos (%). */
    val porcentajeMiniTestsAprobados: Int,
    val costoIaUsd: Double,
)
