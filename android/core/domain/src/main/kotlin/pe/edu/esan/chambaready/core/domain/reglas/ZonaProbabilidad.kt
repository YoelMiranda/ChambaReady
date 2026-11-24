package pe.edu.esan.chambaready.core.domain.reglas

/**
 * Regla 3: zona Baja (< 40 %), Media (40–69 %) y Alta (≥ 70 %). La app solo clasifica el
 * porcentaje que manda el backend para pintarlo; el cálculo de la probabilidad vive en la API (F4).
 */
enum class ZonaProbabilidad(val etiqueta: String, val desde: Int, val hasta: Int) {
    BAJA("Baja", 0, 40),
    MEDIA("Media", 40, 70),
    ALTA("Alta", 70, 100),
    ;

    /** Ancho relativo de la zona en la escala (40, 30 y 30). */
    val ancho: Int get() = hasta - desde

    companion object {
        /** Los valores fuera de 0–100 se ajustan al extremo más cercano. */
        fun de(porcentaje: Int): ZonaProbabilidad {
            val p = porcentaje.coerceIn(0, 100)
            return when {
                p < 40 -> BAJA
                p < 70 -> MEDIA
                else -> ALTA
            }
        }
    }
}
