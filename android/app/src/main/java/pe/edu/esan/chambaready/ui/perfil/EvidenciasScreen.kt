package pe.edu.esan.chambaready.ui.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.Evidencia
import pe.edu.esan.chambaready.core.domain.modelo.Habilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.TipoHabilidad
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.Etiqueta
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import pe.edu.esan.chambaready.ui.registro.Campo
import javax.inject.Inject

data class EvidenciasUiState(
    val evidencias: List<Evidencia> = emptyList(),
    val habilidades: List<Habilidad> = emptyList(),
) {
    fun nombreHabilidad(id: String) = habilidades.firstOrNull { it.id == id }?.nombre ?: id
}

/**
 * P23 Mis evidencias (gratis). Una evidencia nueva queda "En revisión" y no sube el nivel hasta que
 * se valida (regla 4). En F7 el archivo se sube a R2 con una URL prefirmada.
 */
@HiltViewModel
class EvidenciasViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
) : BaseViewModel<EvidenciasUiState, Nothing>(EvidenciasUiState()) {

    init {
        viewModelScope.launch {
            estadoRepositorio.estado.collect { e ->
                val ids = (contenido.habilidadesDetectadasEnCv().map { it.id } + e.niveles.declarados.keys).distinct()
                setState { copy(evidencias = e.evidencias, habilidades = ids.mapNotNull { contenido.habilidad(it) }) }
            }
        }
    }

    fun agregar(titulo: String, habilidadId: String, tipo: TipoEvidencia) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { e ->
                val nivel = e.niveles.declarado(habilidadId).takeIf { it.valor > 0 } ?: NivelHabilidad.BASICO
                val nueva = Evidencia("e${System.currentTimeMillis()}", titulo.trim(), habilidadId, tipo, EstadoEvidencia.EN_REVISION, nivel)
                e.copy(evidencias = e.evidencias + nueva)
            }
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch { estadoRepositorio.actualizar { e -> e.copy(evidencias = e.evidencias.filterNot { it.id == id }) } }
    }
}

@Composable
fun EvidenciasScreen(onBack: () -> Unit, viewModel: EvidenciasViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    EvidenciasContenido(estado, onBack, viewModel::agregar, viewModel::eliminar)
}

@Composable
fun EvidenciasContenido(
    estado: EvidenciasUiState,
    onBack: () -> Unit,
    onAgregar: (String, String, TipoEvidencia) -> Unit,
    onEliminar: (String) -> Unit,
) {
    var agregando by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Evidencia?>(null) }
    PantallaBase(
        titulo = "Mis evidencias",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton("Agregar evidencia", onClick = { agregando = true }, icono = Icons.Outlined.Add, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            Text(
                "Un certificado o un link a tu proyecto confirma tu nivel ante las empresas. Revisamos cada evidencia antes de validarla.",
                style = MaterialTheme.typography.bodyMedium,
                color = ChambaTema.colores.textoSecundario,
            )
            if (estado.evidencias.isEmpty()) {
                TarjetaSeccion { Text("Todavía no tienes evidencias.", style = MaterialTheme.typography.bodyLarge) }
            }
            estado.evidencias.forEach { evidencia ->
                TarjetaSeccion {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (evidencia.tipo == TipoEvidencia.LINK) Icons.Outlined.Link else Icons.Outlined.Description,
                            contentDescription = evidencia.tipo.etiqueta,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f).fillMaxWidth()) {
                            Text("  ${evidencia.titulo}", style = MaterialTheme.typography.bodyLarge)
                            Text("  ${estado.nombreHabilidad(evidencia.habilidadId)} · ${evidencia.nivel.etiqueta}", style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
                        }
                        IconButton(onClick = { porEliminar = evidencia }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${evidencia.titulo}")
                        }
                    }
                    EtiquetaEstado(evidencia.estado)
                }
            }
        }
    }

    if (agregando) {
        DialogoAgregar(estado.habilidades, onCancelar = { agregando = false }) { titulo, habilidad, tipo ->
            agregando = false
            onAgregar(titulo, habilidad, tipo)
        }
    }
    porEliminar?.let { evidencia ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("¿Eliminar evidencia?") },
            text = { Text("Se quitará «${evidencia.titulo}». Si estaba validada, tu nivel demostrado puede bajar.") },
            confirmButton = { TextButton(onClick = { porEliminar = null; onEliminar(evidencia.id) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun EtiquetaEstado(estado: EstadoEvidencia) {
    val colores = ChambaTema.colores
    when (estado) {
        EstadoEvidencia.VALIDADA -> Etiqueta(estado.etiqueta, colores.exitoFondo, colores.exitoTexto)
        EstadoEvidencia.EN_REVISION -> Etiqueta(estado.etiqueta, colores.avisoAmbarFondo, colores.avisoAmbarTexto)
        EstadoEvidencia.LEIDO -> Etiqueta(estado.etiqueta, MaterialTheme.colorScheme.surfaceVariant, colores.textoSecundario)
    }
}

@Composable
private fun DialogoAgregar(
    habilidades: List<Habilidad>,
    onCancelar: () -> Unit,
    onConfirmar: (String, String, TipoEvidencia) -> Unit,
) {
    var titulo by remember { mutableStateOf("") }
    var habilidad by remember { mutableStateOf(habilidades.firstOrNull()?.id) }
    var tipo by remember { mutableStateOf(TipoEvidencia.CERTIFICADO) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Agregar evidencia") },
        text = {
            Column {
                Campo("Título (por ejemplo: Certificado SQL)", titulo, { titulo = it }, error = null)
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                ChipSelector(TipoEvidencia.entries, setOf(tipo), { tipo = it }, { it.etiqueta })
                Text("Habilidad", style = MaterialTheme.typography.labelMedium)
                ChipSelector(habilidades.map { it.id }, setOfNotNull(habilidad), { habilidad = it }, { id -> habilidades.first { it.id == id }.nombre })
            }
        },
        confirmButton = {
            val elegida = habilidad
            TextButton(
                onClick = { if (elegida != null) onConfirmar(titulo, elegida, tipo) },
                enabled = titulo.isNotBlank() && elegida != null,
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun EvidenciasPreview() {
    ChambaReadyTheme {
        EvidenciasContenido(
            estado = EvidenciasUiState(
                evidencias = listOf(
                    Evidencia("e1", "Certificado de Excel avanzado", "excel", TipoEvidencia.CERTIFICADO, EstadoEvidencia.VALIDADA, NivelHabilidad.AVANZADO),
                    Evidencia("e2", "Repositorio de encuestas", "python", TipoEvidencia.LINK, EstadoEvidencia.EN_REVISION, NivelHabilidad.BASICO),
                ),
                habilidades = listOf(Habilidad("excel", "Excel", TipoHabilidad.TECNICA), Habilidad("python", "Python", TipoHabilidad.TECNICA)),
            ),
            onBack = {}, onAgregar = { _, _, _ -> }, onEliminar = {},
        )
    }
}
