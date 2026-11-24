package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.TextoPrincipal
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello

/**
 * Sello de un requisito: Confirmado (hay evidencia o mini-test), Declarado (lo afirma el
 * practicante) o No cumple. Siempre lleva texto e ícono, nunca solo color.
 *
 * Contraste: sobre el gris azulado de "Declarado" (#6C8CA3) el texto blanco solo llega a 3.5:1,
 * por eso ese sello usa texto oscuro (#14212B, 4.6:1).
 */
@Composable
fun SealBadge(tipo: TipoSello, modifier: Modifier = Modifier) {
    val colores = ChambaTema.colores
    val (fondo, texto, icono) = when (tipo) {
        TipoSello.CONFIRMADO -> Triple(colores.selloConfirmado, Color.White, Icons.Outlined.CheckCircle)
        TipoSello.DECLARADO -> Triple(colores.selloDeclarado, TextoPrincipal, Icons.Outlined.Info)
        TipoSello.NO_CUMPLE -> Triple(colores.selloNoCumple, TextoPrincipal, Icons.Outlined.RemoveCircleOutline)
    }
    ContenidoSello(fondo, texto, icono, tipo.etiqueta, modifier)
}

@Composable
private fun ContenidoSello(fondo: Color, texto: Color, icono: ImageVector, etiqueta: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = fondo, contentColor = texto) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(etiqueta, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SealBadgePreview() {
    ChambaReadyTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoSello.entries.forEach { SealBadge(it) }
        }
    }
}
