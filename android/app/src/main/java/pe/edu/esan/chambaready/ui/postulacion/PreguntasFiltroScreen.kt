package pe.edu.esan.chambaready.ui.postulacion

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.remoto.RespuestasFiltroSesion
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.autoevaluacion.PreguntaConOpciones
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import javax.inject.Inject

const val SEGUNDOS_POR_PREGUNTA = 45

data class PreguntasFiltroUiState(
    val preguntas: List<PreguntaOpcion> = emptyList(),
    val indice: Int = 0,
    val segundos: Int = SEGUNDOS_POR_PREGUNTA,
    val elegida: Int? = null,
    val terminado: Boolean = false,
    val cargando: Boolean = true,
    val error: String? = null,
) {
    val actual: PreguntaOpcion? get() = preguntas.getOrNull(indice)
    val esUltima: Boolean get() = indice == preguntas.lastIndex
}

/**
 * P24: 2 preguntas de 45 s con temporizador real. Si se acaba el tiempo, pasa a la siguiente sin
 * respuesta.
 * - Modo demo: corrige el teléfono y guarda los aciertos; P17 recalcula los sellos al volver.
 * - Con backend (F6): las preguntas llegan SIN la respuesta. Se guarda la opción elegida y corrige
 *   el servidor al enviar la postulación (o al instante, si son preguntas pendientes de la regla 13).
 */
@HiltViewModel
class PreguntasFiltroViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
    private val cuenta: CuentaRepository,
    private val sesion: RespuestasFiltroSesion,
) : BaseViewModel<PreguntasFiltroUiState, Nothing>(PreguntasFiltroUiState()) {

    private val vacanteId: String = checkNotNull(savedStateHandle["vacanteId"])

    /** Regla 13: si viene, se responden las preguntas de una postulación ya enviada. */
    private val postulacionId: String? = savedStateHandle["postulacionId"]
    private val aciertos = mutableListOf<Boolean>()
    private val elegidas = mutableMapOf<String, Int?>()
    private var temporizador: Job? = null
    private var delBackend = false

    init {
        viewModelScope.launch {
            val remotas = try {
                cuenta.preguntasFiltro(vacanteId)
            } catch (e: ErrorRemoto) {
                setState { copy(cargando = false, error = e.message) }
                return@launch
            }
            delBackend = remotas != null
            val preguntas = remotas ?: contenido.preguntasFiltro(vacanteId)
            setState { copy(preguntas = preguntas, cargando = false, error = if (preguntas.isEmpty()) "Esta vacante no tiene preguntas para responder." else null) }
            if (preguntas.isNotEmpty()) iniciarPregunta(0)
        }
    }

    private fun iniciarPregunta(indice: Int) {
        setState { copy(indice = indice, segundos = SEGUNDOS_POR_PREGUNTA, elegida = null) }
        temporizador?.cancel()
        temporizador = viewModelScope.launch {
            while (currentState.segundos > 0) {
                delay(1_000)
                setState { copy(segundos = segundos - 1) }
            }
            siguiente()
        }
    }

    fun elegir(opcion: Int) = setState { copy(elegida = opcion) }

    /** "Siguiente" o tiempo agotado: registra la respuesta (o su ausencia) y avanza. */
    fun siguiente() {
        val estado = currentState
        if (estado.terminado) return
        val pregunta = estado.actual ?: return
        elegidas[pregunta.id] = estado.elegida
        aciertos += pregunta.esCorrecta(estado.elegida)
        if (estado.esUltima) terminar() else iniciarPregunta(estado.indice + 1)
    }

    private fun terminar() {
        temporizador?.cancel()
        val pendiente = postulacionId
        viewModelScope.launch {
            when {
                !delBackend -> estadoRepositorio.actualizar { it.copy(respuestasFiltro = it.respuestasFiltro + (vacanteId to aciertos.toList())) }
                pendiente != null -> {
                    val actualizada = try {
                        cuenta.responderFiltroPendiente(pendiente, elegidas.toMap())
                    } catch (e: ErrorRemoto) {
                        setState { copy(error = e.message) }
                        return@launch
                    }
                    if (actualizada != null) {
                        estadoRepositorio.actualizar { e -> e.copy(postulaciones = e.postulaciones.map { if (it.id == pendiente) actualizada else it }) }
                    }
                }
                else -> {
                    sesion.guardar(vacanteId, elegidas.toMap())
                    // Lista vacía = "respondidas"; los aciertos los calcula el servidor al postular.
                    estadoRepositorio.actualizar { it.copy(respuestasFiltro = it.respuestasFiltro + (vacanteId to emptyList())) }
                }
            }
            setState { copy(terminado = true) }
        }
    }
}

@Composable
fun PreguntasFiltroScreen(onBack: () -> Unit, onTerminado: () -> Unit, viewModel: PreguntasFiltroViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { if (estado.terminado) onTerminado() }
    PreguntasFiltroContenido(estado, onBack, viewModel::elegir, viewModel::siguiente)
}

@Composable
fun PreguntasFiltroContenido(
    estado: PreguntasFiltroUiState,
    onBack: () -> Unit,
    onElegir: (Int) -> Unit,
    onSiguiente: () -> Unit,
) {
    PantallaBase(
        titulo = "Pregunta ${estado.indice + 1} de ${estado.preguntas.size.coerceAtLeast(1)}",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(if (estado.esUltima) "Terminar" else "Siguiente", onClick = onSiguiente, enabled = estado.actual != null, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            estado.error?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
            if (estado.cargando || estado.actual == null) return@ContenidoDesplazable
            val urgente = estado.segundos <= 10
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Quedan ${estado.segundos} segundos" },
            ) {
                Text(
                    "${estado.segundos} s",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (urgente) ChambaTema.colores.avisoRojoTexto else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    if (urgente) "  ¡Se acaba el tiempo!" else "  para responder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChambaTema.colores.textoSecundario,
                )
            }
            LinearProgressIndicator(
                progress = { estado.segundos / SEGUNDOS_POR_PREGUNTA.toFloat() },
                color = if (urgente) ChambaTema.colores.avisoRojoTexto else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            estado.actual?.let { PreguntaConOpciones(estado.indice + 1, it, estado.elegida, onElegir) }
            Text(
                "Si se acaba el tiempo pasamos a la siguiente pregunta.",
                style = MaterialTheme.typography.bodySmall,
                color = ChambaTema.colores.textoSecundario,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreguntasFiltroPreview() {
    ChambaReadyTheme {
        PreguntasFiltroContenido(
            PreguntasFiltroUiState(
                preguntas = listOf(
                    PreguntaOpcion("f1", "¿Qué devuelve esta consulta?", listOf("Una fila por venta", "El total por región", "No lo sé"), 1, codigo = "SELECT region, SUM(monto)\nFROM ventas GROUP BY region;"),
                    PreguntaOpcion("f2", "¿Qué hace el signo $ en =A$1?", listOf("Fija la fila", "Convierte a soles", "No lo sé"), 0),
                ),
                segundos = 8,
                elegida = 1,
            ),
            onBack = {}, onElegir = {}, onSiguiente = {},
        )
    }
}
