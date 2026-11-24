package pe.edu.esan.chambaready.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.formatoHoras
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import pe.edu.esan.chambaready.core.domain.reglas.PlanPreparacion
import pe.edu.esan.chambaready.core.domain.reglas.ResultadoViabilidad
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BloqueoPremium
import pe.edu.esan.chambaready.ui.comun.CargandoPantalla
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.Etiqueta
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import pe.edu.esan.chambaready.ui.comun.formatoFecha
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class PlanUiState(
    val vacante: VacanteDetalle? = null,
    val plan: PlanPreparacion? = null,
    val viabilidad: ResultadoViabilidad? = null,
    val completadas: Set<String> = emptySet(),
    val premium: Boolean = true,
) {
    val avance: Double get() = plan?.avance(completadas) ?: 0.0
}

/** P12 Plan (Premium). Las tareas marcadas se guardan y se ven en Inicio, Pendientes y Mi progreso. */
@HiltViewModel
class PlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<PlanUiState, Nothing>(PlanUiState()) {

    private val vacanteId: String = checkNotNull(savedStateHandle["vacanteId"])

    init {
        viewModelScope.launch {
            val vacante = vacantes.obtenerVacante(vacanteId) ?: return@launch
            estadoRepositorio.estado.collect { e ->
                if (e.premium && vacanteId !in e.planesActivos) {
                    estadoRepositorio.actualizar { it.copy(planesActivos = it.planesActivos + vacanteId) }
                }
                setState {
                    copy(
                        vacante = vacante,
                        plan = servicio.plan(vacante, e),
                        viabilidad = servicio.diagnostico(vacante, e).viabilidad,
                        completadas = e.tareasCompletadas,
                        premium = e.premium,
                    )
                }
            }
        }
    }

    fun alternarTarea(tareaId: String) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { e ->
                val nuevas = if (tareaId in e.tareasCompletadas) e.tareasCompletadas - tareaId else e.tareasCompletadas + tareaId
                e.copy(tareasCompletadas = nuevas)
            }
        }
    }
}

@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onMiniTest: (vacanteId: String, moduloId: String) -> Unit,
    onProgreso: (String) -> Unit,
    onPremium: (String) -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PlanContenido(estado, onBack, viewModel::alternarTarea, onMiniTest, onProgreso, onPremium)
}

@Composable
fun PlanContenido(
    estado: PlanUiState,
    onBack: () -> Unit,
    onTarea: (String) -> Unit,
    onMiniTest: (String, String) -> Unit,
    onProgreso: (String) -> Unit,
    onPremium: (String) -> Unit,
) {
    val vacante = estado.vacante
    val plan = estado.plan
    val viabilidad = estado.viabilidad
    PantallaBase(titulo = "Mi plan", onBack = onBack) { padding ->
        if (vacante == null || plan == null || viabilidad == null) {
            CargandoPantalla()
            return@PantallaBase
        }
        val id = vacante.resumen.id
        ContenidoDesplazable(padding) {
            Text(vacante.resumen.puesto, style = MaterialTheme.typography.titleMedium)
            if (!estado.premium) {
                BloqueoPremium("Tu plan es Premium", "Activa Premium para ver tu plan semanal y marcar tus avances.", onDesbloquear = { onPremium(id) })
                return@ContenidoDesplazable
            }
            val colores = ChambaTema.colores
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (viabilidad.esViable) Etiqueta("VIABLE", colores.exitoFondo, colores.exitoTexto) else Etiqueta("NO VIABLE", colores.avisoRojoFondo, colores.avisoRojoTexto)
                Text(
                    "  ${formatoHoras(viabilidad.horasNecesarias)} necesarias · ${formatoHoras(viabilidad.horasDisponibles)} disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text("Avance: ${(estado.avance * 100).toInt()} %", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { estado.avance.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            if (plan.semanas.isEmpty()) {
                TarjetaSeccion { Text("No hay tareas: ya cumples todo o no tienes horas libres. Revisa tu tiempo libre en Perfil.") }
            }
            plan.semanas.forEach { semana ->
                TarjetaSeccion(titulo = "Semana ${semana.numero} · desde el ${formatoFecha(semana.inicio)}") {
                    semana.tareas.forEachIndexed { i, tarea ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        val hecha = tarea.id in estado.completadas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onTarea(tarea.id) },
                        ) {
                            Checkbox(checked = hecha, onCheckedChange = null)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(tarea.titulo, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    listOfNotNull(tarea.curso?.titulo, formatoHoras(tarea.horas)).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colores.textoSecundario,
                                )
                            }
                        }
                    }
                }
            }
            val modulos = plan.tareas.map { it.moduloId }.distinct()
            if (modulos.isNotEmpty()) {
                TarjetaSeccion(titulo = "Mini-test por módulo") {
                    modulos.forEach { modulo ->
                        SecondaryButton(
                            "Mini-test de ${MockDatos.nombreHabilidad(modulo)}",
                            onClick = { onMiniTest(id, modulo) },
                            icono = Icons.Outlined.Quiz,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Text(
                "Terminar un curso no sube tu nivel; lo actualizamos con un mini-test o un certificado.",
                style = MaterialTheme.typography.bodySmall,
                color = colores.textoSecundario,
            )
            SecondaryButton("Ver mi progreso", onClick = { onProgreso(id) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun PlanPreview() {
    val hoy = LocalDate.now()
    val vacante = MockDatos.vacantes(hoy).first()
    val horas = HorasLibres(mapOf(DayOfWeek.SATURDAY to 4.0, DayOfWeek.SUNDAY to 2.0))
    val d = MotorDiagnostico.diagnosticar(vacante, NivelesUsuario(mapOf("sql" to NivelHabilidad.BASICO, "excel" to NivelHabilidad.AVANZADO)), MockDatos.cursos, horas, hoy)
    val plan = MotorDiagnostico.generarPlan(d, horas, hoy, vacante.resumen.fechaCierre)
    ChambaReadyTheme {
        PlanContenido(
            PlanUiState(vacante, plan, d.viabilidad, completadas = setOfNotNull(plan.tareas.firstOrNull()?.id)),
            onBack = {}, onTarea = {}, onMiniTest = { _, _ -> }, onProgreso = {}, onPremium = {},
        )
    }
}
