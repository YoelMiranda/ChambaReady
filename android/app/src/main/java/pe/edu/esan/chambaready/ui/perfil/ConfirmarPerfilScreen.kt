package pe.edu.esan.chambaready.ui.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.Habilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoHabilidad
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import pe.edu.esan.chambaready.ui.registro.PASOS_REGISTRO
import javax.inject.Inject

data class FilaHabilidad(
    val habilidad: Habilidad,
    val nivel: NivelHabilidad,
    val conEvidencia: Boolean,
)

data class ConfirmarPerfilUiState(
    val nombre: String = "",
    val detalleAcademico: String = "",
    val habilidades: List<FilaHabilidad> = emptyList(),
    val proyectos: List<String> = emptyList(),
    val faltantes: List<String> = emptyList(),
    /** Cambios de nivel hechos en modo edición, aún sin guardar. */
    val cambios: Map<String, NivelHabilidad> = emptyMap(),
) {
    fun nivel(fila: FilaHabilidad): NivelHabilidad = cambios[fila.habilidad.id] ?: fila.nivel
    val tecnicas get() = habilidades.filter { it.habilidad.tipo == TipoHabilidad.TECNICA }
    val blandas get() = habilidades.filter { it.habilidad.tipo == TipoHabilidad.BLANDA }
}

/** P03 "¿Este eres tú?". En modo edición (desde P19, Premium) permite cambiar los niveles declarados. */
@HiltViewModel
class ConfirmarPerfilViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val contenido: ContenidoRepository,
) : BaseViewModel<ConfirmarPerfilUiState, Nothing>(ConfirmarPerfilUiState()) {

    init {
        viewModelScope.launch {
            estadoRepositorio.estado.collect { estado ->
                val ids = (contenido.habilidadesDetectadasEnCv().map { it.id } + estado.niveles.declarados.keys).distinct()
                val filas = ids.mapNotNull { id ->
                    contenido.habilidad(id)?.let { h ->
                        val nivel = estado.niveles.declarado(id)
                        FilaHabilidad(h, nivel, estado.tieneEvidencia(id, if (nivel.valor > 0) nivel else NivelHabilidad.BASICO))
                    }
                }
                val registro = estado.registro
                setState {
                    copy(
                        nombre = registro?.nombre ?: "Tu perfil",
                        detalleAcademico = registro?.let { "${it.carrera} · ciclo ${it.ciclo} · ${it.universidad}" } ?: "",
                        habilidades = filas,
                        proyectos = if (estado.cvNombre != null) contenido.proyectosDetectadosEnCv() else emptyList(),
                        faltantes = buildList {
                            if (estado.cvNombre == null) add("No subiste CV: agrega proyectos más adelante desde tu perfil.")
                            filas.filter { it.nivel == NivelHabilidad.NO_LO_SE }.forEach { add("Confirma tu nivel en ${it.habilidad.nombre}.") }
                            if (filas.none { it.conEvidencia }) add("Aún no tienes evidencias. Súbelas en «Mis evidencias» para confirmar tus niveles.")
                        },
                    )
                }
            }
        }
    }

    fun cambiarNivel(habilidadId: String, nivel: NivelHabilidad) = setState { copy(cambios = cambios + (habilidadId to nivel)) }

    fun guardar(alTerminar: () -> Unit) {
        val cambios = currentState.cambios
        viewModelScope.launch {
            estadoRepositorio.actualizar { it.copy(niveles = it.niveles.copy(declarados = it.niveles.declarados + cambios)) }
            setState { copy(cambios = emptyMap()) }
            alTerminar()
        }
    }
}

@Composable
fun ConfirmarPerfilScreen(
    modoEdicion: Boolean,
    onBack: () -> Unit,
    onConfirmar: () -> Unit,
    onCorregir: () -> Unit,
    viewModel: ConfirmarPerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    ConfirmarPerfilContenido(
        estado = estado,
        modoEdicion = modoEdicion,
        onBack = onBack,
        onCambiarNivel = viewModel::cambiarNivel,
        onConfirmar = { viewModel.guardar(onConfirmar) },
        onCorregir = onCorregir,
    )
}

@Composable
fun ConfirmarPerfilContenido(
    estado: ConfirmarPerfilUiState,
    modoEdicion: Boolean,
    onBack: () -> Unit,
    onCambiarNivel: (String, NivelHabilidad) -> Unit,
    onConfirmar: () -> Unit,
    onCorregir: () -> Unit,
) {
    PantallaBase(
        titulo = if (modoEdicion) "Editar datos y habilidades" else "¿Este eres tú?",
        onBack = onBack,
        barraInferior = {
            BarraAcciones {
                if (modoEdicion) {
                    PrimaryButton("Guardar cambios", onClick = onConfirmar, modifier = Modifier.fillMaxWidth())
                } else {
                    PrimaryButton("Sí, este soy yo", onClick = onConfirmar, modifier = Modifier.fillMaxWidth())
                    SecondaryButton("Corregir algo", onClick = onCorregir, modifier = Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            if (!modoEdicion) StepProgress(pasoActual = 4, totalPasos = PASOS_REGISTRO)
            Column {
                Text(estado.nombre, style = MaterialTheme.typography.headlineSmall)
                if (estado.detalleAcademico.isNotBlank()) {
                    Text(estado.detalleAcademico, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                }
            }
            SeccionHabilidades("Habilidades técnicas", estado.tecnicas, estado, modoEdicion, onCambiarNivel)
            SeccionHabilidades("Habilidades blandas", estado.blandas, estado, modoEdicion, onCambiarNivel)
            if (estado.proyectos.isNotEmpty()) {
                TarjetaSeccion(titulo = "Proyectos") {
                    estado.proyectos.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (estado.faltantes.isNotEmpty()) {
                TarjetaSeccion(titulo = "Te falta completar") {
                    estado.faltantes.forEach { texto ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = ChambaTema.colores.avisoAmbarTexto)
                            Spacer(Modifier.width(Dimensiones.espacioChico))
                            Text(texto, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionHabilidades(
    titulo: String,
    filas: List<FilaHabilidad>,
    estado: ConfirmarPerfilUiState,
    modoEdicion: Boolean,
    onCambiarNivel: (String, NivelHabilidad) -> Unit,
) {
    if (filas.isEmpty()) return
    TarjetaSeccion(titulo = titulo) {
        filas.forEach { fila ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fila.habilidad.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (fila.conEvidencia) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Con evidencia", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Dimensiones.espacioChico))
                }
                Text(estado.nivel(fila).etiqueta, style = MaterialTheme.typography.labelMedium, color = ChambaTema.colores.textoSecundario)
            }
            if (modoEdicion) {
                ChipSelector(
                    opciones = NivelHabilidad.entries,
                    seleccionadas = setOf(estado.nivel(fila)),
                    onClick = { onCambiarNivel(fila.habilidad.id, it) },
                    etiqueta = { it.etiqueta },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ConfirmarPerfilPreview() {
    ChambaReadyTheme {
        ConfirmarPerfilContenido(
            estado = ConfirmarPerfilUiState(
                nombre = "Ana Demo",
                detalleAcademico = "Ingeniería de Información · ciclo 7 · Universidad ESAN",
                habilidades = listOf(
                    FilaHabilidad(Habilidad("excel", "Excel", TipoHabilidad.TECNICA), NivelHabilidad.AVANZADO, true),
                    FilaHabilidad(Habilidad("sql", "SQL", TipoHabilidad.TECNICA), NivelHabilidad.BASICO, false),
                    FilaHabilidad(Habilidad("comunicacion", "Comunicación", TipoHabilidad.BLANDA), NivelHabilidad.INTERMEDIO, false),
                ),
                proyectos = listOf("Dashboard de ventas en Excel"),
                faltantes = listOf("Confirma tu nivel en Power BI."),
            ),
            modoEdicion = false,
            onBack = {}, onCambiarNivel = { _, _ -> }, onConfirmar = {}, onCorregir = {},
        )
    }
}
