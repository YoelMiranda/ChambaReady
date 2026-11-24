package pe.edu.esan.chambaready.ui.bienvenida

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.BuildConfig
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject


/** Carga el perfil ficticio de prueba (botón visible en builds de depuración) y dice si hay login. */
@HiltViewModel
class BienvenidaViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val reloj: Clock,
    sesion: SesionRepository,
) : ViewModel() {
    /** false en MODO DEMO: no se muestra "Ya tengo cuenta". */
    val loginDisponible: Boolean = sesion.disponible

    fun cargarDatosDePrueba(alTerminar: () -> Unit) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { MockDatos.estadoDemo(LocalDate.now(reloj)) }
            alTerminar()
        }
    }
}

/** C01 Bienvenida. "Soy empresa" no entra a la app: muestra el link a la web de empresas. */
@Composable
fun BienvenidaScreen(
    onSoyPracticante: () -> Unit,
    onIniciarSesion: () -> Unit,
    onEntrarConDatosDePrueba: () -> Unit,
    onVerCatalogo: () -> Unit,
    viewModel: BienvenidaViewModel = hiltViewModel(),
) {
    val abridorDeLinks = LocalUriHandler.current
    BienvenidaContenido(
        onSoyPracticante = onSoyPracticante,
        onIniciarSesion = if (viewModel.loginDisponible) onIniciarSesion else null,
        onAbrirWebEmpresas = { abridorDeLinks.openUri(BuildConfig.WEB_EMPRESA_URL) },
        mostrarOpcionesDeEquipo = BuildConfig.DEBUG,
        onDatosDePrueba = { viewModel.cargarDatosDePrueba(onEntrarConDatosDePrueba) },
        onVerCatalogo = onVerCatalogo,
    )
}

@Composable
fun BienvenidaContenido(
    onSoyPracticante: () -> Unit,
    /** null = no hay login (modo demo) y no se muestra el botón. */
    onIniciarSesion: (() -> Unit)?,
    onAbrirWebEmpresas: () -> Unit,
    mostrarOpcionesDeEquipo: Boolean,
    onDatosDePrueba: () -> Unit,
    onVerCatalogo: () -> Unit,
) {
    var mostrarLinkEmpresa by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensiones.paddingLateral, vertical = Dimensiones.espacioSeccion),
            verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioGrande),
        ) {
            Text(
                buildAnnotatedString {
                    append("Chamba")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Ready") }
                },
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Tu primera práctica, con un plan que cabe en tu tiempo libre.",
                style = MaterialTheme.typography.bodyLarge,
                color = ChambaTema.colores.textoSecundario,
            )
            listOf(
                "Arma tu perfil" to "Sube tu CV y la IA completa lo que falta.",
                "Mira tu brecha" to "Qué te falta para cada vacante y cuánto tiempo toma.",
                "Sigue tu plan" to "Cursos externos repartidos en tus horas libres.",
            ).forEachIndexed { i, (titulo, detalle) -> Paso(numero = i + 1, titulo = titulo, detalle = detalle) }

            Spacer(Modifier.height(Dimensiones.espacioChico))
            OpcionRol(
                icono = Icons.Outlined.School,
                titulo = "Soy practicante",
                detalle = "Busco mi primera práctica preprofesional.",
                onClick = onSoyPracticante,
            )
            if (onIniciarSesion != null) {
                TextButton(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth()) {
                    Text("¿Ya tienes cuenta? Inicia sesión")
                }
            }
            OpcionRol(
                icono = Icons.Outlined.Business,
                titulo = "Soy empresa",
                detalle = "Publico vacantes y evalúo postulantes.",
                onClick = { mostrarLinkEmpresa = true },
            )
            if (mostrarLinkEmpresa) {
                TarjetaSeccion(titulo = "ChambaReady para empresas") {
                    Text(
                        "Las empresas usan la versión web, pensada para la computadora. Ahí publicas vacantes y ves a tus candidatos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton(
                        "Abrir la web de empresas",
                        onClick = onAbrirWebEmpresas,
                        icono = Icons.AutoMirrored.Outlined.OpenInNew,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (mostrarOpcionesDeEquipo) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDatosDePrueba) { Text("Explorar con datos de prueba") }
                    TextButton(onClick = onVerCatalogo) { Text("Componentes") }
                }
            }
        }
    }
}

@Composable
private fun Paso(numero: Int, titulo: String, detalle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
            Text(
                "$numero",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.width(Dimensiones.espacioMedio))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
        }
    }
}

@Composable
private fun OpcionRol(icono: ImageVector, titulo: String, detalle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Dimensiones.bordeTarjeta, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(Dimensiones.espacioGrande), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(Dimensiones.espacioMedio))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun BienvenidaPreview() {
    ChambaReadyTheme {
        BienvenidaContenido(onSoyPracticante = {}, onIniciarSesion = {}, onAbrirWebEmpresas = {}, mostrarOpcionesDeEquipo = true, onDatosDePrueba = {}, onVerCatalogo = {})
    }
}
