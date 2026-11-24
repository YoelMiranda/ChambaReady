package pe.edu.esan.chambaready.core.domain.modelo

/** Datos del formulario de registro (C02). */
data class DatosRegistro(
    val nombre: String = "",
    val correo: String = "",
    val universidad: String = "",
    val carrera: String = "",
    val ciclo: Int = 0,
)

enum class TipoHabilidad(val etiqueta: String) {
    TECNICA("Técnica"),
    BLANDA("Blanda"),
}

/** Una habilidad del catálogo (tabla `habilidades`). */
data class Habilidad(
    val id: String,
    val nombre: String,
    val tipo: TipoHabilidad,
)

/**
 * Niveles del practicante por id de habilidad. El declarado lo elige él (P02/P03); el demostrado
 * solo sube con mini-test aprobado o evidencia validada (regla 4, ver `ReglasNivel`).
 */
data class NivelesUsuario(
    val declarados: Map<String, NivelHabilidad> = emptyMap(),
    val demostrados: Map<String, NivelHabilidad> = emptyMap(),
) {
    fun declarado(habilidadId: String): NivelHabilidad = declarados[habilidadId] ?: NivelHabilidad.NO_LO_HE_USADO

    fun demostrado(habilidadId: String): NivelHabilidad = demostrados[habilidadId] ?: NivelHabilidad.NO_LO_HE_USADO

    /** Nivel con el que se compara cada requisito en P09 y P10: el mayor entre declarado y demostrado. */
    fun efectivo(habilidadId: String): NivelHabilidad {
        val declarado = declarado(habilidadId)
        val demostrado = demostrado(habilidadId)
        return if (demostrado.valor >= declarado.valor) demostrado else declarado
    }
}
