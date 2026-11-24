package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad

/**
 * Grupo de chips que salta de línea cuando no entra. Sirve para selección única (niveles en P02)
 * y múltiple (filtros en P08): quien lo usa decide qué hacer en [onClick] y qué va en
 * [seleccionadas]. Cada chip ya cumple el objetivo táctil de 48 dp de Material 3.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipSelector(
    opciones: List<T>,
    seleccionadas: Set<T>,
    onClick: (T) -> Unit,
    etiqueta: (T) -> String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico),
    ) {
        opciones.forEach { opcion ->
            val seleccionada = opcion in seleccionadas
            FilterChip(
                selected = seleccionada,
                onClick = { onClick(opcion) },
                label = { Text(etiqueta(opcion), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (seleccionada) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = seleccionada,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F8F7)
@Composable
private fun ChipSelectorPreview() {
    ChambaReadyTheme {
        var nivel by remember { mutableStateOf(NivelHabilidad.INTERMEDIO) }
        var filtros by remember { mutableStateOf(setOf("Remoto")) }
        Column(Modifier.padding(20.dp)) {
            Text("¿Qué nivel tienes en SQL?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            ChipSelector(
                opciones = NivelHabilidad.entries,
                seleccionadas = setOf(nivel),
                onClick = { nivel = it },
                etiqueta = { it.etiqueta },
            )
            Spacer(Modifier.size(16.dp))
            ChipSelector(
                opciones = listOf("Remoto", "Híbrido", "Lima", "Con sueldo", "Cierra pronto"),
                seleccionadas = filtros,
                onClick = { f -> filtros = if (f in filtros) filtros - f else filtros + f },
                etiqueta = { it },
            )
        }
    }
}
