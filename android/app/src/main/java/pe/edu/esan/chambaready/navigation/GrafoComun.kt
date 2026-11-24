package pe.edu.esan.chambaready.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pe.edu.esan.chambaready.ui.auth.IniciarSesionScreen
import pe.edu.esan.chambaready.ui.autoevaluacion.AutoevaluacionScreen
import pe.edu.esan.chambaready.ui.autoevaluacion.ResultadoAutoevaluacionScreen
import pe.edu.esan.chambaready.ui.bienvenida.BienvenidaScreen
import pe.edu.esan.chambaready.ui.catalogo.CatalogoComponentesScreen
import pe.edu.esan.chambaready.ui.perfil.AreaInteresScreen
import pe.edu.esan.chambaready.ui.perfil.ConfirmarPerfilScreen
import pe.edu.esan.chambaready.ui.perfil.TiempoLibreScreen
import pe.edu.esan.chambaready.ui.registro.AsistentePerfilScreen
import pe.edu.esan.chambaready.ui.registro.CargarPerfilScreen
import pe.edu.esan.chambaready.ui.registro.RegistroScreen

/**
 * C01, C02, C03 y el registro de una sola vez: P01 → P02 → P03 → P04 → P21 → P05 → P06.
 * P03 y P21 también se abren desde Perfil en modo edición: ahí "guardar" vuelve atrás.
 * Desde C03 (iniciar sesión) se entra a la app, se completa C02 o se sigue en P01, según la cuenta.
 */
fun NavGraphBuilder.grafoRegistro(
    navController: NavHostController,
    onEntrarALaApp: () -> Unit,
) {
    val volver: () -> Unit = { navController.popBackStack() }

    composable<NavRoutes.Bienvenida> {
        BienvenidaScreen(
            onSoyPracticante = { navController.navigate(NavRoutes.Registro) },
            onIniciarSesion = { navController.navigate(NavRoutes.IniciarSesion) },
            onEntrarConDatosDePrueba = onEntrarALaApp,
            onVerCatalogo = { navController.navigate(NavRoutes.CatalogoComponentes) },
        )
    }
    composable<NavRoutes.CatalogoComponentes> { CatalogoComponentesScreen(onBack = volver) }

    composable<NavRoutes.IniciarSesion> {
        IniciarSesionScreen(
            onBack = volver,
            onEntrar = onEntrarALaApp,
            // Reemplaza C03 en la pila: "atrás" desde C02 o P01 vuelve a Bienvenida, no al login.
            onCompletarRegistro = {
                navController.navigate(NavRoutes.Registro) { popUpTo<NavRoutes.IniciarSesion> { inclusive = true } }
            },
            onContinuarPerfil = {
                navController.navigate(NavRoutes.CargarPerfil) { popUpTo<NavRoutes.IniciarSesion> { inclusive = true } }
            },
        )
    }

    composable<NavRoutes.Registro> {
        RegistroScreen(onBack = volver, onCuentaCreada = { navController.navigate(NavRoutes.CargarPerfil) })
    }
    composable<NavRoutes.CargarPerfil> {
        CargarPerfilScreen(onBack = volver, onContinuar = { navController.navigate(NavRoutes.AsistentePerfil) })
    }
    composable<NavRoutes.AsistentePerfil> {
        AsistentePerfilScreen(onBack = volver, onTerminar = { navController.navigate(NavRoutes.ConfirmarPerfil()) })
    }
    composable<NavRoutes.ConfirmarPerfil> { entrada ->
        val ruta = entrada.toRoute<NavRoutes.ConfirmarPerfil>()
        ConfirmarPerfilScreen(
            modoEdicion = ruta.modoEdicion,
            onBack = volver,
            onConfirmar = { if (ruta.modoEdicion) volver() else navController.navigate(NavRoutes.TiempoLibre) },
            onCorregir = volver,
        )
    }
    composable<NavRoutes.TiempoLibre> {
        TiempoLibreScreen(modoEdicion = false, onBack = volver, onGuardado = { navController.navigate(NavRoutes.AreaInteres()) })
    }
    composable<NavRoutes.AreaInteres> { entrada ->
        val ruta = entrada.toRoute<NavRoutes.AreaInteres>()
        AreaInteresScreen(
            modoEdicion = ruta.modoEdicion,
            onBack = volver,
            onGuardado = { if (ruta.modoEdicion) volver() else navController.navigate(NavRoutes.Autoevaluacion) },
        )
    }
    composable<NavRoutes.Autoevaluacion> {
        AutoevaluacionScreen(onBack = volver, onTerminar = { navController.navigate(NavRoutes.ResultadoAutoevaluacion) })
    }
    composable<NavRoutes.ResultadoAutoevaluacion> {
        ResultadoAutoevaluacionScreen(onBack = volver, onIrAlInicio = onEntrarALaApp)
    }
}
