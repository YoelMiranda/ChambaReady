package pe.edu.esan.chambaready.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.edu.esan.chambaready.core.data.mock.MockContenidoRepository
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.ui.practicante.vacantes.FiltroVacantes
import pe.edu.esan.chambaready.ui.practicante.vacantes.VacantesViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** P08: orden por probabilidad, filtros, buscador y recálculo cuando cambian los niveles. */
@OptIn(ExperimentalCoroutinesApi::class)
class VacantesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zona = ZoneId.of("America/Lima")
    private val hoy = LocalDate.of(2026, 11, 18)
    private val reloj = Clock.fixed(hoy.atStartOfDay(zona).toInstant(), zona)
    private val servicio = ServicioPreparacion(MockContenidoRepository(reloj), reloj)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Estado en memoria: hace lo mismo que DataStore, sin Android. */
    private class EstadoFalso(inicial: EstadoUsuario) : EstadoUsuarioRepository {
        override val estado = MutableStateFlow(inicial)
        override suspend fun actualizar(cambio: (EstadoUsuario) -> EstadoUsuario) = estado.update(cambio)
        override suspend fun borrarTodo() { estado.value = EstadoUsuario() }
    }

    private class VacantesFalsas(private val vacantes: List<VacanteDetalle>, private val falla: Boolean = false) : VacanteRepository {
        override suspend fun listarVacantes(): List<VacanteDetalle> {
            if (falla) error("sin red")
            return vacantes
        }

        override suspend fun obtenerVacante(id: String) = vacantes.firstOrNull { it.resumen.id == id }
    }

    @Test
    fun cargar_ordenaPorProbabilidadDeMayorAMenor() = runTest(dispatcher) {
        val viewModel = VacantesViewModel(VacantesFalsas(MockDatos.vacantes(hoy)), EstadoFalso(MockDatos.estadoDemo(hoy)), servicio)
        advanceUntilIdle()

        val probabilidades = viewModel.uiState.value.vacantes.map { it.vacante.probabilidad }
        assertFalse(viewModel.uiState.value.cargando)
        assertEquals(probabilidades.sortedDescending(), probabilidades)
        assertEquals(5, probabilidades.size)
    }

    @Test
    fun filtrosYBuscador() = runTest(dispatcher) {
        val viewModel = VacantesViewModel(VacantesFalsas(MockDatos.vacantes(hoy)), EstadoFalso(MockDatos.estadoDemo(hoy)), servicio)
        advanceUntilIdle()

        viewModel.alternarFiltro(FiltroVacantes.MI_AREA)
        assertEquals(setOf("v1", "v5"), viewModel.uiState.value.vacantes.map { it.vacante.id }.toSet())

        viewModel.alternarFiltro(FiltroVacantes.CIERRA_PRONTO)
        assertEquals(listOf("v5"), viewModel.uiState.value.vacantes.map { it.vacante.id })

        viewModel.alternarFiltro(FiltroVacantes.MI_AREA)
        viewModel.alternarFiltro(FiltroVacantes.CIERRA_PRONTO)
        viewModel.buscar("android")
        assertEquals(listOf("v2"), viewModel.uiState.value.vacantes.map { it.vacante.id })
    }

    @Test
    fun probabilidadSeRecalculaAlSubirElNivel() = runTest(dispatcher) {
        val estado = EstadoFalso(MockDatos.estadoDemo(hoy))
        val viewModel = VacantesViewModel(VacantesFalsas(MockDatos.vacantes(hoy)), estado, servicio)
        advanceUntilIdle()
        val antes = viewModel.uiState.value.vacantes.first { it.vacante.id == "v1" }.vacante.probabilidad

        // Simula un mini-test aprobado de SQL a nivel intermedio (el filtro de v1).
        estado.actualizar { it.copy(niveles = it.niveles.copy(demostrados = mapOf("sql" to NivelHabilidad.INTERMEDIO))) }
        advanceUntilIdle()

        val despues = viewModel.uiState.value.vacantes.first { it.vacante.id == "v1" }.vacante.probabilidad
        assertTrue("antes=$antes, después=$despues", despues > antes)
    }

    @Test
    fun cargar_errorMuestraMensaje() = runTest(dispatcher) {
        val viewModel = VacantesViewModel(VacantesFalsas(emptyList(), falla = true), EstadoFalso(EstadoUsuario()), servicio)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.cargando)
        assertNotNull(viewModel.uiState.value.error)
    }
}
