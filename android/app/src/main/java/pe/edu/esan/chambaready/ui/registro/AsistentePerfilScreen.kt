package pe.edu.esan.chambaready.ui.registro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.remoto.CvSesion
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.Evidencia
import pe.edu.esan.chambaready.core.domain.modelo.Habilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoEvidencia
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import javax.inject.Inject

enum class AutorMensaje { IA, USUARIO }

data class MensajeChat(val autor: AutorMensaje, val texto: String)

/** Lo que el asistente está preguntando ahora. */
sealed interface PreguntaChat {
    data class Nivel(val habilidad: Habilidad) : PreguntaChat
    data class Respaldo(val habilidad: Habilidad, val nivel: NivelHabilidad) : PreguntaChat
}

enum class RespuestaEvidencia(val etiqueta: String) {
    CERTIFICADO("Tengo un certificado"),
    LINK("Tengo un link"),
    AUN_NO("Aún no tengo"),
}

data class AsistenteUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val pregunta: PreguntaChat? = null,
    val respondidas: Int = 0,
    val total: Int = 1,
    val tieneCv: Boolean = false,
) {
    val terminado: Boolean get() = pregunta == null && respondidas >= total

    /** % de perfil completo: el CV aporta la base y cada habilidad respondida suma el resto. */
    val porcentaje: Int
        get() {
            val base = if (tieneCv) 40 else 10
            return base + (100 - base) * respondidas / total.coerceAtLeast(1)
        }
}

/**
 * P02 Asistente de perfil. En F2 es un guion fijo con las habilidades "leídas" del CV de prueba;
 * en F6 las preguntas las genera Gemini a partir del CV real. Solo pregunta lo que falta.
 */
@HiltViewModel
class AsistentePerfilViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    contenido: ContenidoRepository,
    cvSesion: CvSesion,
) : BaseViewModel<AsistenteUiState, Nothing>(AsistenteUiState()) {

    /** Con backend (F6): las que encontró la IA en el CV; en modo demo, las de prueba. */
    private val analisis = cvSesion.analisis
    private val habilidades = analisis?.habilidades?.keys?.mapNotNull { contenido.habilidad(it) }?.takeIf { it.isNotEmpty() }
        ?: contenido.habilidadesDetectadasEnCv()
    private val niveles = mutableMapOf<String, NivelHabilidad>()
    private val evidencias = mutableListOf<Evidencia>()

    init {
        viewModelScope.launch {
            val estado = estadoRepositorio.estado.first()
            val sugeridas = analisis?.habilidades.orEmpty()
            val introduccion = if (sugeridas.isNotEmpty()) {
                "Leí tu CV. Encontré ${sugeridas.size} habilidades: " +
                    habilidades.filter { it.id in sugeridas }.joinToString { "${it.nombre} (${sugeridas.getValue(it.id).etiqueta.lowercase()})" } +
                    ". Confírmame tu nivel en cada una."
            } else if (estado.cvNombre != null) {
                "Leí tu CV «${estado.cvNombre}». Encontré ${habilidades.size} habilidades. Te pregunto solo lo que falta."
            } else {
                "Vamos a armar tu perfil. Te haré algunas preguntas cortas sobre ${habilidades.size} habilidades comunes."
            }
            setState {
                copy(
                    mensajes = listOf(MensajeChat(AutorMensaje.IA, introduccion)),
                    total = habilidades.size,
                    tieneCv = estado.cvNombre != null,
                )
            }
            preguntarNivel(0)
        }
    }

    private fun preguntarNivel(indice: Int) {
        val habilidad = habilidades.getOrNull(indice)
        if (habilidad == null) {
            setState {
                copy(
                    pregunta = null,
                    mensajes = mensajes + MensajeChat(AutorMensaje.IA, "¡Listo! Revisa tu perfil en el siguiente paso."),
                )
            }
            return
        }
        setState {
            copy(
                pregunta = PreguntaChat.Nivel(habilidad),
                mensajes = mensajes + MensajeChat(AutorMensaje.IA, "¿Qué nivel tienes en ${habilidad.nombre}?"),
            )
        }
    }

    fun responderNivel(nivel: NivelHabilidad) {
        val pregunta = currentState.pregunta as? PreguntaChat.Nivel ?: return
        niveles[pregunta.habilidad.id] = nivel
        setState { copy(mensajes = mensajes + MensajeChat(AutorMensaje.USUARIO, nivel.etiqueta)) }
        if (nivel.valor > 0) {
            setState {
                copy(
                    pregunta = PreguntaChat.Respaldo(pregunta.habilidad, nivel),
                    mensajes = mensajes + MensajeChat(AutorMensaje.IA, "¿Tienes algo que lo respalde? Un certificado o un link a un proyecto."),
                )
            }
        } else {
            siguiente(pregunta.habilidad)
        }
    }

    fun responderEvidencia(respuesta: RespuestaEvidencia) {
        val pregunta = currentState.pregunta as? PreguntaChat.Respaldo ?: return
        setState { copy(mensajes = mensajes + MensajeChat(AutorMensaje.USUARIO, respuesta.etiqueta)) }
        if (respuesta != RespuestaEvidencia.AUN_NO) {
            // Queda "En revisión" hasta que se suba y se valide en P23 (F7). No sube el nivel todavía (regla 4).
            evidencias += Evidencia(
                id = "asistente-${pregunta.habilidad.id}",
                titulo = "${pregunta.habilidad.nombre}: ${if (respuesta == RespuestaEvidencia.CERTIFICADO) "certificado" else "link"} por subir",
                habilidadId = pregunta.habilidad.id,
                tipo = if (respuesta == RespuestaEvidencia.CERTIFICADO) TipoEvidencia.CERTIFICADO else TipoEvidencia.LINK,
                estado = EstadoEvidencia.EN_REVISION,
                nivel = pregunta.nivel,
            )
        }
        siguiente(pregunta.habilidad)
    }

    private fun siguiente(actual: Habilidad) {
        setState { copy(respondidas = respondidas + 1) }
        preguntarNivel(habilidades.indexOf(actual) + 1)
    }

    fun terminar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { estado ->
                estado.copy(
                    niveles = estado.niveles.copy(declarados = estado.niveles.declarados + niveles),
                    evidencias = estado.evidencias.filterNot { e -> evidencias.any { it.id == e.id } } + evidencias,
                )
            }
            alTerminar()
        }
    }
}

@Composable
fun AsistentePerfilScreen(
    onBack: () -> Unit,
    onTerminar: () -> Unit,
    viewModel: AsistentePerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    AsistentePerfilContenido(
        estado = estado,
        onBack = onBack,
        onNivel = viewModel::responderNivel,
        onEvidencia = viewModel::responderEvidencia,
        onTerminar = { viewModel.terminar(onTerminar) },
    )
}

@Composable
fun AsistentePerfilContenido(
    estado: AsistenteUiState,
    onBack: () -> Unit,
    onNivel: (NivelHabilidad) -> Unit,
    onEvidencia: (RespuestaEvidencia) -> Unit,
    onTerminar: () -> Unit,
) {
    val lista = rememberLazyListState()
    LaunchedEffect(estado.mensajes.size) {
        if (estado.mensajes.isNotEmpty()) lista.animateScrollToItem(estado.mensajes.size + 1)
    }
    PantallaBase(
        titulo = "Asistente de perfil",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                when (estado.pregunta) {
                    is PreguntaChat.Nivel -> ChipSelector(
                        opciones = NivelHabilidad.entries,
                        seleccionadas = emptySet(),
                        onClick = onNivel,
                        etiqueta = { it.etiqueta },
                    )
                    is PreguntaChat.Respaldo -> ChipSelector(
                        opciones = RespuestaEvidencia.entries,
                        seleccionadas = emptySet(),
                        onClick = onEvidencia,
                        etiqueta = { it.etiqueta },
                    )
                    null -> PrimaryButton("Ver mi perfil", onClick = onTerminar, enabled = estado.terminado, modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = lista,
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimensiones.paddingLateral, vertical = Dimensiones.espacioGrande),
            verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico),
        ) {
            item { StepProgress(pasoActual = 3, totalPasos = PASOS_REGISTRO) }
            item {
                Text(
                    "Perfil completo: ${estado.porcentaje} %",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                LinearProgressIndicator(
                    progress = { estado.porcentaje / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            itemsIndexed(estado.mensajes) { _, mensaje -> Burbuja(mensaje) }
        }
    }
}

@Composable
private fun Burbuja(mensaje: MensajeChat) {
    val esIa = mensaje.autor == AutorMensaje.IA
    Box(Modifier.fillMaxWidth(), contentAlignment = if (esIa) Alignment.CenterStart else Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (esIa) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
            contentColor = if (esIa) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(mensaje.texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun AsistentePreview() {
    ChambaReadyTheme {
        AsistentePerfilContenido(
            estado = AsistenteUiState(
                mensajes = listOf(
                    MensajeChat(AutorMensaje.IA, "Leí tu CV «CV_Ana.pdf». Encontré 6 habilidades. Te pregunto solo lo que falta."),
                    MensajeChat(AutorMensaje.IA, "¿Qué nivel tienes en SQL?"),
                    MensajeChat(AutorMensaje.USUARIO, "Básico"),
                    MensajeChat(AutorMensaje.IA, "¿Tienes algo que lo respalde? Un certificado o un link a un proyecto."),
                ),
                pregunta = PreguntaChat.Respaldo(Habilidad("sql", "SQL", pe.edu.esan.chambaready.core.domain.modelo.TipoHabilidad.TECNICA), NivelHabilidad.BASICO),
                respondidas = 0,
                total = 6,
                tieneCv = true,
            ),
            onBack = {}, onNivel = {}, onEvidencia = {}, onTerminar = {},
        )
    }
}
