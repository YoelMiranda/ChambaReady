package pe.edu.esan.chambaready.core.data.servicio

import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.reglas.Diagnostico
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import pe.edu.esan.chambaready.core.domain.reglas.PlanPreparacion
import pe.edu.esan.chambaready.core.domain.reglas.ReglasCuentaRegresiva
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Junta las reglas del dominio con el estado del usuario y el contenido, para que varias pantallas
 * (P07, P08, P09, P10, P12, P13, P14, P16, P17, P22) calculen lo mismo de la misma forma.
 * Desde F4, la probabilidad y el diagnóstico los devolverá el backend.
 */
@Singleton
class ServicioPreparacion @Inject constructor(
    private val contenido: ContenidoRepository,
    private val reloj: Clock,
) {
    fun hoy(): LocalDate = LocalDate.now(reloj)

    fun diasRestantes(vacante: VacanteDetalle): Int =
        ReglasCuentaRegresiva.diasHastaCierre(hoy(), vacante.resumen.fechaCierre)

    fun probabilidad(vacante: VacanteDetalle, estado: EstadoUsuario): Int =
        MotorDiagnostico.probabilidad(vacante, estado.nivelesEfectivos)

    fun diagnostico(vacante: VacanteDetalle, estado: EstadoUsuario): Diagnostico =
        MotorDiagnostico.diagnosticar(vacante, estado.nivelesEfectivos, contenido.cursos(), estado.horasLibres, hoy())

    fun plan(vacante: VacanteDetalle, estado: EstadoUsuario): PlanPreparacion =
        MotorDiagnostico.generarPlan(diagnostico(vacante, estado), estado.horasLibres, hoy(), vacante.resumen.fechaCierre)

    /**
     * Sello de cada requisito si postulara ahora (P17), según lo que declara en [loCumple]:
     * Confirmado si hay evidencia, mini-test o pregunta filtro bien respondida; Declarado si solo
     * lo afirma; No cumple si dice que no.
     */
    fun sellos(vacante: VacanteDetalle, estado: EstadoUsuario, loCumple: Map<String, Boolean>): Map<String, TipoSello> {
        val respuestas = estado.respuestasFiltro[vacante.resumen.id].orEmpty()
        val preguntas = contenido.preguntasFiltro(vacante.resumen.id)
        val confirmadosPorFiltro = preguntas.mapIndexedNotNull { i, pregunta ->
            pregunta.requisitoId?.takeIf { respuestas.getOrNull(i) == true }
        }.toSet()
        return vacante.requisitos.associate { requisito ->
            val confirmado = requisito.id in confirmadosPorFiltro || estado.tieneEvidencia(requisito.habilidadId, requisito.nivelExigido)
            val sello = when {
                confirmado -> TipoSello.CONFIRMADO
                loCumple[requisito.id] == false -> TipoSello.NO_CUMPLE
                else -> TipoSello.DECLARADO
            }
            requisito.id to sello
        }
    }
}
