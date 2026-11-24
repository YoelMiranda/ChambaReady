package pe.edu.esan.chambaready.ui.premium

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import pe.edu.esan.chambaready.ui.comun.formatoSoles
import javax.inject.Inject

data class PremiumUiState(val precioCentimos: Long = 0, val activo: Boolean = false)

/** P11. En F2 se activa en modo prueba; en F8 se paga con una suscripción de Mercado Pago. */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val cuenta: CuentaRepository,
    contenido: ContenidoRepository,
) : BaseViewModel<PremiumUiState, Nothing>(PremiumUiState(precioCentimos = contenido.precioPremiumCentimos())) {

    init {
        viewModelScope.launch { estadoRepositorio.estado.collect { e -> setState { copy(activo = e.premium) } } }
    }

    /**
     * F8 SIMULADO: no se cobra nada. Con backend registra un pago de prueba (30 días); en modo demo
     * solo se activa en el teléfono.
     */
    fun activar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            try {
                cuenta.pagarPremium()
            } catch (_: ErrorRemoto) {
                return@launch // Sin conexión no se activa: se puede reintentar.
            }
            estadoRepositorio.actualizar { it.copy(premium = true) }
            alTerminar()
        }
    }
}

private val beneficios = listOf(
    "Resumen del diagnóstico" to true,
    "Mis evidencias" to true,
    "Detalle de brechas y cursos" to false,
    "Plan semanal en tus horas libres" to false,
    "Seguimiento y Mis pendientes" to false,
    "Editar datos y habilidades" to false,
)

/**
 * Al activar, quien llama decide a dónde volver: si venía del diagnóstico abre el plan; si no,
 * vuelve a la pantalla anterior (ver la navegación).
 */
@Composable
fun PremiumScreen(onBack: () -> Unit, onActivado: () -> Unit, viewModel: PremiumViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PremiumContenido(estado, onBack) { viewModel.activar(onActivado) }
}

@Composable
fun PremiumContenido(estado: PremiumUiState, onBack: () -> Unit, onActivar: () -> Unit) {
    PantallaBase(
        titulo = "Premium",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(
                    if (estado.activo) "Premium ya está activo" else "Activar Premium (modo prueba)",
                    onClick = onActivar,
                    enabled = !estado.activo,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            Text("${formatoSoles(estado.precioCentimos)} al mes", style = MaterialTheme.typography.headlineMedium)
            Text("Cancela cuando quieras.", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
            TarjetaSeccion {
                Row {
                    Text("", modifier = Modifier.weight(1f))
                    Text("Gratis", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                    Text("Premium", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                }
                beneficios.forEach { (beneficio, gratis) ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "$beneficio: ${if (gratis) "incluido gratis" else "solo con Premium"}"
                        },
                    ) {
                        Text(beneficio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (gratis) Icons.Outlined.Check else Icons.Outlined.Close,
                            contentDescription = null,
                            tint = if (gratis) MaterialTheme.colorScheme.primary else ChambaTema.colores.textoSecundario,
                            modifier = Modifier.width(64.dp),
                        )
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(72.dp))
                    }
                }
            }
            Surface(color = ChambaTema.colores.avisoAmbarFondo, contentColor = ChambaTema.colores.avisoAmbarTexto, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(Dimensiones.espacioGrande), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(Dimensiones.espacioChico))
                    Text(
                        "Pagar no sube tu lugar ante las empresas ni te da vacantes exclusivas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PremiumPreview() {
    ChambaReadyTheme { PremiumContenido(PremiumUiState(999, activo = false), onBack = {}, onActivar = {}) }
}
