package pe.edu.esan.chambaready.ui.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.CountdownChip
import pe.edu.esan.chambaready.core.designsystem.componentes.VacancyCard
import pe.edu.esan.chambaready.core.designsystem.componentes.formatoHoras
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import pe.edu.esan.chambaready.core.domain.reglas.PlanActivo
import pe.edu.esan.chambaready.core.domain.reglas.ReglasPrioridad
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import pe.edu.esan.chambaready.ui.practicante.vacantes.VacanteItemUi
import pe.edu.esan.chambaready.ui.practicante.vacantes.aItem
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TareaDeHoy(val vacanteId: String, val puesto: String, val titulo: String, val horas: Double)

data class InicioUiState(
    val nombre: String = "",
    val noLeidas: Int = 0,
    val tareaDeHoy: TareaDeHoy? = null,
    val premium: Boolean = false,
    val cierres: List<VacanteItemUi> = emptyList(),
    val ranking: String? = null,
    val cambioRanking: String? = null,
    val recomendadas: List<VacanteItemUi> = emptyList(),
)

/** P07 Inicio: tarea de hoy, cierres cercanos, cambio en el ranking privado, recomendadas y campana. */
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<InicioUiState, Nothing>(InicioUiState()) {

    init {
        viewModelScope.launch {
            val todas = vacantes.listarVacantesOVacio()
            estadoRepositorio.estado.collect { e ->
                val items = todas.map { it.aItem(servicio, e) }
                val abiertas = items.filter { it.diasRestantes >= 0 }
                val notificaciones = contenido.notificaciones(LocalDateTime.of(servicio.hoy(), java.time.LocalTime.NOON))
                // Tarea de hoy: la siguiente del plan con más prioridad (solo con Premium).
                val planes = todas.filter { it.resumen.id in e.planesActivos }
                val tarea = if (!e.premium) {
                    null
                } else {
                    val ordenados = ReglasPrioridad.ordenar(
                        planes.map { v -> PlanActivo(v.resumen.id, servicio.diasRestantes(v), servicio.plan(v, e).avance(e.tareasCompletadas)) },
                    )
                    ordenados.firstNotNullOfOrNull { p ->
                        val v = planes.first { it.resumen.id == p.vacanteId }
                        servicio.plan(v, e).siguienteTarea(e.tareasCompletadas)?.let { TareaDeHoy(v.resumen.id, v.resumen.puesto, it.titulo, it.horas) }
                    }
                }
                val auto = e.autoevaluacion
                setState {
                    copy(
                        nombre = e.registro?.nombre?.substringBefore(" ") ?: "",
                        noLeidas = notificaciones.count { it.id !in e.notificacionesLeidas && e.avisosActivos[it.tipo] != false },
                        tareaDeHoy = tarea,
                        premium = e.premium,
                        cierres = abiertas.sortedBy { it.diasRestantes }.take(3),
                        ranking = auto?.let { "Top ${it.topPorcentaje} % en ${it.area.etiqueta}" },
                        cambioRanking = auto?.topAnterior?.let { anterior ->
                            val diferencia = anterior - auto.topPorcentaje
                            when {
                                diferencia > 0 -> "Mejoraste $diferencia puntos desde tu última evaluación."
                                diferencia < 0 -> "Bajaste ${-diferencia} puntos. Un mini-test te ayuda a recuperarlos."
                                else -> "Sin cambios desde tu última evaluación."
                            }
                        },
                        recomendadas = todas.filter { it.area == e.area }.map { it.aItem(servicio, e) }
                            .filter { it.diasRestantes >= 0 }
                            .sortedByDescending { it.vacante.probabilidad }
                            .take(2),
                    )
                }
            }
        }
    }
}

@Composable
fun InicioScreen(
    onNotificaciones: () -> Unit,
    onAbrirVacante: (String) -> Unit,
    onAbrirPlan: (String) -> Unit,
    onPremium: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    InicioContenido(estado, onNotificaciones, onAbrirVacante, onAbrirPlan, onPremium)
}

@Composable
fun InicioContenido(
    estado: InicioUiState,
    onNotificaciones: () -> Unit,
    onAbrirVacante: (String) -> Unit,
    onAbrirPlan: (String) -> Unit,
    onPremium: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        ContenidoDesplazable(PaddingValues(), modifier = Modifier.statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (estado.nombre.isNotBlank()) "Hola, ${estado.nombre}" else "Hola", style = MaterialTheme.typography.headlineSmall)
                    Text("Esto es lo tuyo para hoy.", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                }
                IconButton(onClick = onNotificaciones) {
                    BadgedBox(badge = { if (estado.noLeidas > 0) Badge { Text("${estado.noLeidas}") } }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = if (estado.noLeidas > 0) "Notificaciones, ${estado.noLeidas} sin leer" else "Notificaciones",
                        )
                    }
                }
            }

            TarjetaSeccion(titulo = "Tarea de hoy") {
                val tarea = estado.tareaDeHoy
                when {
                    tarea != null -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAbrirPlan(tarea.vacanteId) },
                    ) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Dimensiones.espacioChico))
                        Column {
                            Text("${tarea.titulo} · ${formatoHoras(tarea.horas)}", style = MaterialTheme.typography.bodyLarge)
                            Text(tarea.puesto, style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
                        }
                    }
                    !estado.premium -> Text(
                        "Con Premium te armamos un plan y aquí verás qué te toca cada día. Ver Premium",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onPremium),
                    )
                    else -> Text(
                        "No tienes tareas hoy. Abre una vacante y crea tu plan desde el diagnóstico.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChambaTema.colores.textoSecundario,
                    )
                }
            }

            if (estado.cierres.isNotEmpty()) {
                TarjetaSeccion(titulo = "Cierran pronto") {
                    estado.cierres.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onAbrirVacante(item.vacante.id) },
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.vacante.puesto, style = MaterialTheme.typography.bodyLarge)
                                Text(item.vacante.empresa, style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
                            }
                            CountdownChip(item.diasRestantes)
                        }
                    }
                }
            }

            estado.ranking?.let { ranking ->
                TarjetaSeccion(titulo = "Tu ranking privado") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Dimensiones.espacioChico))
                        Text(ranking, style = MaterialTheme.typography.titleMedium)
                    }
                    estado.cambioRanking?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario) }
                    Text("Solo lo ves tú.", style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
                }
            }

            if (estado.recomendadas.isNotEmpty()) {
                Text("Recomendadas para ti", style = MaterialTheme.typography.titleMedium)
                estado.recomendadas.forEach { item ->
                    VacancyCard(item.vacante, item.diasRestantes, onClick = { onAbrirVacante(item.vacante.id) })
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun InicioPreview() {
    val hoy = LocalDate.now()
    val v = VacanteResumen("v1", "Practicante de Analítica de Datos", "Banco Andino", Modalidad.HIBRIDA, "S/ 1,300", hoy.plusDays(12), 58)
    ChambaReadyTheme {
        InicioContenido(
            InicioUiState(
                nombre = "Ana", noLeidas = 3,
                tareaDeHoy = TareaDeHoy("v1", "Practicante de Analítica de Datos", "SQL: sesión 1", 2.0), premium = true,
                cierres = listOf(VacanteItemUi(v.copy(id = "v5", puesto = "Practicante de Datos Junior"), 2)),
                ranking = "Top 18 % en Data y Analytics", cambioRanking = "Mejoraste 3 puntos desde tu última evaluación.",
                recomendadas = listOf(VacanteItemUi(v, 12)),
            ),
            onNotificaciones = {}, onAbrirVacante = {}, onAbrirPlan = {}, onPremium = {},
        )
    }
}
