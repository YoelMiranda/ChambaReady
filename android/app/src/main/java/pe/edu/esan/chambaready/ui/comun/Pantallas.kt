package pe.edu.esan.chambaready.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Estructura común de las pantallas: barra superior con flecha "atrás" (que siempre hace
 * popBackStack, ver la navegación), contenido y, si se pasa, una barra de botones fija abajo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {},
    barraInferior: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    contenido: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = acciones,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = barraInferior,
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        content = contenido,
    )
}

/** Contenido con scroll dentro de la pantalla (las pantallas largas: P09, P10, P12…). */
@Composable
fun ContenidoDesplazable(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimensiones.paddingLateral, vertical = Dimensiones.espacioGrande),
        verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioGrande),
        content = contenido,
    )
}

/** Botones de acción fijos al pie de la pantalla. */
@Composable
fun BarraAcciones(contenido: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimensiones.paddingLateral, vertical = Dimensiones.espacioMedio),
                verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico),
                content = contenido,
            )
        }
    }
}

/** Tarjeta con borde de 1 dp y radio de 16 dp, con título opcional. */
@Composable
fun TarjetaSeccion(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimensiones.bordeTarjeta, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(Dimensiones.espacioGrande),
            verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico),
        ) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

/** Bloque de contenido Premium bloqueado (P10, P12, P14, P16). */
@Composable
fun BloqueoPremium(
    titulo: String,
    descripcion: String,
    onDesbloquear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TarjetaSeccion(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Dimensiones.espacioChico))
            Text(titulo, style = MaterialTheme.typography.titleMedium)
        }
        Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
        PrimaryButton("Desbloquear con Premium", onClick = onDesbloquear, modifier = Modifier.fillMaxWidth())
    }
}

/** Etiqueta pequeña tipo píldora (por ejemplo "VIABLE" o "Filtro"). Siempre con texto. */
@Composable
fun Etiqueta(texto: String, fondo: Color, colorTexto: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = fondo, contentColor = colorTexto) {
        Text(texto, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Punto de color para marcar notificaciones sin leer (acompañado siempre de texto accesible). */
@Composable
fun PuntoNoLeido(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(10.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}
}

@Composable
fun CargandoPantalla(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// ---------- Formatos ----------

private val localePeru: Locale = Locale.forLanguageTag("es-PE")
private val formatoFechaCorta = DateTimeFormatter.ofPattern("d 'de' MMMM", localePeru)

/** "S/ 9.99" a partir de céntimos. */
fun formatoSoles(centimos: Long): String = String.format(Locale.ROOT, "S/ %d.%02d", centimos / 100, centimos % 100)

fun formatoFecha(fecha: LocalDate): String = fecha.format(formatoFechaCorta)
