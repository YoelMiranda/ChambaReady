package pe.edu.esan.chambaready.ui.autoevaluacion

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.repositorio.ResultadoAutoevaluacion
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.domain.reglas.ReglasNivel
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.CargandoPantalla
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.CronometroPreguntas
import pe.edu.esan.chambaready.ui.comun.EstadoCronometro
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.PreguntaCronometrada
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import javax.inject.Inject

/** Máximo de habilidades por evaluación (3 preguntas de 45 s cada una: unos 9 minutos). */
const val MAX_HABILIDADES_EVALUACION = 4

data class HabilidadEvaluable(val id: String, val nombre: String, val nivel: NivelHabilidad)

enum class FaseEvaluacion { ELEGIR, PREPARANDO, RESPONDIENDO, CORRIGIENDO, TERMINADA }

/** Una pregunta con la etiqueta de su habilidad ("SQL · Intermedio"). */
data class PreguntaEvaluacion(val etiqueta: String, val pregunta: PreguntaOpcion)

data class EvaluacionUiState(
    val fase: FaseEvaluacion = FaseEvaluacion.ELEGIR,
    val habilidades: List<HabilidadEvaluable> = emptyList(),
    val elegidas: Set<String> = emptySet(),
    val preguntas: List<PreguntaEvaluacion> = emptyList(),
    val cronometro: EstadoCronometro = EstadoCronometro(),
    /** false: con la cuenta gratis ya usó su evaluación. */
    val puedeEvaluar: Boolean = true,
    val error: String? = null,
) {
    val actual: PreguntaEvaluacion? get() = preguntas.getOrNull(cronometro.indice)
}

/**
 * Evaluación de habilidades (P05 en el registro y "Evaluar mis habilidades" en Perfil).
 * - El estudiante elige hasta 4 habilidades que declaró. Por cada una responde 3 preguntas de SU
 *   nivel declarado, generadas por la IA del backend (técnicas: casos; blandas: situaciones).
 * - Mismo formato que las preguntas filtro: una pregunta a la vez, 45 s cada una.
 * - Aprobar (2 de 3) sube el nivel demostrado: al postular, ese requisito sale "Confirmado".
 * - Con la cuenta gratis, una sola vez; con Premium, cuando quiera.
 * - El total de aciertos da el ranking privado del área (regla 5).
 */
@HiltViewModel
class EvaluacionHabilidadesViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
    private val cuenta: CuentaRepository,
) : BaseViewModel<EvaluacionUiState, Nothing>(EvaluacionUiState()) {

    /** Una habilidad evaluada: sus preguntas y, con backend, el id del mini-test que las corrige. */
    private data class Prueba(val habilidad: HabilidadEvaluable, val miniTestId: String?, val preguntas: List<PreguntaOpcion>)

    private var pruebas: List<Prueba> = emptyList()
    private val cronometro = CronometroPreguntas(
        alcance = viewModelScope,
        onCambio = { c -> setState { copy(cronometro = c) } },
        alTerminar = ::corregir,
    )

    init {
        viewModelScope.launch {
            val e = estadoRepositorio.estado.first()
            val declaradas = e.niveles.declarados
                .filterValues { it.valor > 0 }
                .map { (id, nivel) -> HabilidadEvaluable(id, contenido.habilidad(id)?.nombre ?: id, nivel) }
                .sortedByDescending { it.nivel.valor }
            val puede = try {
                cuenta.estadoEvaluacion()?.puedeEvaluar
            } catch (_: ErrorRemoto) {
                null
            } ?: (e.premium || e.autoevaluacion == null) // Modo demo: gratis, una vez.
            setState {
                copy(
                    habilidades = declaradas,
                    elegidas = declaradas.take(MAX_HABILIDADES_EVALUACION).map { it.id }.toSet(),
                    puedeEvaluar = puede,
                )
            }
        }
    }

    fun alternar(id: String) = setState {
        when {
            id in elegidas -> copy(elegidas = elegidas - id)
            elegidas.size < MAX_HABILIDADES_EVALUACION -> copy(elegidas = elegidas + id)
            else -> this
        }
    }

    fun elegir(opcion: Int) = cronometro.elegir(opcion)

    fun siguiente() = cronometro.siguiente()

    fun empezar() {
        val estado = currentState
        if (estado.fase != FaseEvaluacion.ELEGIR || estado.elegidas.isEmpty() || !estado.puedeEvaluar) return
        val elegidas = estado.habilidades.filter { it.id in estado.elegidas }
        setState { copy(fase = FaseEvaluacion.PREPARANDO, error = null) }
        viewModelScope.launch {
            try {
                // Con backend: preguntas de la IA al nivel declarado. En modo demo: las de prueba.
                val remotas = cuenta.iniciarEvaluacion(elegidas.map { it.id })
                pruebas = if (remotas != null) {
                    elegidas.zip(remotas) { h, test -> Prueba(h, test.id, test.preguntas) }
                } else {
                    elegidas.map { h -> Prueba(h, null, contenido.miniTest(h.id)) }.filter { it.preguntas.isNotEmpty() }
                }
                val preguntas = pruebas.flatMap { p -> p.preguntas.map { PreguntaEvaluacion("${p.habilidad.nombre} · ${p.habilidad.nivel.etiqueta}", it) } }
                if (preguntas.isEmpty()) {
                    setState { copy(fase = FaseEvaluacion.ELEGIR, error = "No hay preguntas para esas habilidades todavía.") }
                    return@launch
                }
                setState { copy(fase = FaseEvaluacion.RESPONDIENDO, preguntas = preguntas) }
                cronometro.empezar(preguntas.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorRemoto) {
                setState { copy(fase = FaseEvaluacion.ELEGIR, error = e.message, puedeEvaluar = e.codigo != "EVALUACION_USADA") }
            }
        }
    }

    /** Corrige cada habilidad (con backend, el servidor) y guarda niveles y ranking. */
    private fun corregir(respuestas: List<Int?>) {
        setState { copy(fase = FaseEvaluacion.CORRIGIENDO) }
        viewModelScope.launch {
            var correctas = 0
            var total = 0
            val nuevos = mutableMapOf<String, NivelHabilidad>()
            val antes = estadoRepositorio.estado.first().nivelesEfectivos
            try {
                var desde = 0
                for (p in pruebas) {
                    val suyas = respuestas.subList(desde, desde + p.preguntas.size)
                    desde += p.preguntas.size
                    val id = p.miniTestId
                    if (id != null) {
                        val r = cuenta.responderMiniTest(id, suyas, null) ?: continue
                        correctas += r.correctas
                        total += r.total
                        nuevos[p.habilidad.id] = r.nivelDespues
                    } else {
                        val aciertos = p.preguntas.zip(suyas).count { (q, elegida) -> q.esCorrecta(elegida) }
                        correctas += aciertos
                        total += p.preguntas.size
                        val actual = antes.demostrado(p.habilidad.id)
                        val aprobo = ReglasNivel.miniTestAprobado(aciertos, p.preguntas.size)
                        nuevos[p.habilidad.id] = if (aprobo && p.habilidad.nivel.valor > actual.valor) p.habilidad.nivel else actual
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorRemoto) {
                setState { copy(fase = FaseEvaluacion.ELEGIR, error = e.message) }
                return@launch
            }
            val e = estadoRepositorio.estado.first()
            val area = e.area ?: AreaInteres.DATA_ANALYTICS
            val top = try {
                cuenta.autoevaluacion(area, correctas, total.coerceAtLeast(1))
            } catch (_: ErrorRemoto) {
                null
            }
            estadoRepositorio.actualizar { actual ->
                actual.copy(
                    niveles = actual.niveles.copy(demostrados = actual.niveles.demostrados + nuevos),
                    autoevaluacion = ResultadoAutoevaluacion(
                        area = area,
                        correctas = correctas,
                        total = total.coerceAtLeast(1),
                        topPorcentaje = top ?: contenido.topPorcentaje(area, correctas, total.coerceAtLeast(1)),
                        topAnterior = actual.autoevaluacion?.takeIf { it.area == area }?.topPorcentaje,
                    ),
                )
            }
            setState { copy(fase = FaseEvaluacion.TERMINADA) }
        }
    }

    override fun onCleared() {
        cronometro.detener()
        super.onCleared()
    }
}

/**
 * @param desdeRegistro en el registro (P05) se puede saltar; desde Perfil, no hace falta.
 * @param onTerminar al terminar de corregir (muestra el resultado, P06).
 */
@Composable
fun EvaluacionHabilidadesScreen(
    desdeRegistro: Boolean,
    onBack: () -> Unit,
    onTerminar: () -> Unit,
    onSaltar: () -> Unit,
    onPremium: () -> Unit,
    viewModel: EvaluacionHabilidadesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.fase) { if (estado.fase == FaseEvaluacion.TERMINADA) onTerminar() }
    EvaluacionHabilidadesContenido(
        estado = estado,
        desdeRegistro = desdeRegistro,
        onBack = onBack,
        onAlternar = viewModel::alternar,
        onEmpezar = viewModel::empezar,
        onSaltar = onSaltar,
        onPremium = onPremium,
        onElegir = viewModel::elegir,
        onSiguiente = viewModel::siguiente,
    )
}

@Composable
fun EvaluacionHabilidadesContenido(
    estado: EvaluacionUiState,
    desdeRegistro: Boolean,
    onBack: () -> Unit,
    onAlternar: (String) -> Unit,
    onEmpezar: () -> Unit,
    onSaltar: () -> Unit,
    onPremium: () -> Unit,
    onElegir: (Int) -> Unit,
    onSiguiente: () -> Unit,
) {
    when (estado.fase) {
        FaseEvaluacion.PREPARANDO, FaseEvaluacion.CORRIGIENDO, FaseEvaluacion.TERMINADA -> CargandoPantalla()
        FaseEvaluacion.RESPONDIENDO -> PantallaBase(
            titulo = "Pregunta ${estado.cronometro.indice + 1} de ${estado.preguntas.size}",
            onBack = onBack,
            barraInferior = {
                BarraAcciones {
                    PrimaryButton(if (estado.cronometro.esUltima) "Terminar" else "Siguiente", onClick = onSiguiente, modifier = Modifier.fillMaxWidth())
                }
            },
        ) { padding ->
            ContenidoDesplazable(padding) {
                estado.actual?.let { PreguntaCronometrada(estado.cronometro, it.pregunta, onElegir, etiqueta = it.etiqueta) }
            }
        }
        FaseEvaluacion.ELEGIR -> PantallaBase(
            titulo = if (desdeRegistro) "Evalúa tus habilidades" else "Evaluar mis habilidades",
            onBack = onBack,
            barraInferior = {
                BarraAcciones {
                    PrimaryButton(
                        "Empezar evaluación",
                        onClick = onEmpezar,
                        enabled = estado.puedeEvaluar && estado.elegidas.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (desdeRegistro) {
                        TextButton(onClick = onSaltar, modifier = Modifier.fillMaxWidth()) { Text("Saltar por ahora") }
                    }
                }
            },
        ) { padding ->
            ContenidoDesplazable(padding) {
                Text(
                    "Elige hasta $MAX_HABILIDADES_EVALUACION habilidades que declaraste. Responderás 3 preguntas de cada una, de tu nivel, " +
                        "con 45 segundos por pregunta. Si aciertas 2 de 3, esa habilidad pasa de «Declarado» a «Confirmado».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChambaTema.colores.textoSecundario,
                )
                if (!estado.puedeEvaluar) {
                    TarjetaSeccion(titulo = "Ya usaste tu evaluación gratis") {
                        Text("Con Premium puedes evaluarte cuando quieras.", style = MaterialTheme.typography.bodyMedium)
                        PrimaryButton("Ver Premium", onClick = onPremium, modifier = Modifier.fillMaxWidth())
                    }
                } else if (desdeRegistro) {
                    Text(
                        "Con la cuenta gratis puedes evaluarte una vez. Si la saltas, podrás hacerla después desde tu Perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChambaTema.colores.textoSecundario,
                    )
                }
                estado.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                if (estado.habilidades.isEmpty()) {
                    Text("Primero declara tus habilidades en tu perfil.", style = MaterialTheme.typography.bodyLarge)
                }
                TarjetaSeccion(titulo = "Tus habilidades (${estado.elegidas.size} de $MAX_HABILIDADES_EVALUACION)") {
                    estado.habilidades.forEach { h ->
                        val elegida = h.id in estado.elegidas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(value = elegida, enabled = estado.puedeEvaluar, role = Role.Checkbox, onValueChange = { onAlternar(h.id) }),
                        ) {
                            Checkbox(checked = elegida, onCheckedChange = null, enabled = estado.puedeEvaluar)
                            Text("${h.nombre} · ${h.nivel.etiqueta}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun EvaluacionPreview() {
    ChambaReadyTheme {
        EvaluacionHabilidadesContenido(
            estado = EvaluacionUiState(
                habilidades = listOf(
                    HabilidadEvaluable("python", "Python", NivelHabilidad.AVANZADO),
                    HabilidadEvaluable("equipo", "Trabajo en equipo", NivelHabilidad.INTERMEDIO),
                ),
                elegidas = setOf("python"),
            ),
            desdeRegistro = true,
            onBack = {}, onAlternar = {}, onEmpezar = {}, onSaltar = {}, onPremium = {}, onElegir = {}, onSiguiente = {},
        )
    }
}
