package pe.edu.esan.chambaready.ui.practicante.vacantes

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Una vacante lista para pintar: probabilidad del usuario y días restantes ya calculados. */
data class VacanteItemUi(
    val vacante: VacanteResumen,
    val diasRestantes: Int,
)

/** Convierte una vacante en item de lista con la probabilidad calculada para este usuario. */
fun VacanteDetalle.aItem(servicio: ServicioPreparacion, estado: EstadoUsuario): VacanteItemUi =
    VacanteItemUi(
        vacante = resumen.copy(probabilidad = servicio.probabilidad(this, estado)),
        diasRestantes = servicio.diasRestantes(this),
    )

/** Filtros de P08. */
enum class FiltroVacantes(val etiqueta: String) {
    MI_AREA("Mi área"),
    REMOTA("Remota"),
    HIBRIDA("Híbrida"),
    PRESENCIAL("Presencial"),
    CIERRA_PRONTO("Cierra pronto"),
}

data class VacantesUiState(
    val cargando: Boolean = true,
    val todas: List<VacanteItemUi> = emptyList(),
    val areas: Map<String, Boolean> = emptyMap(),
    val busqueda: String = "",
    val filtros: Set<FiltroVacantes> = emptySet(),
    val error: String? = null,
) {
    /** Lista filtrada y ordenada por la probabilidad del usuario (de mayor a menor). */
    val vacantes: List<VacanteItemUi>
        get() = todas
            .filter { it.diasRestantes >= 0 }
            .filter { item ->
                val texto = busqueda.trim()
                texto.isEmpty() || item.vacante.puesto.contains(texto, true) || item.vacante.empresa.contains(texto, true)
            }
            .filter { item ->
                filtros.all { filtro ->
                    when (filtro) {
                        FiltroVacantes.MI_AREA -> areas[item.vacante.id] == true
                        FiltroVacantes.REMOTA -> item.vacante.modalidad == Modalidad.REMOTA
                        FiltroVacantes.HIBRIDA -> item.vacante.modalidad == Modalidad.HIBRIDA
                        FiltroVacantes.PRESENCIAL -> item.vacante.modalidad == Modalidad.PRESENCIAL
                        FiltroVacantes.CIERRA_PRONTO -> item.diasRestantes <= 7
                    }
                }
            }
            .sortedByDescending { it.vacante.probabilidad }
}

/** P08 Vacantes: buscador, filtros y orden por probabilidad. Se recalcula cuando cambian los niveles del usuario. */
@HiltViewModel
class VacantesViewModel @Inject constructor(
    private val repositorio: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<VacantesUiState, Nothing>(VacantesUiState()) {

    init {
        cargar()
    }

    fun cargar() {
        setState { copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val vacantes = repositorio.listarVacantes()
                estadoRepositorio.estado.collect { estado ->
                    setState {
                        copy(
                            cargando = false,
                            todas = vacantes.map { it.aItem(servicio, estado) },
                            areas = vacantes.associate { it.resumen.id to (it.area == estado.area) },
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(cargando = false, error = "No pudimos cargar las vacantes. Revisa tu conexión e inténtalo otra vez.") }
            }
        }
    }

    fun buscar(texto: String) = setState { copy(busqueda = texto) }

    fun alternarFiltro(filtro: FiltroVacantes) = setState { copy(filtros = if (filtro in filtros) filtros - filtro else filtros + filtro) }
}
