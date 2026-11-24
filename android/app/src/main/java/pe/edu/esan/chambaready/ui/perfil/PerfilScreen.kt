package pe.edu.esan.chambaready.ui.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.formatoHoras
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import androidx.compose.foundation.layout.PaddingValues
import javax.inject.Inject

data class PerfilUiState(
    val nombre: String = "",
    val area: String = "Sin elegir",
    val premium: Boolean = false,
    val horasSemanales: Double = 0.0,
    val evidencias: Int = 0,
    val avisos: Map<TipoNotificacion, Boolean> = emptyMap(),
)

/** P19 Perfil y configuración. */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val sesion: SesionRepository,
) : BaseViewModel<PerfilUiState, Nothing>(PerfilUiState()) {

    init {
        viewModelScope.launch {
            estadoRepositorio.estado.collect { e ->
                setState {
                    copy(
                        nombre = e.registro?.nombre ?: "Practicante",
                        area = e.area?.etiqueta ?: "Sin elegir",
                        premium = e.premium,
                        horasSemanales = e.horasLibres.totalSemanal,
                        evidencias = e.evidencias.size,
                        avisos = e.avisosActivos,
                    )
                }
            }
        }
    }

    fun cambiarAviso(tipo: TipoNotificacion, activo: Boolean) {
        viewModelScope.launch { estadoRepositorio.actualizar { it.copy(avisosActivos = it.avisosActivos + (tipo to activo)) } }
    }

    /**
     * Cierra la sesión de Firebase (si hay) y borra los datos del teléfono. Lo guardado en el backend
     * no se pierde: vuelve al iniciar sesión otra vez.
     */
    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            sesion.cerrarSesion()
            estadoRepositorio.borrarTodo()
            alTerminar()
        }
    }
}

@Composable
fun PerfilScreen(
    onEditarDatos: (premium: Boolean) -> Unit,
    onPremium: () -> Unit,
    onEvidencias: () -> Unit,
    onArea: () -> Unit,
    onTiempoLibre: () -> Unit,
    onSesionCerrada: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    PerfilContenido(
        estado = estado,
        onEditarDatos = { onEditarDatos(estado.premium) },
        onPremium = onPremium,
        onEvidencias = onEvidencias,
        onArea = onArea,
        onTiempoLibre = onTiempoLibre,
        onCambiarAviso = viewModel::cambiarAviso,
        onCerrarSesion = { viewModel.cerrarSesion(onSesionCerrada) },
    )
}

@Composable
fun PerfilContenido(
    estado: PerfilUiState,
    onEditarDatos: () -> Unit,
    onPremium: () -> Unit,
    onEvidencias: () -> Unit,
    onArea: () -> Unit,
    onTiempoLibre: () -> Unit,
    onCambiarAviso: (TipoNotificacion, Boolean) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    var confirmarSalida by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        ContenidoDesplazable(PaddingValues(), modifier = Modifier.statusBarsPadding()) {
            Column {
                Text(estado.nombre, style = MaterialTheme.typography.headlineSmall)
                Text(estado.area, style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
            }
            TarjetaSeccion(titulo = if (estado.premium) "Premium activo" else "Cuenta gratis") {
                Text(
                    if (estado.premium) {
                        "Tienes el detalle del diagnóstico, tu plan, Mis pendientes y la edición de tu perfil."
                    } else {
                        "Con Premium desbloqueas el detalle del diagnóstico, el plan y Mis pendientes."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChambaTema.colores.textoSecundario,
                )
                if (!estado.premium) {
                    PrimaryButton("Ver Premium", onClick = onPremium, icono = Icons.Outlined.WorkspacePremium, modifier = Modifier.fillMaxWidth())
                }
            }
            TarjetaSeccion {
                Opcion(Icons.Outlined.Edit, "Editar datos y habilidades", if (estado.premium) null else "Premium", onEditarDatos)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Opcion(Icons.Outlined.Verified, "Mis evidencias", "${estado.evidencias}", onEvidencias)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Opcion(Icons.Outlined.Interests, "Área de interés", estado.area, onArea)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Opcion(Icons.Outlined.Schedule, "Mi tiempo libre", "${formatoHoras(estado.horasSemanales)} por semana", onTiempoLibre)
            }
            TarjetaSeccion(titulo = "Notificaciones") {
                TipoNotificacion.entries.forEach { tipo ->
                    val activo = estado.avisos[tipo] ?: true
                    ListItem(
                        headlineContent = { Text(tipo.etiqueta) },
                        trailingContent = { Switch(checked = activo, onCheckedChange = null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.clickable(role = Role.Switch) { onCambiarAviso(tipo, !activo) },
                    )
                }
            }
            TextButton(onClick = { confirmarSalida = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Text("  Cerrar sesión")
            }
        }
    }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("En esta versión de prueba se borrarán los datos guardados en este teléfono.") },
            confirmButton = { TextButton(onClick = { confirmarSalida = false; onCerrarSesion() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Opcion(icono: ImageVector, titulo: String, detalle: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = if (detalle != null) {
            { Text(detalle, color = ChambaTema.colores.textoSecundario) }
        } else {
            null
        },
        leadingContent = { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun PerfilPreview() {
    ChambaReadyTheme {
        PerfilContenido(
            estado = PerfilUiState("Ana Demo", "Data y Analytics", premium = false, horasSemanales = 13.0, evidencias = 2, avisos = TipoNotificacion.entries.associateWith { true }),
            onEditarDatos = {}, onPremium = {}, onEvidencias = {}, onArea = {}, onTiempoLibre = {}, onCambiarAviso = { _, _ -> }, onCerrarSesion = {},
        )
    }
}
