package pe.edu.esan.chambaready.core.data.repositorio

import kotlinx.coroutines.flow.Flow
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.Evidencia
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.core.domain.reglas.ReglasNivel

/** Resultado del último mini-test, para mostrarlo en P16. */
data class ResultadoMiniTest(
    val vacanteId: String,
    val moduloId: String,
    val nombreHabilidad: String,
    val correctas: Int,
    val total: Int,
    val nivelAntes: NivelHabilidad,
    val nivelDespues: NivelHabilidad,
) {
    val aprobado: Boolean get() = ReglasNivel.miniTestAprobado(correctas, total)
}

/** Resultado de la autoevaluación (P05/P06). El ranking es privado y dentro del área (regla 5). */
data class ResultadoAutoevaluacion(
    val area: AreaInteres,
    val correctas: Int,
    val total: Int,
    /** "Top X %" dentro del área. */
    val topPorcentaje: Int,
    /** Top anterior, para mostrar el cambio en P07; null la primera vez. */
    val topAnterior: Int? = null,
)

/**
 * Todo lo que el practicante cambia en la app. Se guarda en DataStore, así que se ve en todas las
 * pantallas y sobrevive al cerrar la app. Desde F4 parte de esto vendrá de la API.
 */
data class EstadoUsuario(
    val registro: DatosRegistro? = null,
    val onboardingCompleto: Boolean = false,
    val cvNombre: String? = null,
    val horasLibres: HorasLibres = HorasLibres(),
    val area: AreaInteres? = null,
    val premium: Boolean = false,
    val niveles: NivelesUsuario = NivelesUsuario(),
    val evidencias: List<Evidencia> = emptyList(),
    val tareasCompletadas: Set<String> = emptySet(),
    /** Vacantes con plan abierto (se recalculan al cambiar las horas libres en P22). */
    val planesActivos: Set<String> = emptySet(),
    /** Vacantes guardadas como meta sin fecha (P13). */
    val metas: Set<String> = emptySet(),
    val notificacionesLeidas: Set<String> = emptySet(),
    val postulaciones: List<Postulacion> = emptyList(),
    /** Respuestas de las preguntas filtro por vacante: true si acertó cada pregunta (P24). */
    val respuestasFiltro: Map<String, List<Boolean>> = emptyMap(),
    /** Probabilidad la primera vez que vio el diagnóstico, para el "antes" de P16. */
    val probabilidadInicial: Map<String, Int> = emptyMap(),
    val ultimoMiniTest: ResultadoMiniTest? = null,
    val autoevaluacion: ResultadoAutoevaluacion? = null,
    /** Interruptores de notificaciones de P19. */
    val avisosActivos: Map<TipoNotificacion, Boolean> = TipoNotificacion.entries.associateWith { true },
) {
    /**
     * Niveles con las evidencias validadas ya aplicadas al nivel demostrado (regla 4). Es lo que
     * deben usar las pantallas para calcular cumplimiento y probabilidad.
     */
    val nivelesEfectivos: NivelesUsuario
        get() {
            val demostrados = niveles.demostrados.toMutableMap()
            evidencias.filter { it.estado == EstadoEvidencia.VALIDADA }.forEach { evidencia ->
                val actual = demostrados[evidencia.habilidadId] ?: NivelHabilidad.NO_LO_HE_USADO
                demostrados[evidencia.habilidadId] = ReglasNivel.trasEvidencia(actual, evidencia.nivel, evidencia.estado)
            }
            return niveles.copy(demostrados = demostrados)
        }

    /** True si hay una evidencia validada o un mini-test aprobado que respalda ese nivel. */
    fun tieneEvidencia(habilidadId: String, nivelExigido: NivelHabilidad): Boolean =
        nivelesEfectivos.demostrado(habilidadId).valor >= nivelExigido.valor && nivelExigido.valor > 0
}

/** Estado del usuario: se lee como Flow y se cambia con [actualizar], que es atómico. */
interface EstadoUsuarioRepository {
    val estado: Flow<EstadoUsuario>

    suspend fun actualizar(cambio: (EstadoUsuario) -> EstadoUsuario)

    /** Cerrar sesión en F2: borra los datos de prueba del teléfono. */
    suspend fun borrarTodo()
}
