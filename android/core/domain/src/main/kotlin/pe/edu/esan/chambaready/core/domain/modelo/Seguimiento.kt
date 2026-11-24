package pe.edu.esan.chambaready.core.domain.modelo

import java.time.LocalDate
import java.time.LocalDateTime

/** Estado de una postulación (P18). [paso] ubica el estado en la línea Enviada → Revisión → Entrevista → Resultado. */
enum class EstadoPostulacion(val etiqueta: String, val paso: Int) {
    ENVIADA("Enviada", 0),
    EN_REVISION("En revisión", 1),
    ENTREVISTA("Entrevista", 2),
    SELECCIONADO("Seleccionado", 3),
    NO_SELECCIONADO("No seleccionado", 3),
    ;

    val cerrada: Boolean get() = this == SELECCIONADO || this == NO_SELECCIONADO
}

data class Postulacion(
    val vacanteId: String,
    val puesto: String,
    val empresa: String,
    val estado: EstadoPostulacion,
    val fechaEnvio: LocalDate,
    /** Motivo que escribió la empresa al descartar (obligatorio en E04); el practicante lo ve. */
    val motivoDescarte: String? = null,
    /** Sello de cada requisito al momento de postular (id de requisito → sello). */
    val sellos: Map<String, TipoSello> = emptyMap(),
    /** Id en el backend (null en el modo demo). */
    val id: String? = null,
    /** Regla 13: la empresa pidió responder las preguntas filtro y aún no responde. */
    val preguntasPendientes: Boolean = false,
)

/** Tipos de notificación de P20. Cada uno abre su propia pantalla. */
enum class TipoNotificacion(val etiqueta: String) {
    CIERRE_CERCANO("Cierre cercano"),
    CAMBIO_ESTADO("Cambio de estado"),
    ENTREVISTA("Entrevista agendada"),
    PLAN_SEMANAL("Plan semanal"),
    NUEVA_VACANTE("Nueva vacante"),
    RANKING("Ranking"),

    /** F9: la empresa pidió responder las preguntas filtro después de postular (regla 13). */
    PREGUNTAS_FILTRO("Preguntas filtro"),

    /** F8: se registró un pago simulado. */
    PAGO("Pagos"),

    /** F7: el equipo validó una evidencia. */
    EVIDENCIA("Evidencias"),
}

data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val cuerpo: String,
    val fecha: LocalDateTime,
    /** Vacante relacionada, si la hay. */
    val vacanteId: String? = null,
)

enum class TipoEvidencia(val etiqueta: String) {
    CERTIFICADO("Certificado"),
    LINK("Link"),
}

/** Evidencia del practicante (P23). Solo una evidencia validada puede subir el nivel demostrado. */
data class Evidencia(
    val id: String,
    val titulo: String,
    val habilidadId: String,
    val tipo: TipoEvidencia,
    val estado: EstadoEvidencia,
    /** Nivel que respalda la evidencia una vez validada. */
    val nivel: NivelHabilidad,
)
