package pe.edu.esan.chambaready.core.domain.modelo

/** Roles que asigna el backend. En la app Android solo inicia sesión el practicante. */
enum class RolUsuario { PRACTICANTE, EMPRESA }

/**
 * Área de interés principal del practicante (P21). La autoevaluación, el ranking privado y las
 * recomendaciones se calculan dentro de esta área.
 */
enum class AreaInteres(val etiqueta: String) {
    DATA_ANALYTICS("Data y Analytics"),
    DESARROLLO_SOFTWARE("Desarrollo de software"),
    MARKETING_DIGITAL("Marketing digital"),
    FINANZAS("Finanzas"),
    OPERACIONES_LOGISTICA("Operaciones y logística"),
    DISENO_UX_UI("Diseño UX/UI"),
    RECURSOS_HUMANOS("Recursos humanos"),
    VENTAS_COMERCIAL("Ventas y comercial"),
}

/**
 * Nivel de una habilidad. [valor] alimenta las barras de nivel (0 a 3). `NO_LO_SE` se dibuja
 * como 0, pero conserva su propia etiqueta para no confundirlo con "No lo he usado".
 */
enum class NivelHabilidad(val etiqueta: String, val valor: Int) {
    NO_LO_HE_USADO("No lo he usado", 0),
    BASICO("Básico", 1),
    INTERMEDIO("Intermedio", 2),
    AVANZADO("Avanzado", 3),
    NO_LO_SE("No lo sé", 0),
    ;

    companion object {
        /** Niveles que una empresa puede exigir en un requisito (E03). */
        val exigibles = listOf(BASICO, INTERMEDIO, AVANZADO)
    }
}

/**
 * Tipo de requisito de una vacante. Un requisito marcado como filtro en E03 se guarda como
 * [FILTRO] y además es obligatorio. El peso de cada tipo está en `ReglasProbabilidad`.
 */
enum class TipoRequisito(val etiqueta: String) {
    FILTRO("Filtro"),
    OBLIGATORIO("Obligatorio"),
    DESEABLE("Deseable"),
}

/** Cómo cumple el practicante un requisito (P09). */
enum class CumplimientoRequisito(val etiqueta: String) {
    CUMPLE("Cumples"),
    PARCIAL("Parcial"),
    FALTA("Te falta"),
}

/** Sello de un requisito en una postulación (P17, E04). Siempre se muestra con texto. */
enum class TipoSello(val etiqueta: String) {
    CONFIRMADO("Confirmado"),
    DECLARADO("Declarado"),
    NO_CUMPLE("No cumple"),
}

/** Estado de una evidencia (P23). Solo una evidencia VALIDADA puede subir el nivel demostrado. */
enum class EstadoEvidencia(val etiqueta: String) {
    VALIDADA("Validada"),
    EN_REVISION("En revisión"),
    LEIDO("Leído"),
}

/** Modalidad de trabajo de una vacante. */
enum class Modalidad(val etiqueta: String) {
    PRESENCIAL("Presencial"),
    HIBRIDA("Híbrida"),
    REMOTA("Remota"),
}
