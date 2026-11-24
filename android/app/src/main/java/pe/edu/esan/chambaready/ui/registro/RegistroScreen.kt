package pe.edu.esan.chambaready.ui.registro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.auth.ErrorAuth
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.reglas.CampoRegistro
import pe.edu.esan.chambaready.core.domain.reglas.ReglasRegistro
import pe.edu.esan.chambaready.core.domain.reglas.ReglasSesion
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.auth.CampoContrasena
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import javax.inject.Inject

/** Total de pasos del registro: C02, P01, P02, P03, P04 y P21. */
const val PASOS_REGISTRO = 6

data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val universidad: String = "",
    val carrera: String = "",
    val ciclo: String = "",
    val errores: Map<CampoRegistro, String> = emptyMap(),
    val universidades: List<String> = emptyList(),
    val contrasena: String = "",
    val errorContrasena: String? = null,
    /** false en MODO DEMO: no se crea cuenta en Firebase, todo queda en el teléfono. */
    val loginDisponible: Boolean = false,
    /** Ya hay sesión de Firebase (entró desde C03 sin registro): no se pide contraseña y el correo es fijo. */
    val conSesion: Boolean = false,
    val cargando: Boolean = false,
) {
    /** La contraseña solo se pide al crear una cuenta nueva con correo. */
    val pideContrasena: Boolean get() = loginDisponible && !conSesion

    /** Sugerencias del buscador de universidad (se ocultan cuando ya eligió una de la lista). */
    val sugerencias: List<String>
        get() = if (universidad.isBlank() || universidad in universidades) {
            emptyList()
        } else {
            universidades.filter { it.contains(universidad.trim(), ignoreCase = true) }.take(4)
        }

    fun aDatos() = DatosRegistro(nombre.trim(), correo.trim(), universidad.trim(), carrera.trim(), ciclo.toIntOrNull() ?: 0)
}

/**
 * C02. Valida con [ReglasRegistro] y guarda los datos en el estado del usuario.
 * Con Firebase configurado además: crea la cuenta con correo y contraseña, registra el rol
 * PRACTICANTE en el backend (POST /v1/registro) y sube los datos del perfil.
 */
@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val sesion: SesionRepository,
    private val cuenta: CuentaRepository,
    contenido: ContenidoRepository,
) : BaseViewModel<RegistroUiState, String>(
    RegistroUiState(
        universidades = contenido.universidades(),
        loginDisponible = sesion.disponible,
        conSesion = sesion.haySesion(),
        // Si viene de C03 con una cuenta sin registrar, ya sabemos su correo.
        correo = sesion.correoActual(),
    ),
) {

    fun onNombre(valor: String) = setState { copy(nombre = valor, errores = errores - CampoRegistro.NOMBRE) }
    fun onCorreo(valor: String) = setState { copy(correo = valor, errores = errores - CampoRegistro.CORREO) }
    fun onUniversidad(valor: String) = setState { copy(universidad = valor, errores = errores - CampoRegistro.UNIVERSIDAD) }
    fun onCarrera(valor: String) = setState { copy(carrera = valor, errores = errores - CampoRegistro.CARRERA) }
    fun onCiclo(valor: String) = setState { copy(ciclo = valor.filter { it.isDigit() }.take(2), errores = errores - CampoRegistro.CICLO) }
    fun onContrasena(valor: String) = setState { copy(contrasena = valor, errorContrasena = null) }

    fun crearCuenta(alTerminar: () -> Unit) {
        val estado = currentState
        if (estado.cargando) return
        val datos = estado.aDatos()
        val errores = ReglasRegistro.validar(datos)
        val errorContrasena = if (estado.pideContrasena) ReglasSesion.validarContrasena(estado.contrasena) else null
        setState { copy(errores = errores, errorContrasena = errorContrasena) }
        if (errores.isNotEmpty() || errorContrasena != null) return

        setState { copy(cargando = true) }
        viewModelScope.launch {
            try {
                if (estado.pideContrasena) {
                    sesion.crearCuenta(datos.correo, estado.contrasena)
                    // Si lo siguiente falla, al reintentar ya no se vuelve a crear la cuenta.
                    setState { copy(conSesion = true) }
                }
                cuenta.registrarPracticante(datos.nombre)
                // Al guardar, SincronizadorPerfil sube estos datos al backend.
                estadoRepositorio.actualizar { it.copy(registro = datos) }
                alTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAuth) {
                sendEvent(e.message.orEmpty())
            } catch (e: ErrorRemoto) {
                // 409 = el correo ya es de una empresa: esa cuenta no puede ser practicante.
                if (e.estadoHttp == 409) {
                    sesion.cerrarSesion()
                    setState { copy(conSesion = false) }
                }
                sendEvent(e.message.orEmpty())
            } finally {
                setState { copy(cargando = false) }
            }
        }
    }
}

@Composable
fun RegistroScreen(
    onBack: () -> Unit,
    onCuentaCreada: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it) } }
    RegistroContenido(
        estado = estado,
        snackbar = snackbar,
        onBack = onBack,
        onNombre = viewModel::onNombre,
        onCorreo = viewModel::onCorreo,
        onUniversidad = viewModel::onUniversidad,
        onCarrera = viewModel::onCarrera,
        onCiclo = viewModel::onCiclo,
        onContrasena = viewModel::onContrasena,
        onCrearCuenta = { viewModel.crearCuenta(onCuentaCreada) },
    )
}

@Composable
fun RegistroContenido(
    estado: RegistroUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onNombre: (String) -> Unit,
    onCorreo: (String) -> Unit,
    onUniversidad: (String) -> Unit,
    onCarrera: (String) -> Unit,
    onCiclo: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onCrearCuenta: () -> Unit,
) {
    PantallaBase(
        titulo = "Crea tu cuenta",
        onBack = onBack,
        snackbarHostState = snackbar,
        barraInferior = {
            BarraAcciones {
                PrimaryButton(
                    if (estado.conSesion) "Continuar" else "Crear cuenta",
                    onClick = onCrearCuenta,
                    cargando = estado.cargando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        ContenidoDesplazable(padding) {
            StepProgress(pasoActual = 1, totalPasos = PASOS_REGISTRO)
            Campo("Nombre y apellido", estado.nombre, onNombre, estado.errores[CampoRegistro.NOMBRE])
            Campo(
                "Correo",
                estado.correo,
                onCorreo,
                estado.errores[CampoRegistro.CORREO],
                KeyboardType.Email,
                // Con sesión iniciada el correo es el de la cuenta y no se cambia aquí.
                habilitado = !estado.conSesion,
            )
            if (estado.pideContrasena) {
                CampoContrasena(estado.contrasena, onContrasena, estado.errorContrasena, etiqueta = "Contraseña (8 o más, con letras y números)")
            }
            Column {
                Campo("Universidad (escribe para buscar)", estado.universidad, onUniversidad, estado.errores[CampoRegistro.UNIVERSIDAD])
                estado.sugerencias.forEach { sugerencia ->
                    Text(
                        sugerencia,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUniversidad(sugerencia) }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
            Campo("Carrera", estado.carrera, onCarrera, estado.errores[CampoRegistro.CARRERA])
            Campo("Ciclo (1 a 14)", estado.ciclo, onCiclo, estado.errores[CampoRegistro.CICLO], KeyboardType.Number)
        }
    }
}

/** Campo de texto con su error debajo (el error también lo anuncia TalkBack). */
@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        enabled = habilitado,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun RegistroPreview() {
    ChambaReadyTheme {
        RegistroContenido(
            estado = RegistroUiState(
                nombre = "Ana", correo = "ana@", universidad = "Univ", universidades = MockDatos.universidades,
                errores = mapOf(CampoRegistro.CORREO to "Escribe un correo válido."), loginDisponible = true,
            ),
            snackbar = SnackbarHostState(),
            onBack = {}, onNombre = {}, onCorreo = {}, onUniversidad = {}, onCarrera = {}, onCiclo = {}, onContrasena = {}, onCrearCuenta = {},
        )
    }
}
