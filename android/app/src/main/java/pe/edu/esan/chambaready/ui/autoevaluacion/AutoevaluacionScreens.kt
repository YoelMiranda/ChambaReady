package pe.edu.esan.chambaready.ui.autoevaluacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.repositorio.ResultadoAutoevaluacion
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.SkillLevelBar
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.designsystem.theme.EstiloCodigo
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import javax.inject.Inject

// ---------- Pregunta de opción múltiple reutilizable (P05, P15, P24) ----------

/** Una pregunta con sus opciones como botones de radio accesibles. El código va en JetBrains Mono. */
@Composable
fun PreguntaConOpciones(
    numero: Int,
    pregunta: PreguntaOpcion,
    elegida: Int?,
    onElegir: (Int) -> Unit,
    habilitada: Boolean = true,
) {
    TarjetaSeccion {
        Text("$numero. ${pregunta.enunciado}", style = MaterialTheme.typography.titleMedium)
        pregunta.codigo?.let { codigo ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(codigo, style = EstiloCodigo, modifier = Modifier.padding(12.dp))
            }
        }
        Column(Modifier.selectableGroup()) {
            pregunta.opciones.forEachIndexed { i, opcion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = elegida == i, enabled = habilitada, role = Role.RadioButton, onClick = { onElegir(i) })
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = elegida == i, onClick = null, enabled = habilitada)
                    Text(opcion, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

// ---------- P05 Autoevaluación ----------

data class AutoevaluacionUiState(
    val area: AreaInteres? = null,
    val preguntas: List<PreguntaOpcion> = emptyList(),
    val respuestas: Map<String, Int> = emptyMap(),
) {
    val completa: Boolean get() = preguntas.isNotEmpty() && respuestas.size == preguntas.size
}

/**
 * P05: preguntas del área elegida, con "No lo sé" y sin tiempo límite. El resultado (privado)
 * se guarda y se muestra en P06. En F6 las preguntas las genera Gemini.
 */
@HiltViewModel
class AutoevaluacionViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
    private val cuenta: CuentaRepository,
) : BaseViewModel<AutoevaluacionUiState, Nothing>(AutoevaluacionUiState()) {

    init {
        viewModelScope.launch {
            val area = estadoRepositorio.estado.first().area ?: AreaInteres.DATA_ANALYTICS
            setState { copy(area = area, preguntas = contenido.preguntasAutoevaluacion(area)) }
        }
    }

    fun responder(preguntaId: String, opcion: Int) = setState { copy(respuestas = respuestas + (preguntaId to opcion)) }

    fun terminar(alTerminar: () -> Unit) {
        val estado = currentState
        val area = estado.area ?: return
        val correctas = estado.preguntas.count { it.esCorrecta(estado.respuestas[it.id]) }
        viewModelScope.launch {
            // Ranking privado (regla 5): con backend se compara con todos los del área; sin conexión
            // o en modo demo, se usa la estimación local.
            val topDelServidor = try {
                cuenta.autoevaluacion(area, correctas, estado.preguntas.size)
            } catch (_: ErrorRemoto) {
                null
            }
            estadoRepositorio.actualizar { e ->
                val anterior = e.autoevaluacion?.takeIf { it.area == area }?.topPorcentaje
                e.copy(
                    autoevaluacion = ResultadoAutoevaluacion(
                        area = area,
                        correctas = correctas,
                        total = estado.preguntas.size,
                        topPorcentaje = topDelServidor ?: contenido.topPorcentaje(area, correctas, estado.preguntas.size),
                        topAnterior = anterior,
                    ),
                )
            }
            alTerminar()
        }
    }
}

@Composable
fun AutoevaluacionScreen(onBack: () -> Unit, onTerminar: () -> Unit, viewModel: AutoevaluacionViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    AutoevaluacionContenido(estado, onBack, viewModel::responder) { viewModel.terminar(onTerminar) }
}

@Composable
fun AutoevaluacionContenido(
    estado: AutoevaluacionUiState,
    onBack: () -> Unit,
    onResponder: (String, Int) -> Unit,
    onTerminar: () -> Unit,
) {
    PantallaBase(
        titulo = "Autoevaluación",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton("Ver mi resultado", onClick = onTerminar, enabled = estado.completa, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(Dimensiones.espacioGrande), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(Dimensiones.espacioChico))
                    Text("No es un examen; nadie más ve tus respuestas.", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
            estado.area?.let { Text("Preguntas de ${it.etiqueta}. Tómate tu tiempo.", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario) }
            estado.preguntas.forEachIndexed { i, pregunta ->
                PreguntaConOpciones(i + 1, pregunta, estado.respuestas[pregunta.id], { onResponder(pregunta.id, it) })
            }
        }
    }
}

// ---------- P06 Resultado ----------

data class ComparacionHabilidad(val nombre: String, val declarado: NivelHabilidad, val demostrado: NivelHabilidad)

data class ResultadoUiState(
    val resultado: ResultadoAutoevaluacion? = null,
    val comparaciones: List<ComparacionHabilidad> = emptyList(),
)

/** P06: "Top X % en {área}" (privado) y nivel declarado vs. demostrado. */
@HiltViewModel
class ResultadoAutoevaluacionViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
) : BaseViewModel<ResultadoUiState, Nothing>(ResultadoUiState()) {

    init {
        viewModelScope.launch {
            estadoRepositorio.estado.collect { e ->
                val niveles = e.nivelesEfectivos
                setState {
                    copy(
                        resultado = e.autoevaluacion,
                        comparaciones = niveles.declarados.keys.mapNotNull { id ->
                            contenido.habilidad(id)?.let { ComparacionHabilidad(it.nombre, niveles.declarado(id), niveles.demostrado(id)) }
                        },
                    )
                }
            }
        }
    }

    fun terminarRegistro(alTerminar: () -> Unit) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { it.copy(onboardingCompleto = true) }
            alTerminar()
        }
    }
}

@Composable
fun ResultadoAutoevaluacionScreen(
    onBack: () -> Unit,
    onIrAlInicio: () -> Unit,
    viewModel: ResultadoAutoevaluacionViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    ResultadoAutoevaluacionContenido(estado, onBack) { viewModel.terminarRegistro(onIrAlInicio) }
}

@Composable
fun ResultadoAutoevaluacionContenido(estado: ResultadoUiState, onBack: () -> Unit, onIrAlInicio: () -> Unit) {
    PantallaBase(
        titulo = "Tu resultado",
        onBack = onBack,
        barraInferior = { BarraAcciones { PrimaryButton("Ir al inicio", onClick = onIrAlInicio, modifier = Modifier.fillMaxWidth()) } },
    ) { padding ->
        ContenidoDesplazable(padding) {
            estado.resultado?.let { r ->
                TarjetaSeccion {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Dimensiones.espacioChico))
                        Text("Top ${r.topPorcentaje} % en ${r.area.etiqueta}", style = MaterialTheme.typography.headlineSmall)
                    }
                    Text("Solo lo ves tú. Las empresas nunca ven tu ranking.", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                    Text("Acertaste ${r.correctas} de ${r.total}.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            TarjetaSeccion(titulo = "Declarado vs. demostrado") {
                Text(
                    "El nivel demostrado sube con un mini-test aprobado o una evidencia validada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChambaTema.colores.textoSecundario,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioGrande)) {
                    estado.comparaciones.forEach { c ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkillLevelBar("${c.nombre} · declarado", c.declarado)
                            SkillLevelBar("${c.nombre} · demostrado", c.demostrado, color = ChambaTema.colores.selloDeclarado)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun AutoevaluacionPreview() {
    ChambaReadyTheme {
        AutoevaluacionContenido(
            AutoevaluacionUiState(
                AreaInteres.DATA_ANALYTICS,
                listOf(
                    PreguntaOpcion("p1", "¿Qué devuelve esta consulta?", listOf("El total por región", "Una fila por venta", "No lo sé"), 0, codigo = "SELECT region, SUM(monto)\nFROM ventas GROUP BY region;"),
                ),
                respuestas = mapOf("p1" to 0),
            ),
            onBack = {}, onResponder = { _, _ -> }, onTerminar = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ResultadoPreview() {
    ChambaReadyTheme {
        ResultadoAutoevaluacionContenido(
            ResultadoUiState(
                ResultadoAutoevaluacion(AreaInteres.DATA_ANALYTICS, 2, 3, 18),
                listOf(ComparacionHabilidad("SQL", NivelHabilidad.INTERMEDIO, NivelHabilidad.BASICO), ComparacionHabilidad("Excel", NivelHabilidad.AVANZADO, NivelHabilidad.AVANZADO)),
            ),
            onBack = {}, onIrAlInicio = {},
        )
    }
}
