package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones

private val PaddingBoton = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/**
 * Botón principal: 54 dp de alto y radio de 14 dp. Toma el color primario del rol activo.
 * Mientras [cargando] es true muestra un indicador y no se puede pulsar.
 */
@Composable
fun PrimaryButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null,
    cargando: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimensiones.alturaBoton),
        enabled = enabled && !cargando,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingBoton,
    ) {
        ContenidoBoton(texto = texto, icono = icono, cargando = cargando)
    }
}

/** Botón secundario: mismo tamaño que [PrimaryButton], con borde de 1 dp y fondo transparente. */
@Composable
fun SecondaryButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimensiones.alturaBoton),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingBoton,
    ) {
        ContenidoBoton(texto = texto, icono = icono, cargando = false)
    }
}

@Composable
private fun ContenidoBoton(texto: String, icono: ImageVector?, cargando: Boolean) {
    if (cargando) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
        Spacer(Modifier.width(Dimensiones.espacioChico))
    } else if (icono != null) {
        // El texto del botón ya describe la acción: el ícono es decorativo.
        Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Dimensiones.espacioChico))
    }
    Text(text = texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F8F7)
@Composable
private fun BotonesPreview() {
    ChambaReadyTheme {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PrimaryButton("Mejorar mis probabilidades", onClick = {}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(12.dp))
            PrimaryButton("Enviando…", onClick = {}, cargando = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(12.dp))
            SecondaryButton("Postular sin preparación", onClick = {}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(12.dp))
            PrimaryButton("Deshabilitado", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}

