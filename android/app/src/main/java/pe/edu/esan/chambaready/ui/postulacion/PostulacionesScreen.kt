package pe.edu.esan.chambaready.ui.postulacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.EstadoPostulacion
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import pe.edu.esan.chambaready.ui.comun.formatoFecha
import java.time.LocalDate
import javax.inject.Inject

data class PostulacionesUiState(val postulaciones: List<Postulacion> = emptyList()) {
    val activas get() = postulaciones.filter { !it.estado.cerrada }.sortedByDescending { it.fechaEnvio }
    val cerradas get() = postulaciones.filter { it.estado.cerrada }.sortedByDescending { it.fechaEnvio }
}

/** P18 Mis postulaciones. En F4 el estado lo actualiza la empresa desde la web (E04/E07). */
@HiltViewModel
class PostulacionesViewModel @Inject constructor(
    estadoRepositorio: EstadoUsuarioRepository,
    cuenta: CuentaRepository,
) : BaseViewModel<PostulacionesUiState, Nothing>(PostulacionesUiState()) {
    init {
        viewModelScope.launch { estadoRepositorio.estado.collect { e -> setState { copy(postulaciones = e.postulaciones) } } }
        // Con backend: trae los estados que movió la empresa (E04). Sin conexión, se queda lo guardado.
        viewModelScope.launch {
            try {
                val remotas = cuenta.postulaciones() ?: return@launch
                estadoRepositorio.actualizar { it.copy(postulaciones = remotas) }
            } catch (_: ErrorRemoto) {
                // Se muestra lo último guardado en el teléfono.
            }
        }
    }
}

private enum class PestanaPostulaciones(val etiqueta: String) { ACTIVAS("Activas"), CERRADAS("Cerradas") }

@Composable
fun PostulacionesScreen(
    onMejorar: (String) -> Unit,
    onResponderPreguntas: (vacanteId: String, postulacionId: String) -> Unit = { _, _ -> },
    viewModel: PostulacionesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PostulacionesContenido(estado, onMejorar, onResponderPreguntas)
}

@Composable
fun PostulacionesContenido(
    estado: PostulacionesUiState,
    onMejorar: (String) -> Unit,
    onResponderPreguntas: (vacanteId: String, postulacionId: String) -> Unit = { _, _ -> },
) {
    var pestana by rememberSaveable { mutableStateOf(PestanaPostulaciones.ACTIVAS) }
    Surface(color = MaterialTheme.colorScheme.background) {
        ContenidoDesplazable(PaddingValues(), modifier = Modifier.statusBarsPadding()) {
            Text("Mis postulaciones", style = MaterialTheme.typography.headlineSmall)
            ChipSelector(PestanaPostulaciones.entries, setOf(pestana), { pestana = it }, { "${it.etiqueta} (${if (it == PestanaPostulaciones.ACTIVAS) estado.activas.size else estado.cerradas.size})" })
            val lista = if (pestana == PestanaPostulaciones.ACTIVAS) estado.activas else estado.cerradas
            if (lista.isEmpty()) {
                Text(
                    if (pestana == PestanaPostulaciones.ACTIVAS) "Aún no tienes postulaciones activas." else "No tienes postulaciones cerradas.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChambaTema.colores.textoSecundario,
                )
            }
            lista.forEach { p -> TarjetaPostulacion(p, onMejorar, onResponderPreguntas) }
        }
    }
}

@Composable
private fun TarjetaPostulacion(p: Postulacion, onMejorar: (String) -> Unit, onResponderPreguntas: (String, String) -> Unit) {
    TarjetaSeccion {
        Text(p.puesto, style = MaterialTheme.typography.titleMedium)
        Text("${p.empresa} · enviada el ${formatoFecha(p.fechaEnvio)}", style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
        LineaEstado(p.estado)
        val postulacionId = p.id
        if (p.preguntasPendientes && postulacionId != null) {
            // Regla 13: la empresa (con plan) quiere confirmar tus requisitos.
            Text("La empresa te pide responder las preguntas filtro.", style = MaterialTheme.typography.bodyMedium)
            PrimaryButton("Responder preguntas", onClick = { onResponderPreguntas(p.vacanteId, postulacionId) }, modifier = Modifier.fillMaxWidth())
        }
        if (p.estado == EstadoPostulacion.NO_SELECCIONADO) {
            Text("Motivo de la empresa:", style = MaterialTheme.typography.labelMedium)
            Text(p.motivoDescarte ?: "La empresa no indicó un motivo.", style = MaterialTheme.typography.bodyMedium)
            SecondaryButton("Mejorar esto", onClick = { onMejorar(p.vacanteId) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Línea Enviada → Revisión → Entrevista → Resultado. Se lee como una frase para TalkBack. */
@Composable
private fun LineaEstado(estado: EstadoPostulacion) {
    val pasos = listOf("Enviada", "Revisión", "Entrevista", "Resultado")
    val colorResultado = if (estado == EstadoPostulacion.NO_SELECCIONADO) ChambaTema.colores.avisoRojoTexto else MaterialTheme.colorScheme.primary
    Column(Modifier.clearAndSetSemantics { contentDescription = "Estado: ${estado.etiqueta}" }) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pasos.indices.forEach { i ->
                val hecho = i <= estado.paso
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(if (hecho) (if (i == 3) colorResultado else MaterialTheme.colorScheme.primary) else ChambaTema.colores.borde, CircleShape),
                )
            }
        }
        Row {
            pasos.forEachIndexed { i, paso ->
                Text(
                    if (i == 3 && estado.cerrada) estado.etiqueta else paso,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == estado.paso) MaterialTheme.colorScheme.onBackground else ChambaTema.colores.textoSecundario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PostulacionesPreview() {
    val hoy = LocalDate.now()
    ChambaReadyTheme {
        PostulacionesContenido(
            PostulacionesUiState(
                listOf(
                    Postulacion("v4", "Practicante de Finanzas Corporativas", "Grupo Pacífico", EstadoPostulacion.ENTREVISTA, hoy.minusDays(6)),
                    Postulacion("v1", "Practicante de Analítica de Datos", "Banco Andino", EstadoPostulacion.ENVIADA, hoy),
                ),
            ),
            onMejorar = {},
        )
    }
}
