package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad

private const val SEGMENTOS = 3

/**
 * Barra de nivel de una habilidad en tres segmentos (Básico, Intermedio, Avanzado). Si se pasa
 * [nivelRequerido], el segmento exigido que aún no se alcanza aparece con borde. Se usa en P06
 * (declarado vs. demostrado), P09 y P16.
 */
@Composable
fun SkillLevelBar(
    nombre: String,
    nivel: NivelHabilidad,
    modifier: Modifier = Modifier,
    nivelRequerido: NivelHabilidad? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val colores = ChambaTema.colores
    val detalle = nivelRequerido?.let { " · se requiere ${it.etiqueta}" }.orEmpty()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$nombre: nivel ${nivel.etiqueta}$detalle" },
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(nivel.etiqueta + detalle, style = MaterialTheme.typography.labelSmall, color = colores.textoSecundario)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(SEGMENTOS) { indice ->
                val alcanzado = indice < nivel.valor
                val exigidoPendiente = !alcanzado && nivelRequerido != null && indice < nivelRequerido.valor
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(if (alcanzado) color else colores.borde, CircleShape)
                        .then(
                            if (exigidoPendiente) Modifier.border(1.5.dp, color, CircleShape) else Modifier,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun SkillLevelBarPreview() {
    ChambaReadyTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SkillLevelBar("SQL", NivelHabilidad.INTERMEDIO, nivelRequerido = NivelHabilidad.AVANZADO)
            SkillLevelBar("Excel", NivelHabilidad.AVANZADO)
            SkillLevelBar("Power BI", NivelHabilidad.NO_LO_SE, nivelRequerido = NivelHabilidad.BASICO)
            SkillLevelBar("Python (demostrado)", NivelHabilidad.BASICO, color = ChambaTema.colores.selloDeclarado)
        }
    }
}
