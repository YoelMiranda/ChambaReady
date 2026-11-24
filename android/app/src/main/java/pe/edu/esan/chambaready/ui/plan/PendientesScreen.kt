package pe.edu.esan.chambaready.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.CountdownChip
import pe.edu.esan.chambaready.core.designsystem.componentes.formatoHoras
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.reglas.PlanActivo
import pe.edu.esan.chambaready.core.domain.reglas.ReglasPrioridad
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BloqueoPremium
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import javax.inject.Inject

data class PendienteUi(
    val vacanteId: String,
    val puesto: String,
    val diasRestantes: Int,
    val avance: Double,
    val hoyToca: String?,
)

data class PendientesUiState(
    val premium: Boolean = false,
    val planes: List<PendienteUi> = emptyList(),
    val horasHechasSemana: Double = 0.0,
    val horasLibresSemana: Double = 0.0,
    val metas: List<Pair<String, String>> = emptyList(),
)

/** P14 Mis pendientes (Premium): planes ordenados por prioridad (cierre más cercano y menos avance). */
@HiltViewModel
class PendientesViewModel @Inject constructor(
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<PendientesUiState, Nothing>(PendientesUiState()) {

    init {
        viewModelScope.launch {
            val todas = vacantes.listarVacantesOVacio()
            estadoRepositorio.estado.collect { e ->
                val activos = todas.filter { it.resumen.id in e.planesActivos }
                val planes = activos.associateWith { servicio.plan(it, e) }
                val orden = ReglasPrioridad.ordenar(
                    planes.map { (v, plan) -> PlanActivo(v.resumen.id, servicio.diasRestantes(v), plan.avance(e.tareasCompletadas)) },
                )
                setState {
                    copy(
                        premium = e.premium,
                        planes = orden.map { p ->
                            val (v, plan) = planes.entries.first { it.key.resumen.id == p.vacanteId }.toPair()
                            PendienteUi(v.resumen.id, v.resumen.puesto, p.diasRestantes, p.avance, plan.siguienteTarea(e.tareasCompletadas)?.let { "${it.titulo} · ${formatoHoras(it.horas)}" })
                        },
                        // Horas de las tareas de esta semana (semana 1 de cada plan) ya marcadas.
                        horasHechasSemana = planes.values.sumOf { plan ->
                            plan.semanas.firstOrNull()?.tareas?.filter { it.id in e.tareasCompletadas }?.sumOf { it.horas } ?: 0.0
                        },
                        horasLibresSemana = e.horasLibres.totalSemanal,
                        metas = todas.filter { it.resumen.id in e.metas }.map { it.resumen.id to it.resumen.puesto },
                    )
                }
            }
        }
    }
}

@Composable
fun PendientesScreen(onAbrirPlan: (String) -> Unit, onAbrirVacante: (String) -> Unit, onPremium: () -> Unit, viewModel: PendientesViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PendientesContenido(estado, onAbrirPlan, onAbrirVacante, onPremium)
}

@Composable
fun PendientesContenido(estado: PendientesUiState, onAbrirPlan: (String) -> Unit, onAbrirVacante: (String) -> Unit, onPremium: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        ContenidoDesplazable(PaddingValues(), modifier = Modifier.statusBarsPadding()) {
            Text("Mis pendientes", style = MaterialTheme.typography.headlineSmall)
            if (!estado.premium) {
                BloqueoPremium(
                    "Mis pendientes es Premium",
                    "Aquí verás todos tus planes ordenados por prioridad y qué te toca hoy en cada uno.",
                    onDesbloquear = onPremium,
                )
                return@ContenidoDesplazable
            }
            TarjetaSeccion(titulo = "Esta semana") {
                Text(
                    "${formatoHoras(estado.horasHechasSemana)} hechas de ${formatoHoras(estado.horasLibresSemana)} libres",
                    style = MaterialTheme.typography.bodyLarge,
                )
                val fraccion = if (estado.horasLibresSemana > 0) (estado.horasHechasSemana / estado.horasLibresSemana).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { fraccion }, modifier = Modifier.fillMaxWidth())
            }
            if (estado.planes.isEmpty()) {
                Text(
                    "No tienes planes activos. Abre una vacante, mira tu diagnóstico y crea tu plan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChambaTema.colores.textoSecundario,
                )
            }
            estado.planes.forEach { p ->
                TarjetaSeccion(modifier = Modifier.clickable { onAbrirPlan(p.vacanteId) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.puesto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        CountdownChip(p.diasRestantes)
                    }
                    Text("Avance: ${(p.avance * 100).toInt()} %", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { p.avance.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text(
                        p.hoyToca?.let { "Hoy toca: $it" } ?: "¡Plan terminado! Haz el mini-test para confirmar tu nivel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (estado.metas.isNotEmpty()) {
                TarjetaSeccion(titulo = "Metas sin fecha") {
                    estado.metas.forEach { (id, puesto) ->
                        Column(Modifier.fillMaxWidth().clickable { onAbrirVacante(id) }.padding(vertical = 8.dp)) {
                            Text(puesto, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PendientesPreview() {
    ChambaReadyTheme {
        PendientesContenido(
            PendientesUiState(
                premium = true,
                planes = listOf(
                    PendienteUi("v5", "Practicante de Datos Junior", 2, 0.6, "SQL: sesión 3 · 2 h"),
                    PendienteUi("v1", "Practicante de Analítica de Datos", 12, 0.1, "Power BI: sesión 1 · 2 h"),
                ),
                horasHechasSemana = 4.0,
                horasLibresSemana = 13.0,
                metas = listOf("v2" to "Practicante de Desarrollo Android"),
            ),
            onAbrirPlan = {}, onAbrirVacante = {}, onPremium = {},
        )
    }
}
