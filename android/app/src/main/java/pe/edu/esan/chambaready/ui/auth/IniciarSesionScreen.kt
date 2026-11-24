package pe.edu.esan.chambaready.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.auth.ErrorAuth
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.domain.reglas.DestinoTrasLogin
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.registro.Campo
import javax.inject.Inject

data class IniciarSesionUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    /** false en MODO DEMO: la pantalla explica cómo activar el login. */
    val loginDisponible: Boolean = true,
)

sealed interface IniciarSesionEvento {
    data class Mensaje(val texto: String) : IniciarSesionEvento

    /** Practicante con el registro terminado: entra a Inicio. */
    data object Entrar : IniciarSesionEvento

    /** Cuenta de Firebase sin registro en ChambaReady: completa C02. */
    data object CompletarRegistro : IniciarSesionEvento

    /** Registrado como practicante pero no terminó P01–P06: sigue desde P01. */
    data object ContinuarPerfil : IniciarSesionEvento
}

/**
 * C03 Iniciar sesión. Después de Firebase pregunta al backend (GET /v1/sesion) qué rol tiene la
 * cuenta: una empresa no entra a la app; un practicante recupera su perfil guardado.
 */
@HiltViewModel
class IniciarSesionViewModel @Inject constructor(
    private val sesion: SesionRepository,
    private val cuenta: CuentaRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
) : BaseViewModel<IniciarSesionUiState, IniciarSesionEvento>(IniciarSesionUiState(loginDisponible = sesion.disponible)) {

    fun onCorreo(valor: String) = setState { copy(correo = valor) }
    fun onContrasena(valor: String) = setState { copy(contrasena = valor) }

    fun iniciarSesion() {
        val estado = currentState
        if (estado.correo.isBlank() || estado.contrasena.isEmpty()) {
            sendEvent(IniciarSesionEvento.Mensaje("Escribe tu correo y tu contraseña."))
            return
        }
        ejecutar { sesion.iniciarSesion(estado.correo, estado.contrasena) }
    }

    fun olvideContrasena() {
        val correo = currentState.correo
        if (correo.isBlank()) {
            sendEvent(IniciarSesionEvento.Mensaje("Escribe tu correo y vuelve a tocar «Olvidé mi contraseña»."))
            return
        }
        viewModelScope.launch {
            try {
                sesion.enviarCorreoDeRecuperacion(correo)
                sendEvent(IniciarSesionEvento.Mensaje("Te enviamos un correo para crear una contraseña nueva."))
            } catch (e: ErrorAuth) {
                sendEvent(IniciarSesionEvento.Mensaje(e.message.orEmpty()))
            }
        }
    }

    /** Corre el login de Firebase y luego decide a dónde ir. */
    private fun ejecutar(login: suspend () -> Unit) {
        if (currentState.cargando) return
        setState { copy(cargando = true) }
        viewModelScope.launch {
            try {
                login()
                decidirDestino()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAuth) {
                sendEvent(IniciarSesionEvento.Mensaje(e.message.orEmpty()))
            } catch (e: ErrorRemoto) {
                // Firebase dijo que sí, pero el backend no respondió: no dejamos una sesión a medias.
                sesion.cerrarSesion()
                sendEvent(IniciarSesionEvento.Mensaje(e.message.orEmpty()))
            } finally {
                setState { copy(cargando = false) }
            }
        }
    }

    private suspend fun decidirDestino() {
        when (cuenta.destinoTrasLogin()) {
            DestinoTrasLogin.USAR_WEB_EMPRESA -> {
                sesion.cerrarSesion()
                sendEvent(IniciarSesionEvento.Mensaje("Esta cuenta es de una empresa. Ingresa desde la web de empresas."))
            }
            DestinoTrasLogin.COMPLETAR_REGISTRO -> sendEvent(IniciarSesionEvento.CompletarRegistro)
            DestinoTrasLogin.ENTRAR -> {
                // Teléfono nuevo o reinstalación: parte de un estado limpio y trae lo del backend.
                val local = estadoRepositorio.estado.first()
                val base = if (local.registro?.correo.equals(sesion.correoActual(), ignoreCase = true)) local else EstadoUsuario()
                val remoto = cuenta.traerPerfil(base, sesion.correoActual())
                estadoRepositorio.actualizar { remoto }
                sendEvent(if (remoto.onboardingCompleto) IniciarSesionEvento.Entrar else IniciarSesionEvento.ContinuarPerfil)
            }
        }
    }
}

@Composable
fun IniciarSesionScreen(
    onBack: () -> Unit,
    onEntrar: () -> Unit,
    onCompletarRegistro: () -> Unit,
    onContinuarPerfil: () -> Unit,
    viewModel: IniciarSesionViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            when (evento) {
                is IniciarSesionEvento.Mensaje -> snackbar.showSnackbar(evento.texto)
                IniciarSesionEvento.Entrar -> onEntrar()
                IniciarSesionEvento.CompletarRegistro -> onCompletarRegistro()
                IniciarSesionEvento.ContinuarPerfil -> onContinuarPerfil()
            }
        }
    }
    IniciarSesionContenido(
        estado = estado,
        snackbar = snackbar,
        onBack = onBack,
        onCorreo = viewModel::onCorreo,
        onContrasena = viewModel::onContrasena,
        onIniciar = viewModel::iniciarSesion,
        onOlvide = viewModel::olvideContrasena,
    )
}

@Composable
fun IniciarSesionContenido(
    estado: IniciarSesionUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onCorreo: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onIniciar: () -> Unit,
    onOlvide: () -> Unit,
) {
    PantallaBase(
        titulo = "Inicia sesión",
        onBack = onBack,
        snackbarHostState = snackbar,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(
                    "Iniciar sesión",
                    onClick = onIniciar,
                    enabled = estado.loginDisponible,
                    cargando = estado.cargando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            if (!estado.loginDisponible) {
                Text(
                    "Esta versión corre en modo demo, sin login. Para activarlo, agrega app/google-services.json (ver docs/despliegue.md).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Campo("Correo", estado.correo, onCorreo, error = null, tipoTeclado = KeyboardType.Email)
            CampoContrasena(estado.contrasena, onContrasena, error = null)
            TextButton(onClick = onOlvide, enabled = estado.loginDisponible) { Text("Olvidé mi contraseña") }
        }
    }
}

/** Campo de contraseña con botón para mostrarla u ocultarla (también lo usa C02). */
@Composable
fun CampoContrasena(valor: String, onCambio: (String) -> Unit, error: String?, etiqueta: String = "Contraseña") {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun IniciarSesionPreview() {
    ChambaReadyTheme {
        IniciarSesionContenido(
            estado = IniciarSesionUiState(correo = "ana@uni.pe"),
            snackbar = SnackbarHostState(),
            onBack = {}, onCorreo = {}, onContrasena = {}, onIniciar = {}, onOlvide = {},
        )
    }
}
