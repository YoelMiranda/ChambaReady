package pe.edu.esan.chambaready.navigation

/**
 * Pestañas de la barra inferior, en el mismo orden que
 * [pe.edu.esan.chambaready.core.designsystem.componentes.ItemsBarraInferior.practicante].
 * Probado en `MenuPrincipalTest`.
 */
object MenuPrincipal {

    val pestanas: List<NavRoutes> = listOf(
        NavRoutes.InicioPracticante,
        NavRoutes.Vacantes,
        NavRoutes.Pendientes,
        NavRoutes.Postulaciones,
        NavRoutes.PerfilPracticante,
    )

    /** Primera pestaña: es a donde se entra después del registro o del login. */
    val inicio: NavRoutes get() = pestanas.first()
}
