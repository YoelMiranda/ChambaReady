package pe.edu.esan.chambaready.ui.notificaciones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.PuntoNoLeido
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class FiltroNotificaciones(val etiqueta: String) { TODAS("Todas"), SIN_LEER("Sin leer") }

data class NotificacionesUiState(
    val notificaciones: List<Notificacion> = emptyList(),
    val leidas: Set<String> = emptySet(),
    val filtro: FiltroNotificaciones = FiltroNotificaciones.TODAS,
    val hoy: LocalDate = LocalDate.now(),
) {
    private val visibles get() = notificaciones.filter { filtro == FiltroNotificaciones.TODAS || it.id !in leidas }
    val deHoy get() = visibles.filter { it.fecha.toLocalDate() == hoy }
    val anteriores get() = visibles.filter { it.fecha.toLocalDate() != hoy }
    val hayNoLeidas get() = notificaciones.any { it.id !in leidas }
}

/** P20. Tocar una notificación la marca leída y abre su pantalla. En F9 llegan por FCM con deep link. */
@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val cuenta: CuentaRepository,
    contenido: ContenidoRepository,
    servicio: ServicioPreparacion,
) : BaseViewModel<NotificacionesUiState, Nothing>(NotificacionesUiState(hoy = servicio.hoy())) {

    init {
        viewModelScope.launch {
            // F9: con login, las notificaciones vienen del backend (sin push: se leen al abrir esta
            // pantalla). En modo demo o sin conexión, las de prueba.
            val remotas = try {
                cuenta.notificaciones()
            } catch (_: ErrorRemoto) {
                null
            }
            val todas = (remotas?.map { it.notificacion } ?: contenido.notificaciones(LocalDateTime.of(servicio.hoy(), LocalTime.NOON)))
                .sortedByDescending { it.fecha }
            val leidasEnServidor = remotas?.filter { it.leida }?.map { it.notificacion.id }?.toSet().orEmpty()
            estadoRepositorio.estado.collect { e ->
                setState {
                    copy(
                        notificaciones = todas.filter { e.avisosActivos[it.tipo] != false },
                        leidas = e.notificacionesLeidas + leidasEnServidor,
                    )
                }
            }
        }
    }

    fun filtrar(filtro: FiltroNotificaciones) = setState { copy(filtro = filtro) }

    fun marcarLeida(id: String) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { it.copy(notificacionesLeidas = it.notificacionesLeidas + id) }
            try {
                cuenta.marcarNotificacionLeida(id)
            } catch (_: ErrorRemoto) {
                // Sin conexión: queda leída en el teléfono.
            }
        }
    }

    fun marcarTodoLeido() {
        val ids = currentState.notificaciones.map { it.id }
        viewModelScope.launch {
            estadoRepositorio.actualizar { it.copy(notificacionesLeidas = it.notificacionesLeidas + ids) }
            try {
                cuenta.marcarTodasLeidas()
            } catch (_: ErrorRemoto) {
                // Sin conexión: quedan leídas en el teléfono.
            }
        }
    }
}

@Composable
fun NotificacionesScreen(
    onBack: () -> Unit,
    onAbrir: (Notificacion) -> Unit,
    viewModel: NotificacionesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    NotificacionesContenido(
        estado = estado,
        onBack = onBack,
        onFiltro = viewModel::filtrar,
        onMarcarTodo = viewModel::marcarTodoLeido,
        onTocar = { n ->
            viewModel.marcarLeida(n.id)
            onAbrir(n)
        },
    )
}

@Composable
fun NotificacionesContenido(
    estado: NotificacionesUiState,
    onBack: () -> Unit,
    onFiltro: (FiltroNotificaciones) -> Unit,
    onMarcarTodo: () -> Unit,
    onTocar: (Notificacion) -> Unit,
) {
    PantallaBase(
        titulo = "Notificaciones",
        onBack = onBack,
        acciones = { TextButton(onClick = onMarcarTodo, enabled = estado.hayNoLeidas) { Text("Marcar todo leído") } },
    ) { padding ->
        ContenidoDesplazable(padding) {
            ChipSelector(FiltroNotificaciones.entries, setOf(estado.filtro), onFiltro, { it.etiqueta })
            Grupo("Hoy", estado.deHoy, estado.leidas, onTocar)
            Grupo("Anteriores", estado.anteriores, estado.leidas, onTocar)
            if (estado.deHoy.isEmpty() && estado.anteriores.isEmpty()) {
                Text("No tienes notificaciones sin leer.", style = MaterialTheme.typography.bodyLarge, color = ChambaTema.colores.textoSecundario)
            }
        }
    }
}

private val formatoHora = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-PE"))

private fun icono(tipo: TipoNotificacion): ImageVector = when (tipo) {
    TipoNotificacion.CIERRE_CERCANO -> Icons.Outlined.Schedule
    TipoNotificacion.CAMBIO_ESTADO -> Icons.AutoMirrored.Outlined.Assignment
    TipoNotificacion.ENTREVISTA -> Icons.Outlined.Event
    TipoNotificacion.PLAN_SEMANAL -> Icons.Outlined.TaskAlt
    TipoNotificacion.NUEVA_VACANTE -> Icons.Outlined.WorkOutline
    TipoNotificacion.RANKING -> Icons.AutoMirrored.Outlined.TrendingUp
    TipoNotificacion.PREGUNTAS_FILTRO -> Icons.Outlined.Quiz
    TipoNotificacion.PAGO -> Icons.Outlined.CreditCard
    TipoNotificacion.EVIDENCIA -> Icons.Outlined.Verified
}

@Composable
private fun Grupo(titulo: String, lista: List<Notificacion>, leidas: Set<String>, onTocar: (Notificacion) -> Unit) {
    if (lista.isEmpty()) return
    TarjetaSeccion(titulo = titulo) {
        lista.forEachIndexed { i, n ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            val leida = n.id in leidas
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTocar(n) }
                    .semantics { contentDescription = "${if (leida) "" else "Sin leer. "}${n.titulo}. ${n.cuerpo}" }
                    .padding(vertical = 8.dp),
            ) {
                Icon(icono(n.tipo), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Dimensiones.espacioMedio))
                Column(Modifier.weight(1f)) {
                    Text(n.titulo, style = if (leida) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleSmall)
                    Text(n.cuerpo, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                    Text(n.fecha.format(formatoHora), style = MaterialTheme.typography.labelSmall, color = ChambaTema.colores.textoSecundario)
                }
                if (!leida) PuntoNoLeido(Modifier.padding(top = 6.dp).size(10.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun NotificacionesPreview() {
    val ahora = LocalDateTime.now()
    ChambaReadyTheme {
        NotificacionesContenido(
            NotificacionesUiState(
                notificaciones = listOf(
                    Notificacion("n1", TipoNotificacion.CIERRE_CERCANO, "Cierra pronto", "«Datos Junior» cierra en 2 días.", ahora),
                    Notificacion("n2", TipoNotificacion.RANKING, "Subiste en tu ranking", "Top 18 % en Data y Analytics.", ahora.minusDays(3)),
                ),
                leidas = setOf("n2"),
                hoy = ahora.toLocalDate(),
            ),
            onBack = {}, onFiltro = {}, onMarcarTodo = {}, onTocar = {},
        )
    }
}
