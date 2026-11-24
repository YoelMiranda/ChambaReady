package pe.edu.esan.chambaready.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
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
    val destinoInicial: StateFlow<NavRoutes?> = flow {
        val sesionValida = !sesion.disponible || sesion.haySesion()
        val listo = sesionValida && estadoRepositorio.estado.first().onboardingCompleto
        emit(if (listo) MenuPrincipal.inicio else NavRoutes.Bienvenida)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * Raíz de la app: aplica el tema, muestra la barra inferior solo en las pestañas y aloja el grafo.
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
            ContenidoApp(destinoInicial = inicio)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContenidoApp(destinoInicial: NavRoutes, navController: NavHostController = rememberNavController()) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val destino = entradaActual?.destination
    val indicePestana = MenuPrincipal.pestanas.indexOfFirst { pestana ->
        destino?.hierarchy?.any { it.hasRoute(pestana::class) } == true
    }

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
        bottomBar = {
            if (indicePestana >= 0) {
                BottomNavBar(
                    items = ItemsBarraInferior.practicante,
                    indiceSeleccionado = indicePestana,
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
