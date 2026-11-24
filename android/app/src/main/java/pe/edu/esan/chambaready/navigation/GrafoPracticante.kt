package pe.edu.esan.chambaready.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.ui.inicio.InicioScreen
import pe.edu.esan.chambaready.ui.notificaciones.NotificacionesScreen
import pe.edu.esan.chambaready.ui.perfil.EvidenciasScreen
import pe.edu.esan.chambaready.ui.perfil.PerfilScreen
import pe.edu.esan.chambaready.ui.perfil.TiempoLibreScreen
import pe.edu.esan.chambaready.ui.plan.MiProgresoScreen
import pe.edu.esan.chambaready.ui.plan.MiniTestScreen
import pe.edu.esan.chambaready.ui.plan.PendientesScreen
import pe.edu.esan.chambaready.ui.plan.PlanScreen
import pe.edu.esan.chambaready.ui.postulacion.PostulacionesScreen
import pe.edu.esan.chambaready.ui.postulacion.PostularScreen
import pe.edu.esan.chambaready.ui.postulacion.PreguntasFiltroScreen
import pe.edu.esan.chambaready.ui.practicante.vacantes.VacantesScreen
import pe.edu.esan.chambaready.ui.premium.PremiumScreen
import pe.edu.esan.chambaready.ui.vacante.DetalleVacanteScreen
import pe.edu.esan.chambaready.ui.vacante.DiagnosticoScreen
import pe.edu.esan.chambaready.ui.vacante.NoViableScreen

/**
 * Pantallas de uso diario (P07–P20, P22–P24). Todas las flechas "atrás" llaman a [volver] =
 * `popBackStack()`, así que regresan a la pantalla desde la que se llegó:
 * - Pendientes → Plan → atrás = Pendientes.
 * - Notificaciones → Plan → atrás = Notificaciones.
 * - Diagnóstico → Premium → (activar) Plan → atrás = Diagnóstico.
 */
fun NavGraphBuilder.grafoPracticante(
    navController: NavHostController,
    onCerrarSesion: () -> Unit,
) {
    val volver: () -> Unit = { navController.popBackStack() }
    val abrirVacante: (String) -> Unit = { navController.navigate(NavRoutes.DetalleVacante(it)) }
    val abrirPlan: (String) -> Unit = { navController.navigate(NavRoutes.Plan(it)) }

    // ---------- Pestañas ----------
    composable<NavRoutes.InicioPracticante> {
        InicioScreen(
            onNotificaciones = { navController.navigate(NavRoutes.Notificaciones) },
            onAbrirVacante = abrirVacante,
            onAbrirPlan = abrirPlan,
            onPremium = { navController.navigate(NavRoutes.Premium()) },
        )
    }
    composable<NavRoutes.Vacantes> { VacantesScreen(onAbrirVacante = abrirVacante) }
    composable<NavRoutes.Pendientes> {
        PendientesScreen(onAbrirPlan = abrirPlan, onAbrirVacante = abrirVacante, onPremium = { navController.navigate(NavRoutes.Premium()) })
    }
    composable<NavRoutes.Postulaciones> {
        PostulacionesScreen(
            onMejorar = { navController.navigate(NavRoutes.Diagnostico(it)) },
            // Regla 13: la empresa pidió las preguntas; al terminar vuelve a esta lista.
            onResponderPreguntas = { vacanteId, postulacionId -> navController.navigate(NavRoutes.PreguntasFiltro(vacanteId, postulacionId)) },
        )
    }
    composable<NavRoutes.PerfilPracticante> {
        PerfilScreen(
            // "Editar datos y habilidades" es Premium: sin Premium abre P11; con Premium, P03 en edición.
            onEditarDatos = { premium ->
                navController.navigate(if (premium) NavRoutes.ConfirmarPerfil(modoEdicion = true) else NavRoutes.Premium())
            },
            onPremium = { navController.navigate(NavRoutes.Premium()) },
            onEvidencias = { navController.navigate(NavRoutes.MisEvidencias) },
            onArea = { navController.navigate(NavRoutes.AreaInteres(modoEdicion = true)) },
            onTiempoLibre = { navController.navigate(NavRoutes.EditarTiempoLibre) },
            onSesionCerrada = onCerrarSesion,
        )
    }

    // ---------- Uso diario ----------
    composable<NavRoutes.Notificaciones> {
        NotificacionesScreen(onBack = volver, onAbrir = { navController.abrirNotificacion(it) })
    }
    composable<NavRoutes.DetalleVacante> {
        DetalleVacanteScreen(
            onBack = volver,
            onMejorar = { navController.navigate(NavRoutes.Diagnostico(it)) },
            onPostular = { navController.navigate(NavRoutes.Postular(it)) },
        )
    }
    composable<NavRoutes.Diagnostico> {
        DiagnosticoScreen(
            onBack = volver,
            onPremium = { navController.navigate(NavRoutes.Premium(planVacanteId = it)) },
            onPlan = abrirPlan,
            onNoViable = { navController.navigate(NavRoutes.NoViable(it)) },
        )
    }
    composable<NavRoutes.Premium> { entrada ->
        val ruta = entrada.toRoute<NavRoutes.Premium>()
        PremiumScreen(
            onBack = volver,
            onActivado = {
                val vacanteId = ruta.planVacanteId
                if (vacanteId != null) {
                    // Vuelve al contexto: si venía del diagnóstico, abre el plan en lugar de Premium.
                    navController.navigate(NavRoutes.Plan(vacanteId)) { popUpTo<NavRoutes.Premium> { inclusive = true } }
                } else {
                    volver()
                }
            },
        )
    }
    composable<NavRoutes.Plan> {
        PlanScreen(
            onBack = volver,
            onMiniTest = { vacante, modulo -> navController.navigate(NavRoutes.MiniTest(vacante, modulo)) },
            onProgreso = { navController.navigate(NavRoutes.MiProgreso(it)) },
            onPremium = { navController.navigate(NavRoutes.Premium(planVacanteId = it)) },
        )
    }
    composable<NavRoutes.NoViable> {
        NoViableScreen(
            onBack = volver,
            onVacante = abrirVacante,
            onSubirHoras = { navController.navigate(NavRoutes.EditarTiempoLibre) },
            onPostular = { navController.navigate(NavRoutes.Postular(it)) },
        )
    }
    composable<NavRoutes.MiniTest> { entrada ->
        val ruta = entrada.toRoute<NavRoutes.MiniTest>()
        MiniTestScreen(
            onBack = volver,
            onTerminado = {
                // El resultado reemplaza al mini-test: "atrás" (o "Seguir con mi plan") vuelve al plan.
                navController.navigate(NavRoutes.MiProgreso(ruta.vacanteId, trasMiniTest = true)) {
                    popUpTo<NavRoutes.MiniTest> { inclusive = true }
                }
            },
            onSubirCertificado = { navController.navigate(NavRoutes.MisEvidencias) },
        )
    }
    composable<NavRoutes.MiProgreso> { entrada ->
        val ruta = entrada.toRoute<NavRoutes.MiProgreso>()
        MiProgresoScreen(
            trasMiniTest = ruta.trasMiniTest,
            onBack = volver,
            onVolverAlInicio = { navController.irAPestana(NavRoutes.InicioPracticante) },
            onSeguirPlan = volver,
            onPostular = { navController.navigate(NavRoutes.Postular(ruta.vacanteId)) },
            onPremium = { navController.navigate(NavRoutes.Premium(planVacanteId = ruta.vacanteId)) },
        )
    }
    composable<NavRoutes.Postular> {
        PostularScreen(
            onBack = volver,
            onResponderPreguntas = { navController.navigate(NavRoutes.PreguntasFiltro(it)) },
            onEnviada = { navController.irAPestana(NavRoutes.Postulaciones) },
        )
    }
    composable<NavRoutes.PreguntasFiltro> {
        // Al terminar vuelve a la pantalla anterior: P17 (antes de postular) o P18 (preguntas pendientes).
        PreguntasFiltroScreen(onBack = volver, onTerminado = volver)
    }
    composable<NavRoutes.EditarTiempoLibre> {
        TiempoLibreScreen(modoEdicion = true, onBack = volver, onGuardado = volver)
    }
    composable<NavRoutes.MisEvidencias> { EvidenciasScreen(onBack = volver) }
}

/**
 * Cada notificación abre su pantalla (en F9 se usa lo mismo para los deep links de FCM).
 * Las que llevan a una pestaña usan [irAPestana]; las demás se apilan sobre Notificaciones.
 */
fun NavHostController.abrirNotificacion(notificacion: Notificacion) {
    val vacanteId = notificacion.vacanteId
    when (notificacion.tipo) {
        TipoNotificacion.CIERRE_CERCANO, TipoNotificacion.NUEVA_VACANTE ->
            if (vacanteId != null) navigate(NavRoutes.DetalleVacante(vacanteId))
        TipoNotificacion.PLAN_SEMANAL ->
            if (vacanteId != null) navigate(NavRoutes.Plan(vacanteId))
        TipoNotificacion.CAMBIO_ESTADO, TipoNotificacion.ENTREVISTA, TipoNotificacion.PREGUNTAS_FILTRO -> irAPestana(NavRoutes.Postulaciones)
        TipoNotificacion.RANKING -> irAPestana(NavRoutes.InicioPracticante)
        TipoNotificacion.PAGO -> navigate(NavRoutes.Premium())
        TipoNotificacion.EVIDENCIA -> navigate(NavRoutes.MisEvidencias)
    }
}
