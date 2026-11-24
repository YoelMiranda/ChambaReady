package pe.edu.esan.chambaready.ui.postulacion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.remoto.RespuestasFiltroSesion
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.SealBadge
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.EstadoPostulacion
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.reglas.DeclaracionRequisito
import pe.edu.esan.chambaready.core.domain.reglas.ReglasPostulacion
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.CargandoPantalla
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.Etiqueta
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import java.time.LocalDate
import javax.inject.Inject

data class PostularUiState(
    val vacante: VacanteDetalle? = null,
    /** Lo que declara por requisito; si no tocó nada, se asume "Lo cumplo". */
    val loCumple: Map<String, Boolean> = emptyMap(),
    val sellos: Map<String, TipoSello> = emptyMap(),
    val hayPreguntas: Boolean = false,
    val preguntasRespondidas: Boolean = false,
    val aciertos: Int = 0,
    val declaracionJurada: Boolean = false,
    val enviando: Boolean = false,
    /** Con backend, el servidor corrige: no se sabe cuántas acertó hasta enviar. */
    val correccionEnServidor: Boolean = false,
) {
    val puedePostular: Boolean
        get() = vacante?.let { v ->
            ReglasPostulacion.puedePostular(v.requisitos.map { DeclaracionRequisito(it.tipo, loCumple[it.id] ?: true) })
        } ?: false

    val puedeEnviar: Boolean get() = puedePostular && declaracionJurada && !enviando
}

/**
 * P17 Postular. Postular nunca se bloquea, salvo por un requisito filtro que declara no cumplir
 * (regla 7). Si la empresa no tiene evaluaciones, no hay preguntas y los sellos quedan como
 * declarados (regla 11).
 */
@HiltViewModel
class PostularViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
    private val cuenta: CuentaRepository,
    private val sesion: RespuestasFiltroSesion,
) : BaseViewModel<PostularUiState, String>(PostularUiState()) {

    private val vacanteId: String = checkNotNull(savedStateHandle["vacanteId"])

    /** Último estado del usuario recibido; lo usa [declarar] para recalcular los sellos. */
    private var ultimoEstado: EstadoUsuario? = null

    init {
        viewModelScope.launch {
            val vacante = vacantes.obtenerVacante(vacanteId) ?: return@launch
            // Al volver de P24 el estado cambia (respuestas guardadas) y los sellos se recalculan solos.
            estadoRepositorio.estado.collect { e ->
                ultimoEstado = e
                val respuestas = e.respuestasFiltro[vacanteId]
                setState {
                    copy(
                        vacante = vacante,
                        sellos = servicio.sellos(vacante, e, loCumple),
                        hayPreguntas = vacante.evaluacionActiva && vacante.empresaConEvaluaciones,
                        preguntasRespondidas = respuestas != null,
                        aciertos = respuestas?.count { it } ?: 0,
                        correccionEnServidor = sesion.obtener(vacanteId) != null,
                    )
                }
            }
        }
    }

    fun declarar(requisitoId: String, cumple: Boolean) {
        val vacante = currentState.vacante ?: return
        val estadoUsuario = ultimoEstado ?: return
        val nuevo = currentState.loCumple + (requisitoId to cumple)
        setState { copy(loCumple = nuevo, sellos = servicio.sellos(vacante, estadoUsuario, nuevo)) }
    }

    fun alternarDeclaracion() = setState { copy(declaracionJurada = !declaracionJurada) }

    fun enviar(alTerminar: () -> Unit) {
        val estado = currentState
        val vacante = estado.vacante ?: return
        if (!estado.puedeEnviar) return
        setState { copy(enviando = true) }
        viewModelScope.launch {
            val local = Postulacion(
                vacanteId = vacanteId,
                puesto = vacante.resumen.puesto,
                empresa = vacante.resumen.empresa,
                estado = EstadoPostulacion.ENVIADA,
                fechaEnvio = servicio.hoy(),
                sellos = estado.sellos,
            )
            try {
                // Con backend, la guardada es la del servidor (él decide los sellos); en demo, la local.
                val guardada = cuenta.postular(local, estado.loCumple, estado.declaracionJurada, sesion.obtener(vacanteId).orEmpty())
                sesion.borrar(vacanteId)
                estadoRepositorio.actualizar { e ->
                    e.copy(postulaciones = e.postulaciones.filterNot { it.vacanteId == vacanteId } + guardada)
                }
                alTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorRemoto) {
                sendEvent(e.message.orEmpty())
            } finally {
                setState { copy(enviando = false) }
            }
        }
    }
}

@Composable
fun PostularScreen(
    onBack: () -> Unit,
    onResponderPreguntas: (String) -> Unit,
    onEnviada: () -> Unit,
    viewModel: PostularViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }
    PostularContenido(estado, onBack, viewModel::declarar, viewModel::alternarDeclaracion, onResponderPreguntas, snackbar) { viewModel.enviar(onEnviada) }
}

@Composable
fun PostularContenido(
    estado: PostularUiState,
    onBack: () -> Unit,
    onDeclarar: (String, Boolean) -> Unit,
    onDeclaracionJurada: () -> Unit,
    onResponderPreguntas: (String) -> Unit,
    snackbar: SnackbarHostState? = null,
    onEnviar: () -> Unit,
) {
    val vacante = estado.vacante
    PantallaBase(
        titulo = "Postular",
        onBack = onBack,
        snackbarHostState = snackbar,
        barraInferior = {
            BarraAcciones {
                if (vacante != null && !estado.puedePostular) {
                    Text(
                        "Declaraste no cumplir un requisito filtro: esta empresa no recibe postulaciones sin él.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                PrimaryButton(
                    "Enviar postulación",
                    onClick = onEnviar,
                    enabled = estado.puedeEnviar,
                    cargando = estado.enviando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        if (vacante == null) {
            CargandoPantalla()
            return@PantallaBase
        }
        ContenidoDesplazable(padding) {
            Text(vacante.resumen.puesto, style = MaterialTheme.typography.titleMedium)
            Text(vacante.resumen.empresa, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)

            TarjetaSeccion(titulo = "Tus requisitos") {
                vacante.requisitos.sortedBy { it.tipo.ordinal }.forEachIndexed { i, requisito ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    val sello = estado.sellos[requisito.id] ?: TipoSello.DECLARADO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${requisito.nombre} · ${requisito.nivelExigido.etiqueta}", style = MaterialTheme.typography.bodyLarge)
                            if (requisito.tipo == TipoRequisito.FILTRO) {
                                Etiqueta("Filtro", ChambaTema.colores.avisoRojoFondo, ChambaTema.colores.avisoRojoTexto)
                            }
                        }
                        SealBadge(sello)
                    }
                    if (sello != TipoSello.CONFIRMADO) {
                        val cumple = estado.loCumple[requisito.id] ?: true
                        ChipSelector(
                            opciones = listOf(true, false),
                            seleccionadas = setOf(cumple),
                            onClick = { onDeclarar(requisito.id, it) },
                            etiqueta = { if (it) "Lo cumplo" else "No lo cumplo" },
                        )
                    }
                }
            }

            if (estado.hayPreguntas) {
                TarjetaSeccion(titulo = "Preguntas filtro") {
                    if (estado.preguntasRespondidas) {
                        Text(
                            if (estado.correccionEnServidor) {
                                "Respondiste las preguntas. Al enviar, las bien respondidas confirman su requisito."
                            } else {
                                "Respondiste las preguntas: ${estado.aciertos} de 2 correctas. Las bien respondidas confirman su requisito."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            "La empresa pide 2 preguntas de 45 segundos cada una. Confirman tus requisitos más importantes.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        SecondaryButton(
                            "Responder ahora",
                            onClick = { onResponderPreguntas(vacante.resumen.id) },
                            icono = Icons.Outlined.Timer,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Text(
                    "Esta vacante no tiene preguntas filtro: tus requisitos se envían como declarados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChambaTema.colores.textoSecundario,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = estado.declaracionJurada, role = Role.Checkbox, onValueChange = { onDeclaracionJurada() }),
            ) {
                Checkbox(checked = estado.declaracionJurada, onCheckedChange = null)
                Text(
                    "Declaro que la información de mi postulación es verdadera.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun PostularPreview() {
    val vacante = MockDatos.vacantes(LocalDate.now()).first()
    ChambaReadyTheme {
        PostularContenido(
            PostularUiState(
                vacante = vacante,
                sellos = mapOf("v1r1" to TipoSello.DECLARADO, "v1r2" to TipoSello.CONFIRMADO, "v1r3" to TipoSello.NO_CUMPLE, "v1r4" to TipoSello.DECLARADO),
                loCumple = mapOf("v1r3" to false),
                hayPreguntas = true,
            ),
            onBack = {}, onDeclarar = { _, _ -> }, onDeclaracionJurada = {}, onResponderPreguntas = {}, onEnviar = {},
        )
    }
}
