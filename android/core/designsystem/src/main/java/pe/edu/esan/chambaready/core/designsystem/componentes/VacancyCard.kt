package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import pe.edu.esan.chambaready.core.domain.reglas.ZonaProbabilidad
import java.time.LocalDate

/**
 * Tarjeta de vacante para P07 y P08: puesto, empresa, modalidad, sueldo, cuenta regresiva y la
 * probabilidad del usuario (barra + %). Toda la tarjeta es un solo objetivo táctil.
 */
@Composable
fun VacancyCard(
    vacante: VacanteResumen,
    diasRestantes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = ChambaTema.colores
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Dimensiones.bordeTarjeta, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(Dimensiones.espacioGrande)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoIniciales(nombre = vacante.empresa)
                Spacer(Modifier.width(Dimensiones.espacioMedio))
                Column(Modifier.weight(1f)) {
                    Text(
                        vacante.puesto,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(vacante.empresa, style = MaterialTheme.typography.bodyMedium, color = colores.textoSecundario)
                }
            }
            Spacer(Modifier.height(Dimensiones.espacioMedio))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = listOfNotNull(vacante.modalidad.etiqueta, vacante.sueldo).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.textoSecundario,
                    modifier = Modifier.weight(1f),
                )
                CountdownChip(diasRestantes = diasRestantes)
            }
            Spacer(Modifier.height(Dimensiones.espacioMedio))
            BarraProbabilidad(porcentaje = vacante.probabilidad)
        }
    }
}

/** Barra horizontal con el color de la zona y el porcentaje a la derecha. */
@Composable
fun BarraProbabilidad(porcentaje: Int, modifier: Modifier = Modifier) {
    val colores = ChambaTema.colores
    val valor = porcentaje.coerceIn(0, 100)
    val zona = ZonaProbabilidad.de(valor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Tu probabilidad: $valor por ciento, ${zona.etiqueta.lowercase()}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(valor / 100f)
                    .fillMaxHeight()
                    .background(colorDeZona(zona, colores), CircleShape),
            )
        }
        Spacer(Modifier.width(Dimensiones.espacioMedio))
        Text("$valor %", style = MaterialTheme.typography.labelMedium)
    }
}

/** Círculo con las iniciales de la empresa. En F7 se reemplaza por el logo (Coil) si existe. */
@Composable
private fun LogoIniciales(nombre: String) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F8F7, widthDp = 360)
@Composable
private fun VacancyCardPreview() {
    ChambaReadyTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VacancyCard(
                vacante = VacanteResumen("1", "Practicante de Analítica de Datos", "Banco Andino", Modalidad.HIBRIDA, "S/ 1,300", LocalDate.now().plusDays(2), 74),
                diasRestantes = 2,
                onClick = {},
            )
            VacancyCard(
                vacante = VacanteResumen("2", "Practicante de Marketing Digital", "Retail Sur", Modalidad.REMOTA, null, LocalDate.now().plusDays(12), 35),
                diasRestantes = 12,
                onClick = {},
            )
        }
    }
}
