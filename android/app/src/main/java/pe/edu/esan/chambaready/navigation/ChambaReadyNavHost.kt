package pe.edu.esan.chambaready.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.designsystem.componentes.BottomNavBar
import pe.edu.esan.chambaready.core.designsystem.componentes.ItemsBarraInferior
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.ui.comun.CargandoPantalla
import javax.inject.Inject

/**
 * Decide la pantalla inicial: Inicio si ya terminó el registro; Bienvenida si no.
 * Con login (Firebase configurado) además exige una sesión iniciada en este teléfono.
 */
@HiltViewModel
class SesionViewModel @Inject constructor(
    estadoRepositorio: EstadoUsuarioRepository,
    sesion: SesionRepository,
) : ViewModel() {
    /** Sin google-services.json la app corre con datos de prueba: se avisa arriba para no confundirse. */
    val modoDemo: Boolean = !sesion.disponible

    val destinoInicial: StateFlow<NavRoutes?> = flow {
        val sesionValida = !sesion.disponible || sesion.haySesion()
        val listo = sesionValida && estadoRepositorio.estado.first().onboardingCompleto
        emit(if (listo) MenuPrincipal.inicio else NavRoutes.Bienvenida)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * Raíz de la app: aplica el tema, muestra la barra inferior en toda la app (salvo el registro) y aloja el grafo.
 *
 * Reglas de navegación (sección "Navegación" del brief):
 * - "Atrás" siempre es `popBackStack()`: vuelve a la pantalla anterior REAL, nunca a una fija.
 * - Cambiar de pestaña guarda y restaura el estado de cada una y no apila pestañas repetidas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChambaReadyApp(sesion: SesionViewModel = hiltViewModel()) {
    val destinoInicial by sesion.destinoInicial.collectAsStateWithLifecycle()
    ChambaReadyTheme {
        val inicio = destinoInicial
        if (inicio == null) {
            CargandoPantalla()
        } else {
            ContenidoApp(destinoInicial = inicio, modoDemo = sesion.modoDemo)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContenidoApp(destinoInicial: NavRoutes, navController: NavHostController = rememberNavController(), modoDemo: Boolean = false) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val destino = entradaActual?.destination
    val indicePestana = MenuPrincipal.pestanas.indexOfFirst { pestana ->
        destino?.hierarchy?.any { it.hasRoute(pestana::class) } == true
    }
    // La barra inferior se ve en TODA la app (Mi plan, detalle, postular…), no solo en las pestañas.
    // Fuera de una pestaña queda marcada la última pestaña visitada (de donde vino el usuario).
    var ultimaPestana by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(indicePestana) { if (indicePestana >= 0) ultimaPestana = indicePestana }
    val mostrarBarra = entradaActual?.let { !esPantallaDeRegistro(it) } ?: false

    // Entrar a la app (tras el registro o con datos de prueba): limpia la pila para que "atrás" no vuelva al registro.
    val entrarALaApp: () -> Unit = {
        navController.navigate(MenuPrincipal.inicio) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    val cerrarSesion: () -> Unit = {
        navController.navigate(NavRoutes.Bienvenida) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Cada pantalla maneja sus propios márgenes del sistema (barra de estado, gestos).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (modoDemo) {
                // Las vacantes de prueba NO son las publicadas en la web: este aviso lo deja claro.
                Text(
                    "Modo demo: datos de prueba, sin conexión al backend",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChambaTema.colores.avisoAmbarTexto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChambaTema.colores.avisoAmbarFondo)
                        .statusBarsPadding()
                        .padding(vertical = 4.dp),
                )
            }
        },
        bottomBar = {
            if (mostrarBarra) {
                BottomNavBar(
                    items = ItemsBarraInferior.practicante,
                    indiceSeleccionado = if (indicePestana >= 0) indicePestana else ultimaPestana,
                    onSeleccion = { indice -> navController.irAPestana(MenuPrincipal.pestanas[indice]) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = destinoInicial,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            grafoRegistro(navController, onEntrarALaApp = entrarALaApp)
            grafoPracticante(navController, onCerrarSesion = cerrarSesion)
        }
    }
}

/**
 * Pantallas del registro e inicio de sesión: ahí NO va la barra inferior (el usuario aún no entra a
 * la app). P03 y P21 también se abren desde Perfil en modo edición: en ese caso sí la llevan.
 */
private fun esPantallaDeRegistro(entrada: NavBackStackEntry): Boolean {
    val destino = entrada.destination
    val siempreSinBarra = listOf(
        NavRoutes.Bienvenida::class, NavRoutes.Registro::class, NavRoutes.IniciarSesion::class, NavRoutes.CatalogoComponentes::class,
        NavRoutes.CargarPerfil::class, NavRoutes.AsistentePerfil::class, NavRoutes.TiempoLibre::class,
        NavRoutes.Autoevaluacion::class, NavRoutes.ResultadoAutoevaluacion::class,
    )
    return when {
        siempreSinBarra.any { destino.hasRoute(it) } -> true
        destino.hasRoute(NavRoutes.ConfirmarPerfil::class) -> !entrada.toRoute<NavRoutes.ConfirmarPerfil>().modoEdicion
        destino.hasRoute(NavRoutes.AreaInteres::class) -> !entrada.toRoute<NavRoutes.AreaInteres>().modoEdicion
        else -> false
    }
}

/**
 * Patrón estándar de pestañas: vuelve a la raíz de pestañas guardando el estado de la actual,
 * no duplica la pestaña si ya está arriba y restaura su estado anterior.
 * También se usa desde otras pantallas que llevan a una pestaña (por ejemplo, tras postular).
 */
fun NavHostController.irAPestana(pestana: NavRoutes) {
    navigate(pestana) {
        popUpTo(MenuPrincipal.inicio) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
