package pe.edu.esan.chambaready.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.ColoresChamba
import pe.edu.esan.chambaready.core.domain.reglas.ZonaProbabilidad
import kotlin.math.roundToInt

/** Marca adicional sobre la escala, por ejemplo "Antes" o "Meta" en P16. */
data class PuntoEscala(val porcentaje: Int, val etiqueta: String)

/**
 * Escala de probabilidad de tres zonas (Baja 0–40, Media 40–70, Alta 70–100) con el marcador
 * "Tú estás aquí · X %". Es el elemento clave de P09; P10 y P16 la reutilizan con [puntosExtra].
 * Para TalkBack se lee como una sola frase con el porcentaje y la zona.
 */
@Composable
fun ProbabilityScale(
    porcentaje: Int,
    modifier: Modifier = Modifier,
    etiqueta: String = "Tú estás aquí",
    puntosExtra: List<PuntoEscala> = emptyList(),
) {
    val colores = ChambaTema.colores
    val valor = porcentaje.coerceIn(0, 100)
    val zona = ZonaProbabilidad.de(valor)
    val descripcion = buildString {
        append("$etiqueta: $valor por ciento, probabilidad ${zona.etiqueta.lowercase()}.")
        puntosExtra.forEach { append(" ${it.etiqueta}: ${it.porcentaje.coerceIn(0, 100)} por ciento.") }
    }

    Column(modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descripcion }) {
        EnPosicion(fraccion = valor / 100f) {
            Surface(
                color = colores.marcadorEscala,
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "$etiqueta · $valor %",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White, // Siempre legible sobre el marcador oscuro.
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(22.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape),
            ) {
                ZonaProbabilidad.entries.forEach { z ->
                    Box(
                        Modifier
                            .weight(z.ancho.toFloat())
                            .fillMaxHeight()
                            .background(colorDeZona(z, colores)),
                    )
                }
            }
            puntosExtra.forEach { punto ->
                EnPosicion(fraccion = punto.porcentaje.coerceIn(0, 100) / 100f, modifier = Modifier.align(Alignment.Center)) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, colores.marcadorEscala, CircleShape),
                    )
                }
            }
            EnPosicion(fraccion = valor / 100f, modifier = Modifier.align(Alignment.Center)) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(22.dp)
                        .background(colores.marcadorEscala, RoundedCornerShape(2.dp)),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            ZonaProbabilidad.entries.forEach { z ->
                Text(
                    text = z.etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = colores.textoSecundario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(z.ancho.toFloat()),
                )
            }
        }
        if (puntosExtra.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                puntosExtra.forEach { punto ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                                .border(2.dp, colores.marcadorEscala, CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${punto.etiqueta} · ${punto.porcentaje.coerceIn(0, 100)} %",
                            style = MaterialTheme.typography.labelSmall,
                            color = colores.textoSecundario,
                        )
                    }
                }
            }
        }
    }
}

/** Color de fondo de cada zona de la escala. También lo usan las barras de [VacancyCard]. */
fun colorDeZona(zona: ZonaProbabilidad, colores: ColoresChamba): Color = when (zona) {
    ZonaProbabilidad.BAJA -> colores.zonaBaja
    ZonaProbabilidad.MEDIA -> colores.zonaMedia
    ZonaProbabilidad.ALTA -> colores.zonaAlta
}

/**
 * Coloca a su único hijo centrado en [fraccion] del ancho disponible, sin salirse por los bordes.
 * Así la etiqueta del marcador no se corta cuando la probabilidad es 2 % o 98 %.
 */
@Composable
private fun EnPosicion(
    fraccion: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { medibles, restricciones ->
        val hijo = medibles.first().measure(restricciones.copy(minWidth = 0, minHeight = 0))
        val ancho = restricciones.maxWidth
        val x = (ancho * fraccion - hijo.width / 2f)
            .roundToInt()
            .coerceIn(0, (ancho - hijo.width).coerceAtLeast(0))
        layout(ancho, hijo.height) { hijo.place(x, 0) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun ProbabilityScalePreview() {
    ChambaReadyTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ProbabilityScale(porcentaje = 58)
            ProbabilityScale(porcentaje = 3)
            ProbabilityScale(porcentaje = 97)
            ProbabilityScale(
                porcentaje = 52,
                etiqueta = "Ahora",
                puntosExtra = listOf(PuntoEscala(31, "Antes"), PuntoEscala(78, "Meta")),
            )
        }
    }
}
