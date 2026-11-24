package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme

/** Un ítem de la barra inferior. */
data class ItemBarraInferior(val etiqueta: String, val icono: ImageVector)

/**
 * Barra inferior de Material 3 con el indicador tipo píldora en el ítem activo (usa el
 * `secondaryContainer` del rol). El ícono es decorativo porque la etiqueta ya lo nombra.
 */
@Composable
fun BottomNavBar(
    items: List<ItemBarraInferior>,
    indiceSeleccionado: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            items.forEachIndexed { indice, item ->
                NavigationBarItem(
                    selected = indice == indiceSeleccionado,
                    onClick = { onSeleccion(indice) },
                    icon = { Icon(item.icono, contentDescription = null) },
                    label = {
                        Text(
                            item.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/** Los 5 ítems del practicante, en el orden de los mockups. */
object ItemsBarraInferior {
    val practicante = listOf(
        ItemBarraInferior("Inicio", Icons.Outlined.Home),
        ItemBarraInferior("Vacantes", Icons.Outlined.WorkOutline),
        ItemBarraInferior("Pendientes", Icons.Outlined.TaskAlt),
        ItemBarraInferior("Postulaciones", Icons.AutoMirrored.Outlined.Assignment),
        ItemBarraInferior("Perfil", Icons.Outlined.Person),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BottomNavBarPracticantePreview() {
    ChambaReadyTheme {
        BottomNavBar(items = ItemsBarraInferior.practicante, indiceSeleccionado = 1, onSeleccion = {})
    }
}

