package pe.edu.esan.chambaready.ui.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
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
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.registro.PASOS_REGISTRO
import javax.inject.Inject

data class AreaUiState(val seleccionada: AreaInteres? = null)

/** P21: una sola área principal. La autoevaluación, el ranking y las recomendaciones se calculan dentro de ella. */
@HiltViewModel
class AreaInteresViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
) : BaseViewModel<AreaUiState, Nothing>(AreaUiState()) {

    init {
        viewModelScope.launch {
            val areaGuardada = estadoRepositorio.estado.first().area

            setState {
                copy(seleccionada = areaGuardada)
            }
        }
    }

    fun elegir(area: AreaInteres) {
        viewModelScope.launch {
            setState { copy(seleccionada = area) }
        }
    }

    fun guardar(alTerminar: () -> Unit) {
        val area = currentState.seleccionada ?: return
        viewModelScope.launch {
            estadoRepositorio.actualizar { it.copy(area = area) }
            alTerminar()
        }
    }
}

@Composable
fun AreaInteresScreen(
    modoEdicion: Boolean,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: AreaInteresViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    AreaInteresContenido(estado, modoEdicion, onBack, viewModel::elegir) { viewModel.guardar(onGuardado) }
}

@Composable
fun AreaInteresContenido(
    estado: AreaUiState,
    modoEdicion: Boolean,
    onBack: () -> Unit,
    onElegir: (AreaInteres) -> Unit,
    onGuardar: () -> Unit,
) {
    PantallaBase(
        titulo = "Área de interés",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(
                    if (modoEdicion) "Guardar cambios" else "Continuar",
                    onClick = onGuardar,
                    enabled = estado.seleccionada != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            if (!modoEdicion) StepProgress(pasoActual = 6, totalPasos = PASOS_REGISTRO)
            Text(
                "Elige tu área principal. Tu autoevaluación, tu ranking privado y las vacantes recomendadas se calculan dentro de ella.",
                style = MaterialTheme.typography.bodyLarge,
                color = ChambaTema.colores.textoSecundario,
            )
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico)) {
                AreaInteres.entries.forEach { area ->
                    val elegida = area == estado.seleccionada
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = elegida, onClick = { onElegir(area) }, role = Role.RadioButton),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = elegida, onClick = null)
                            Text(area.etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun AreaInteresPreview() {
    ChambaReadyTheme {
        AreaInteresContenido(AreaUiState(AreaInteres.DATA_ANALYTICS), modoEdicion = false, onBack = {}, onElegir = {}, onGuardar = {})
    }
}
