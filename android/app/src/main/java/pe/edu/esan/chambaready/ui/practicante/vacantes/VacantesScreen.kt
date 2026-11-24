package pe.edu.esan.chambaready.ui.practicante.vacantes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.VacancyCard
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.reglas.ReglasCuentaRegresiva
import java.time.LocalDate

/** P08 Vacantes. Conecta el ViewModel con el contenido sin estado. */
@Composable
fun VacantesScreen(
    onAbrirVacante: (String) -> Unit,
    viewModel: VacantesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    VacantesContenido(
        estado = estado,
        onAbrirVacante = onAbrirVacante,
        onBuscar = viewModel::buscar,
        onFiltro = viewModel::alternarFiltro,
        onReintentar = viewModel::cargar,
    )
}

@Composable
fun VacantesContenido(
    estado: VacantesUiState,
    onAbrirVacante: (String) -> Unit,
    onBuscar: (String) -> Unit,
    onFiltro: (FiltroVacantes) -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        when {
            estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            estado.error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(Dimensiones.paddingLateral),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(estado.error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Dimensiones.espacioGrande))
                PrimaryButton("Reintentar", onClick = onReintentar, modifier = Modifier.fillMaxWidth())
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = Dimensiones.paddingLateral, vertical = Dimensiones.espacioGrande),
                verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioMedio),
            ) {
                item {
                    Text("Vacantes", style = MaterialTheme.typography.headlineSmall)
                    Text("Ordenadas por tu probabilidad", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                }
                item {
                    OutlinedTextField(
                        value = estado.busqueda,
                        onValueChange = onBuscar,
                        placeholder = { Text("Buscar por puesto o empresa") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    ChipSelector(
                        opciones = FiltroVacantes.entries,
                        seleccionadas = estado.filtros,
                        onClick = onFiltro,
                        etiqueta = { it.etiqueta },
                    )
                }
                if (estado.vacantes.isEmpty()) {
                    item {
                        Text(
                            "No hay vacantes con esos filtros.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChambaTema.colores.textoSecundario,
                        )
                    }
                }
                items(estado.vacantes, key = { it.vacante.id }) { item ->
                    VacancyCard(
                        vacante = item.vacante,
                        diasRestantes = item.diasRestantes,
                        onClick = { onAbrirVacante(item.vacante.id) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun VacantesContenidoPreview() {
    val hoy = LocalDate.now()
    val items = MockDatos.vacantes(hoy).mapIndexed { i, v ->
        VacanteItemUi(v.resumen.copy(probabilidad = 80 - i * 15), ReglasCuentaRegresiva.diasHastaCierre(hoy, v.resumen.fechaCierre))
    }
    ChambaReadyTheme {
        VacantesContenido(
            estado = VacantesUiState(cargando = false, todas = items, filtros = setOf(FiltroVacantes.HIBRIDA)),
            onAbrirVacante = {}, onBuscar = {}, onFiltro = {}, onReintentar = {},
        )
    }
}
