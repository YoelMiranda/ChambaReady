package pe.edu.esan.chambaready.ui.registro

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.CvSesion
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.reglas.ReglasRegistro
import pe.edu.esan.chambaready.core.domain.reglas.ResultadoCv
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import javax.inject.Inject

data class CargarPerfilUiState(
    val archivo: String? = null,
    val error: String? = null,
    val linkedin: String = "",
    /** Subiendo y analizando el CV (F6/F7). */
    val analizando: Boolean = false,
    /** Lo que encontró la IA, para mostrarlo antes de seguir. */
    val resumenCv: String? = null,
)

/**
 * P01. Revisa el CV con [ReglasRegistro.validarCv] (PDF o Word, máx. 5 MB).
 * Con backend: lo sube (F7, guardado en la base) y la IA del servidor lo lee (F6). Lo encontrado se
 * usa en P02 ([CvSesion]). En modo demo solo se guarda el nombre.
 */
@HiltViewModel
class CargarPerfilViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val cuenta: CuentaRepository,
    private val cvSesion: CvSesion,
) : BaseViewModel<CargarPerfilUiState, Nothing>(CargarPerfilUiState()) {

    /** @param leer lee el contenido del archivo (se llama en un hilo de entrada/salida). */
    fun onArchivoElegido(nombre: String, tipoMime: String?, bytes: Long, leer: () -> ByteArray) {
        val resultado = ReglasRegistro.validarCv(tipoMime, bytes)
        if (resultado != ResultadoCv.VALIDO) {
            setState { copy(archivo = null, error = resultado.mensaje, resumenCv = null) }
            return
        }
        setState { copy(archivo = nombre, error = null, resumenCv = null, analizando = true) }
        cvSesion.analisis = null
        viewModelScope.launch {
            try {
                val contenido = withContext(Dispatchers.IO) { leer() }
                val analisis = cuenta.analizarCv(nombre, tipoMime ?: "application/pdf", contenido)
                cvSesion.analisis = analisis
                val resumen = analisis?.let {
                    when {
                        it.habilidades.isEmpty() && !it.conIa -> "CV guardado. La IA no está activa en el servidor: completarás tus habilidades en el siguiente paso."
                        it.habilidades.isEmpty() -> "CV guardado. No encontramos habilidades del catálogo: te preguntaremos en el siguiente paso."
                        else -> "Encontramos ${it.habilidades.size} habilidades en tu CV. Las confirmas en el siguiente paso."
                    }
                }
                setState { copy(resumenCv = resumen) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorRemoto) {
                setState { copy(archivo = null, error = e.message) }
            } catch (e: java.io.IOException) {
                setState { copy(archivo = null, error = "No pudimos leer el archivo. Elige otro.") }
            } finally {
                setState { copy(analizando = false) }
            }
        }
    }

    fun onLinkedin(valor: String) = setState { copy(linkedin = valor) }

    fun continuar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            estadoRepositorio.actualizar { it.copy(cvNombre = currentState.archivo) }
            alTerminar()
        }
    }
}

@Composable
fun CargarPerfilScreen(
    onBack: () -> Unit,
    onContinuar: () -> Unit,
    viewModel: CargarPerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            var nombre = "CV"
            var bytes = 0L
            contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val iNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val iTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (iNombre >= 0) nombre = cursor.getString(iNombre)
                    if (iTamano >= 0) bytes = cursor.getLong(iTamano)
                }
            }
            viewModel.onArchivoElegido(nombre, contexto.contentResolver.getType(uri), bytes) {
                contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            }
        }
    }
    CargarPerfilContenido(
        estado = estado,
        onBack = onBack,
        onElegirArchivo = {
            selector.launch(
                arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                ),
            )
        },
        onLinkedin = viewModel::onLinkedin,
        onContinuar = { viewModel.continuar(onContinuar) },
    )
}

@Composable
fun CargarPerfilContenido(
    estado: CargarPerfilUiState,
    onBack: () -> Unit,
    onElegirArchivo: () -> Unit,
    onLinkedin: (String) -> Unit,
    onContinuar: () -> Unit,
) {
    PantallaBase(
        titulo = "Carga tu perfil",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(
                    if (estado.archivo != null || estado.linkedin.isNotBlank()) "Continuar" else "Empezar desde cero",
                    onClick = onContinuar,
                    enabled = !estado.analizando,
                    cargando = estado.analizando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            StepProgress(pasoActual = 2, totalPasos = PASOS_REGISTRO)
            Text(
                "Con tu CV la IA completa casi todo por ti. Solo te preguntará lo que falte.",
                style = MaterialTheme.typography.bodyLarge,
                color = ChambaTema.colores.textoSecundario,
            )
            TarjetaSeccion(titulo = "Sube tu CV (recomendado)") {
                Text("PDF o Word, máximo 5 MB.", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                if (estado.archivo != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Dimensiones.espacioChico))
                        Text(estado.archivo, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (estado.analizando) {
                    Text("Leyendo tu CV…", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                }
                estado.resumenCv?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (estado.error != null) {
                    Text(estado.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                SecondaryButton(
                    if (estado.archivo == null) "Elegir archivo" else "Cambiar archivo",
                    onClick = onElegirArchivo,
                    icono = Icons.Outlined.UploadFile,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TarjetaSeccion(titulo = "O pega tu LinkedIn") {
                Campo("Link de tu perfil", estado.linkedin, onLinkedin, error = null)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = ChambaTema.colores.textoSecundario)
                    Spacer(Modifier.width(Dimensiones.espacioChico))
                    Text("Solo leemos tu perfil público.", style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
                }
            }
            TextButton(onClick = onContinuar) { Text("No tengo CV: empezar desde cero") }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CargarPerfilPreview() {
    ChambaReadyTheme {
        CargarPerfilContenido(CargarPerfilUiState(archivo = "CV_Ana.pdf"), onBack = {}, onElegirArchivo = {}, onLinkedin = {}, onContinuar = {})
    }
}
