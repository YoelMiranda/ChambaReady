package pe.edu.esan.chambaready.ui.plan

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.remoto.MiniTestRemoto
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.repositorio.ResultadoMiniTest
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.ProbabilityScale
import pe.edu.esan.chambaready.core.designsystem.componentes.PuntoEscala
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import pe.edu.esan.chambaready.core.domain.reglas.ReglasNivel
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.autoevaluacion.PreguntaConOpciones
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.BloqueoPremium
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.CronometroPreguntas
import pe.edu.esan.chambaready.ui.comun.EstadoCronometro
import pe.edu.esan.chambaready.ui.comun.PreguntaCronometrada
import pe.edu.esan.chambaready.ui.comun.Etiqueta
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import javax.inject.Inject

// ---------- P15 Mini-test ----------

data class MiniTestUiState(
    val habilidad: String = "",
    val preguntas: List<PreguntaOpcion> = emptyList(),
    val cronometro: EstadoCronometro = EstadoCronometro(),
    val enviando: Boolean = false,
    val error: String? = null,
    /** Ya se corrigió: la pantalla pasa a P16. */
    val terminado: Boolean = false,
) {
    val actual: PreguntaOpcion? get() = preguntas.getOrNull(cronometro.indice)
}

/**
 * P15: 3 preguntas de la habilidad, al nivel declarado, con el mismo formato que las preguntas
 * filtro: una a la vez y 45 s cada una. Con 2 de 3 o más, el nivel demostrado sube (regla 4) y, al
 * postular, ese requisito sale "Confirmado". Con backend corrige el servidor.
 */
@HiltViewModel
class MiniTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
    private val cuenta: CuentaRepository,
) : BaseViewModel<MiniTestUiState, Nothing>(MiniTestUiState()) {

    private val vacanteId: String = checkNotNull(savedStateHandle["vacanteId"])
    private val moduloId: String = checkNotNull(savedStateHandle["moduloId"])

    /** Mini-test del backend (F6); null en modo demo o si no hubo conexión (entonces se usa el local). */
    private var remoto: MiniTestRemoto? = null

    /** Respuestas guardadas por si falla el envío y hay que reintentar. */
    private var respuestas: List<Int?> = emptyList()

    private val cronometro = CronometroPreguntas(
        alcance = viewModelScope,
        onCambio = { c -> setState { copy(cronometro = c) } },
        alTerminar = { r ->
            respuestas = r
            enviar()
        },
    )

    init {
        setState { copy(habilidad = contenido.habilidad(moduloId)?.nombre ?: moduloId) }
        viewModelScope.launch {
            remoto = try {
                cuenta.miniTest(moduloId)
            } catch (_: ErrorRemoto) {
                null
            }
            val preguntas = remoto?.preguntas ?: contenido.miniTest(moduloId)
            setState { copy(preguntas = preguntas) }
            if (preguntas.isNotEmpty()) cronometro.empezar(preguntas.size)
        }
    }

    fun elegir(opcion: Int) = cronometro.elegir(opcion)

    fun siguiente() = cronometro.siguiente()

    /** Reintenta el envío con las mismas respuestas (por ejemplo, si no había conexión). */
    fun enviar() {
        val estado = currentState
        if (estado.enviando || estado.terminado) return
        val test = remoto
        setState { copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                if (test != null) {
                    // Con backend: el servidor corrige y decide el nivel (las preguntas llegaron sin respuesta).
                    val r = cuenta.responderMiniTest(test.id, respuestas, vacanteId) ?: return@launch
                    estadoRepositorio.actualizar { e ->
                        e.copy(
                            niveles = e.niveles.copy(demostrados = e.niveles.demostrados + (moduloId to r.nivelDespues)),
                            ultimoMiniTest = ResultadoMiniTest(vacanteId, moduloId, estado.habilidad, r.correctas, r.total, r.nivelAntes, r.nivelDespues),
                        )
                    }
                } else {
                    corregirEnElTelefono(estado)
                }
                setState { copy(terminado = true) }
            } catch (e: ErrorRemoto) {
                setState { copy(error = e.message) }
            } finally {
                setState { copy(enviando = false) }
            }
        }
    }

    /** Modo demo: corrige el teléfono. Sube al nivel declarado, nunca a menos de lo que pide la vacante. */
    private suspend fun corregirEnElTelefono(estado: MiniTestUiState) {
        val correctas = estado.preguntas.zip(respuestas).count { (pregunta, elegida) -> pregunta.esCorrecta(elegida) }
        val vacante = vacantes.obtenerVacante(vacanteId)
        val exigido = vacante?.requisitos?.firstOrNull { it.habilidadId == moduloId }?.nivelExigido ?: NivelHabilidad.BASICO
        estadoRepositorio.actualizar { e ->
            val antes = e.nivelesEfectivos.demostrado(moduloId)
            val delTest = ReglasNivel.nivelDelMiniTest(e.niveles.declarado(moduloId), antes)
            val nivelDelModulo = if (delTest.valor >= exigido.valor) delTest else exigido
            val despues = if (ReglasNivel.miniTestAprobado(correctas, estado.preguntas.size) && nivelDelModulo.valor > antes.valor) nivelDelModulo else antes
            e.copy(
                niveles = e.niveles.copy(demostrados = e.niveles.demostrados + (moduloId to despues)),
                ultimoMiniTest = ResultadoMiniTest(vacanteId, moduloId, estado.habilidad, correctas, estado.preguntas.size, antes, despues),
            )
        }
    }

    override fun onCleared() {
        cronometro.detener()
        super.onCleared()
    }
}

@Composable
fun MiniTestScreen(onBack: () -> Unit, onTerminado: () -> Unit, onSubirCertificado: () -> Unit, viewModel: MiniTestViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { if (estado.terminado) onTerminado() }
    MiniTestContenido(estado, onBack, viewModel::elegir, viewModel::siguiente, viewModel::enviar, onSubirCertificado)
}

@Composable
fun MiniTestContenido(
    estado: MiniTestUiState,
    onBack: () -> Unit,
    onElegir: (Int) -> Unit,
    onSiguiente: () -> Unit,
    onReintentar: () -> Unit,
    onSubirCertificado: () -> Unit,
) {
    val respondiendo = !estado.cronometro.terminado && estado.actual != null
    PantallaBase(
        titulo = if (respondiendo) "Mini-test de ${estado.habilidad} · ${estado.cronometro.indice + 1} de ${estado.preguntas.size}" else "Mini-test de ${estado.habilidad}",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                when {
                    respondiendo -> PrimaryButton(if (estado.cronometro.esUltima) "Terminar" else "Siguiente", onClick = onSiguiente, modifier = Modifier.fillMaxWidth())
                    estado.error != null -> PrimaryButton("Reintentar envío", onClick = onReintentar, enabled = !estado.enviando, cargando = estado.enviando, modifier = Modifier.fillMaxWidth())
                    else -> PrimaryButton("Corrigiendo…", onClick = {}, enabled = false, cargando = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            Text("Apruebas con 2 de 3. Tienes 45 segundos por pregunta.", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
            estado.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            if (respondiendo) {
                estado.actual?.let { PreguntaCronometrada(estado.cronometro, it, onElegir) }
            }
            TextButton(onClick = onSubirCertificado) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null)
                Text("  Prefiero subir un certificado como evidencia")
            }
        }
    }
}

// ---------- P16 Mi progreso ----------

enum class EstadoHabilidad(val etiqueta: String) { LOGRADO("Logrado"), EN_CURSO("En curso"), PENDIENTE("Pendiente") }

data class ProgresoUiState(
    val puesto: String = "",
    val antes: Int = 0,
    val ahora: Int = 0,
    val meta: Int = 100,
    val habilidades: List<Pair<String, EstadoHabilidad>> = emptyList(),
    val tareasSemana: Pair<Int, Int> = 0 to 0,
    val ranking: String? = null,
    val resultado: ResultadoMiniTest? = null,
    val premium: Boolean = true,
)

/** P16 Mi progreso: probabilidad antes / ahora / meta en la misma escala. */
@HiltViewModel
class ProgresoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<ProgresoUiState, Nothing>(ProgresoUiState()) {

    private val vacanteId: String = checkNotNull(savedStateHandle["vacanteId"])
    private val trasMiniTest: Boolean = savedStateHandle["trasMiniTest"] ?: false

    init {
        viewModelScope.launch {
            val vacante = vacantes.obtenerVacante(vacanteId) ?: return@launch
            estadoRepositorio.estado.collect { e ->
                val niveles = e.nivelesEfectivos
                val diagnostico = servicio.diagnostico(vacante, e)
                val plan = servicio.plan(vacante, e)
                val semana = plan.semanas.firstOrNull()?.tareas.orEmpty()
                setState {
                    copy(
                        puesto = vacante.resumen.puesto,
                        antes = e.probabilidadInicial[vacanteId] ?: diagnostico.probabilidadActual,
                        ahora = diagnostico.probabilidadActual,
                        meta = diagnostico.probabilidadMeta,
                        habilidades = vacante.requisitos.map { r ->
                            val estadoHabilidad = when {
                                MotorDiagnostico.cumplimiento(r, niveles) == CumplimientoRequisito.CUMPLE -> EstadoHabilidad.LOGRADO
                                plan.tareas.any { it.moduloId == r.habilidadId && it.id in e.tareasCompletadas } -> EstadoHabilidad.EN_CURSO
                                else -> EstadoHabilidad.PENDIENTE
                            }
                            r.nombre to estadoHabilidad
                        },
                        tareasSemana = semana.count { it.id in e.tareasCompletadas } to semana.size,
                        ranking = e.autoevaluacion?.let { "Top ${it.topPorcentaje} % en ${it.area.etiqueta} (privado)" },
                        resultado = e.ultimoMiniTest?.takeIf { trasMiniTest && it.vacanteId == vacanteId },
                        premium = e.premium,
                    )
                }
            }
        }
    }
}

@Composable
fun MiProgresoScreen(
    trasMiniTest: Boolean,
    onBack: () -> Unit,
    onVolverAlInicio: () -> Unit,
    onSeguirPlan: () -> Unit,
    onPostular: () -> Unit,
    onPremium: () -> Unit,
    viewModel: ProgresoViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    MiProgresoContenido(estado, trasMiniTest, onBack, onVolverAlInicio, onSeguirPlan, onPostular, onPremium)
}

@Composable
fun MiProgresoContenido(
    estado: ProgresoUiState,
    trasMiniTest: Boolean,
    onBack: () -> Unit,
    onVolverAlInicio: () -> Unit,
    onSeguirPlan: () -> Unit,
    onPostular: () -> Unit,
    onPremium: () -> Unit,
) {
    PantallaBase(
        titulo = "Mi progreso",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                if (trasMiniTest) {
                    PrimaryButton("Volver al inicio", onClick = onVolverAlInicio, modifier = Modifier.fillMaxWidth())
                    SecondaryButton("Seguir con mi plan", onClick = onSeguirPlan, modifier = Modifier.fillMaxWidth())
                } else {
                    PrimaryButton("Postular", onClick = onPostular, modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            Text(estado.puesto, style = MaterialTheme.typography.titleMedium)
            estado.resultado?.let { r ->
                TarjetaSeccion {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (r.aprobado) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (r.aprobado) MaterialTheme.colorScheme.primary else ChambaTema.colores.avisoAmbarTexto,
                        )
                        Spacer(Modifier.width(Dimensiones.espacioChico))
                        Text(if (r.aprobado) "¡Aprobaste!" else "Esta vez no", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Acertaste ${r.correctas} de ${r.total} en ${r.nombreHabilidad}.", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (r.nivelDespues != r.nivelAntes) {
                            "Tu nivel demostrado subió de ${r.nivelAntes.etiqueta} a ${r.nivelDespues.etiqueta}."
                        } else if (r.aprobado) {
                            "Ya tenías ese nivel demostrado."
                        } else {
                            "Repasa el curso y vuelve a intentarlo cuando quieras."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChambaTema.colores.textoSecundario,
                    )
                }
            }
            if (!estado.premium) {
                BloqueoPremium("El seguimiento es Premium", "Activa Premium para seguir tu avance semana a semana.", onDesbloquear = onPremium)
                return@ContenidoDesplazable
            }
            TarjetaSeccion(titulo = "Tu probabilidad") {
                ProbabilityScale(
                    porcentaje = estado.ahora,
                    etiqueta = "Ahora",
                    puntosExtra = listOf(PuntoEscala(estado.antes, "Antes"), PuntoEscala(estado.meta, "Meta")),
                )
            }
            TarjetaSeccion(titulo = "Por habilidad") {
                val colores = ChambaTema.colores
                estado.habilidades.forEach { (nombre, e) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        when (e) {
                            EstadoHabilidad.LOGRADO -> Etiqueta(e.etiqueta, colores.exitoFondo, colores.exitoTexto)
                            EstadoHabilidad.EN_CURSO -> Etiqueta(e.etiqueta, colores.avisoAmbarFondo, colores.avisoAmbarTexto)
                            EstadoHabilidad.PENDIENTE -> Etiqueta(e.etiqueta, MaterialTheme.colorScheme.surfaceVariant, colores.textoSecundario)
                        }
                    }
                }
            }
            TarjetaSeccion(titulo = "Esta semana") {
                Text("${estado.tareasSemana.first} de ${estado.tareasSemana.second} tareas cumplidas", style = MaterialTheme.typography.bodyLarge)
                estado.ranking?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario) }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun MiniTestPreview() {
    ChambaReadyTheme {
        MiniTestContenido(
            MiniTestUiState(
                "SQL",
                listOf(PreguntaOpcion("q1", "¿Qué cláusula filtra filas antes de agrupar?", listOf("HAVING", "WHERE", "No lo sé"), 1)),
                cronometro = EstadoCronometro(indice = 0, total = 1, segundos = 32),
            ),
            onBack = {}, onElegir = {}, onSiguiente = {}, onReintentar = {}, onSubirCertificado = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun MiProgresoPreview() {
    ChambaReadyTheme {
        MiProgresoContenido(
            ProgresoUiState(
                puesto = "Practicante de Analítica de Datos", antes = 42, ahora = 58, meta = 100,
                habilidades = listOf("SQL" to EstadoHabilidad.LOGRADO, "Power BI" to EstadoHabilidad.EN_CURSO, "Comunicación" to EstadoHabilidad.PENDIENTE),
                tareasSemana = 2 to 3, ranking = "Top 18 % en Data y Analytics (privado)",
                resultado = ResultadoMiniTest("v1", "sql", "SQL", 2, 3, NivelHabilidad.BASICO, NivelHabilidad.INTERMEDIO),
            ),
            trasMiniTest = true,
            onBack = {}, onVolverAlInicio = {}, onSeguirPlan = {}, onPostular = {}, onPremium = {},
        )
    }
}
