package pe.edu.esan.chambaready.ui.perfil

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.componentes.WeekHoursEditor
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.registro.PASOS_REGISTRO
import java.time.DayOfWeek
import javax.inject.Inject

data class TiempoLibreUiState(
    val horas: HorasLibres = HorasLibres(),
    val cargado: Boolean = false,
)

/**
 * P04 (registro) y P22 (editar desde Perfil). Usan el mismo editor por día. Al guardar en P22 se
 * recalculan los planes activos y se avisa qué vacantes dejan de ser alcanzables.
 */
@HiltViewModel
class TiempoLibreViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val vacantes: VacanteRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<TiempoLibreUiState, Nothing>(TiempoLibreUiState()) {

    init {
        viewModelScope.launch {
            setState { copy(horas = estadoRepositorio.estado.first().horasLibres, cargado = true) }
        }
    }

    fun cambiar(horas: HorasLibres) = setState { copy(horas = horas) }

    /** Guarda y devuelve los puestos que eran viables y con las nuevas horas ya no lo son. */
    fun guardar(alTerminar: (dejanDeSerAlcanzables: List<String>) -> Unit) {
        val nuevas = currentState.horas
        viewModelScope.launch {
            val antes = estadoRepositorio.estado.first()
            val despues = antes.copy(horasLibres = nuevas)
            val afectadas = antes.planesActivos.mapNotNull { id -> vacantes.obtenerVacante(id) }.filter { vacante ->
                servicio.diagnostico(vacante, antes).viabilidad.esViable && !servicio.diagnostico(vacante, despues).viabilidad.esViable
            }.map { it.resumen.puesto }
            estadoRepositorio.actualizar { it.copy(horasLibres = nuevas) }
            alTerminar(afectadas)
        }
    }
}

@Composable
fun TiempoLibreScreen(
    modoEdicion: Boolean,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: TiempoLibreViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var aviso by remember { mutableStateOf<List<String>?>(null) }
    TiempoLibreContenido(
        estado = estado,
        modoEdicion = modoEdicion,
        onBack = onBack,
        onCambio = viewModel::cambiar,
        onGuardar = {
            viewModel.guardar { afectadas ->
                if (modoEdicion && afectadas.isNotEmpty()) aviso = afectadas else onGuardado()
            }
        },
    )
    aviso?.let { afectadas ->
        AlertDialog(
            onDismissRequest = { aviso = null; onGuardado() },
            title = { Text("Revisa tus planes") },
            text = {
                Text(
                    "Con tus nuevas horas ya no llegas a tiempo a: ${afectadas.joinToString(", ")}. " +
                        "Puedes subir tus horas otra vez, buscar una vacante parecida o guardarla como meta.",
                )
            },
            confirmButton = { TextButton(onClick = { aviso = null; onGuardado() }) { Text("Entendido") } },
        )
    }
}

@Composable
fun TiempoLibreContenido(
    estado: TiempoLibreUiState,
    modoEdicion: Boolean,
    onBack: () -> Unit,
    onCambio: (HorasLibres) -> Unit,
    onGuardar: () -> Unit,
) {
    PantallaBase(
        titulo = if (modoEdicion) "Editar tiempo libre" else "Mi tiempo libre",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(
                    if (modoEdicion) "Guardar cambios" else "Continuar",
                    onClick = onGuardar,
                    enabled = estado.cargado,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            if (!modoEdicion) StepProgress(pasoActual = 5, totalPasos = PASOS_REGISTRO)
            Text(
                "¿Cuántas horas puedes estudiar cada día? Usamos esto para saber si llegas a tiempo a cada vacante.",
                style = MaterialTheme.typography.bodyLarge,
                color = ChambaTema.colores.textoSecundario,
            )
            WeekHoursEditor(horasLibres = estado.horas, onCambio = onCambio)
            Text(
                "Máximo 8 h por día, en pasos de media hora. Puedes cambiarlo cuando quieras desde tu perfil.",
                style = MaterialTheme.typography.bodySmall,
                color = ChambaTema.colores.textoSecundario,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun TiempoLibrePreview() {
    ChambaReadyTheme {
        TiempoLibreContenido(
            estado = TiempoLibreUiState(HorasLibres(mapOf(DayOfWeek.MONDAY to 1.5, DayOfWeek.SATURDAY to 4.0)), cargado = true),
            modoEdicion = true,
            onBack = {}, onCambio = {}, onGuardar = {},
        )
    }
}
