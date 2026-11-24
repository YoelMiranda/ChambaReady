package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema

/** Progreso por pasos ("Paso 1 de 6") del registro (C02, P01–P06) y del asistente E03. */
@Composable
fun StepProgress(
    pasoActual: Int,
    totalPasos: Int,
    modifier: Modifier = Modifier,
    mostrarTexto: Boolean = true,
) {
    val colores = ChambaTema.colores
    val paso = pasoActual.coerceIn(0, totalPasos)
    Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Paso $paso de $totalPasos" }) {
        if (mostrarTexto) {
            Text("Paso $paso de $totalPasos", style = MaterialTheme.typography.labelMedium, color = colores.textoSecundario)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(totalPasos) { indice ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (indice < paso) MaterialTheme.colorScheme.primary else colores.borde,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F8F7, widthDp = 360)
@Composable
private fun StepProgressPreview() {
    ChambaReadyTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepProgress(pasoActual = 1, totalPasos = 6)
            StepProgress(pasoActual = 4, totalPasos = 6)
            StepProgress(pasoActual = 2, totalPasos = 3, mostrarTexto = false)
        }
    }
}
