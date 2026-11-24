package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val LocalePeru: Locale = Locale.forLanguageTag("es-PE")

/** "1.5 h" o "2 h". */
fun formatoHoras(horas: Double): String =
    if (horas % 1.0 == 0.0) "${horas.toInt()} h" else "$horas h"

/**
 * Fila de un día con botones − y + (pasos de 0.5 h, de 0 a 8 h). Los botones miden 48 dp y
 * tienen descripción para TalkBack ("Quitar media hora el lunes"). Se usa en P04 y P22.
 */
@Composable
fun DayHoursStepper(
    dia: DayOfWeek,
    horas: Double,
    onRestar: () -> Unit,
    onSumar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nombreDia = dia.getDisplayName(TextStyle.FULL, LocalePeru).replaceFirstChar { it.uppercase(LocalePeru) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(nombreDia, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedIconButton(
            onClick = onRestar,
            enabled = horas > 0.0,
            modifier = Modifier.size(Dimensiones.tactilMinimo),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Icon(Icons.Outlined.Remove, contentDescription = "Quitar media hora el $nombreDia")
        }
        Text(
            text = formatoHoras(horas),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        OutlinedIconButton(
            onClick = onSumar,
            enabled = horas < HorasLibres.HORAS_MAXIMAS_POR_DIA,
            modifier = Modifier.size(Dimensiones.tactilMinimo),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Sumar media hora el $nombreDia")
        }
    }
}

/** Editor completo de la semana (P04 y P22): un [DayHoursStepper] por día y el total en vivo. */
@Composable
fun WeekHoursEditor(
    horasLibres: HorasLibres,
    onCambio: (HorasLibres) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimensiones.bordeTarjeta, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = Dimensiones.espacioGrande, vertical = Dimensiones.espacioChico)) {
            DayOfWeek.entries.forEach { dia ->
                DayHoursStepper(
                    dia = dia,
                    horas = horasLibres.horas(dia),
                    onRestar = { onCambio(horasLibres.restar(dia)) },
                    onSumar = { onCambio(horasLibres.sumar(dia)) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = Dimensiones.espacioChico))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensiones.espacioChico)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total semanal", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatoHoras(horasLibres.totalSemanal),
                    style = MaterialTheme.typography.titleMedium,
                    color = ChambaTema.colores.exitoTexto,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F8F7, widthDp = 360)
@Composable
private fun WeekHoursEditorPreview() {
    ChambaReadyTheme {
        var horas by remember {
            mutableStateOf(HorasLibres(mapOf(DayOfWeek.MONDAY to 2.0, DayOfWeek.SATURDAY to 4.5, DayOfWeek.SUNDAY to 8.0)))
        }
        WeekHoursEditor(horasLibres = horas, onCambio = { horas = it }, modifier = Modifier.padding(20.dp))
    }
}
