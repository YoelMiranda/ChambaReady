package pe.edu.esan.chambaready.ui.vacante

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.CountdownChip
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.ProbabilityScale
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.RequisitoVacante
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.navigation.NavRoutes
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.CargandoPantalla
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.Etiqueta
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import java.time.LocalDate
import javax.inject.Inject

data class RequisitoUi(val requisito: RequisitoVacante, val cumplimiento: CumplimientoRequisito)

data class DetalleVacanteUiState(
    val vacante: VacanteDetalle? = null,
    val diasRestantes: Int = 0,
    val probabilidad: Int = 0,
    val requisitos: List<RequisitoUi> = emptyList(),
    val yaPostulo: Boolean = false,
    val noEncontrada: Boolean = false,
)

/** P09 Detalle de vacante (pantalla clave). La vacante llega por la ruta tipada. */
@HiltViewModel
class DetalleVacanteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<DetalleVacanteUiState, Nothing>(DetalleVacanteUiState()) {

    private val vacanteId = savedStateHandle.toRoute<NavRoutes.DetalleVacante>().vacanteId

    init {
        viewModelScope.launch {
            val vacante = vacantes.obtenerVacante(vacanteId)
            if (vacante == null) {
                setState { copy(noEncontrada = true) }
                return@launch
            }
            estadoRepositorio.estado.collect { e ->
                val niveles = e.nivelesEfectivos
                setState {
                    copy(
                        vacante = vacante,
                        diasRestantes = servicio.diasRestantes(vacante),
                        probabilidad = servicio.probabilidad(vacante, e),
                        requisitos = vacante.requisitos.sortedBy { it.tipo.ordinal }.map { RequisitoUi(it, MotorDiagnostico.cumplimiento(it, niveles)) },
                        yaPostulo = e.postulaciones.any { it.vacanteId == vacanteId },
                    )
                }
            }
        }
    }
}

@Composable
fun DetalleVacanteScreen(
    onBack: () -> Unit,
    onMejorar: (String) -> Unit,
    onPostular: (String) -> Unit,
    viewModel: DetalleVacanteViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    DetalleVacanteContenido(estado, onBack, onMejorar, onPostular)
}

@Composable
fun DetalleVacanteContenido(
    estado: DetalleVacanteUiState,
    onBack: () -> Unit,
    onMejorar: (String) -> Unit,
    onPostular: (String) -> Unit,
) {
    val vacante = estado.vacante
    PantallaBase(
        titulo = "Detalle de vacante",
        onBack = onBack,
        barraInferior = {
            if (vacante != null && estado.diasRestantes >= 0) {
                BarraAcciones {
                    PrimaryButton("Mejorar mis probabilidades", onClick = { onMejorar(vacante.resumen.id) }, modifier = Modifier.fillMaxWidth())
                    if (!estado.yaPostulo) {
                        SecondaryButton("Postular sin preparación", onClick = { onPostular(vacante.resumen.id) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { padding ->
        when {
            estado.noEncontrada -> ContenidoDesplazable(padding) { Text("Esta vacante ya no está disponible.", style = MaterialTheme.typography.bodyLarge) }
            vacante == null -> CargandoPantalla()
            else -> ContenidoDesplazable(padding) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(vacante.resumen.empresa, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                    Text(vacante.resumen.puesto, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOfNotNull(vacante.resumen.modalidad.etiqueta, vacante.resumen.sueldo, vacante.area.etiqueta).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChambaTema.colores.textoSecundario,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico), verticalAlignment = Alignment.CenterVertically) {
                    CountdownChip(estado.diasRestantes)
                    if (estado.yaPostulo) Etiqueta("Ya postulaste", ChambaTema.colores.exitoFondo, ChambaTema.colores.exitoTexto)
                }
                TarjetaSeccion(titulo = "Tu probabilidad") {
                    ProbabilityScale(porcentaje = estado.probabilidad)
                    Text(
                        "Se calcula con los requisitos que cumples y su peso (filtro, obligatorio, deseable). Pagar Premium no la cambia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChambaTema.colores.textoSecundario,
                    )
                }
                TarjetaSeccion(titulo = "Qué harás") {
                    Text(vacante.descripcion, style = MaterialTheme.typography.bodyMedium)
                    vacante.queHaras.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
                TarjetaSeccion(titulo = "Requisitos") {
                    estado.requisitos.forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        FilaRequisito(r)
                    }
                }
            }
        }
    }
}

/** Fila de requisito: tipo (con texto), habilidad y nivel, y si cumple. */
@Composable
fun FilaRequisito(r: RequisitoUi) {
    val colores = ChambaTema.colores
    val (fondoTipo, textoTipo) = when (r.requisito.tipo) {
        TipoRequisito.FILTRO -> colores.avisoRojoFondo to colores.avisoRojoTexto
        TipoRequisito.OBLIGATORIO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TipoRequisito.DESEABLE -> MaterialTheme.colorScheme.surfaceVariant to colores.textoSecundario
    }
    val colorCumple: Color = when (r.cumplimiento) {
        CumplimientoRequisito.CUMPLE -> colores.exitoTexto
        CumplimientoRequisito.PARCIAL -> colores.avisoAmbarTexto
        CumplimientoRequisito.FALTA -> colores.avisoRojoTexto
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Etiqueta(r.requisito.tipo.etiqueta, fondoTipo, textoTipo)
            Text("${r.requisito.nombre} · ${r.requisito.nivelExigido.etiqueta}", style = MaterialTheme.typography.bodyLarge)
        }
        Text(r.cumplimiento.etiqueta, style = MaterialTheme.typography.labelMedium, color = colorCumple)
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun DetalleVacantePreview() {
    val vacante = MockDatos.vacantes(LocalDate.now()).first()
    ChambaReadyTheme {
        DetalleVacanteContenido(
            DetalleVacanteUiState(
                vacante = vacante,
                diasRestantes = 12,
                probabilidad = 58,
                requisitos = vacante.requisitos.map { RequisitoUi(it, CumplimientoRequisito.entries[it.id.last().code % 3]) },
            ),
            onBack = {}, onMejorar = {}, onPostular = {},
        )
    }
}
