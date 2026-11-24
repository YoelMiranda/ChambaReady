package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.reglas.ReglasCuentaRegresiva
import pe.edu.esan.chambaready.core.domain.reglas.UrgenciaCierre

/**
 * Cuenta regresiva hasta el cierre de una vacante (regla 2): rojo si faltan 3 días o menos,
 * ámbar entre 4 y 7, y verde si faltan más de 7. El texto siempre acompaña al color.
 */
@Composable
fun CountdownChip(diasRestantes: Int, modifier: Modifier = Modifier) {
    val colores = ChambaTema.colores
    val (fondo, texto) = when (ReglasCuentaRegresiva.urgencia(diasRestantes)) {
        UrgenciaCierre.ROJA -> colores.avisoRojoFondo to colores.avisoRojoTexto
        UrgenciaCierre.AMBAR -> colores.avisoAmbarFondo to colores.avisoAmbarTexto
        UrgenciaCierre.VERDE -> colores.exitoFondo to colores.exitoTexto
        UrgenciaCierre.CERRADA -> MaterialTheme.colorScheme.surfaceVariant to colores.textoSecundario
    }
    Surface(modifier = modifier, shape = CircleShape, color = fondo, contentColor = texto) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(ReglasCuentaRegresiva.texto(diasRestantes), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CountdownChipPreview() {
    ChambaReadyTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CountdownChip(diasRestantes = 0)
            CountdownChip(diasRestantes = 2)
            CountdownChip(diasRestantes = 6)
            CountdownChip(diasRestantes = 15)
            CountdownChip(diasRestantes = -1)
        }
    }
}
