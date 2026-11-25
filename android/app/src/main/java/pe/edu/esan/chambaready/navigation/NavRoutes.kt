package pe.edu.esan.chambaready.navigation

import kotlinx.serialization.Serializable

/**
 * Todas las rutas de la app del estudiante, tipadas con kotlinx.serialization (Navigation 2.8+).
 * Se navega con objetos: `navController.navigate(NavRoutes.DetalleVacante(id))`.
 * El comentario de cada ruta indica su código de pantalla en los mockups.
 * Las pantallas de empresa NO están aquí: viven en la web (`web-empresa/`).
 */
sealed interface NavRoutes {

    // ---------- Comunes ----------
    /** C01 */
    @Serializable data object Bienvenida : NavRoutes

    /** C02 */
    @Serializable data object Registro : NavRoutes

    /** C03 Iniciar sesión (F5, solo con Firebase configurado). */
    @Serializable data object IniciarSesion : NavRoutes

    /** Solo para el equipo: muestra todos los componentes del sistema de diseño. */
    @Serializable data object CatalogoComponentes : NavRoutes

    // ---------- Registro (una sola vez) ----------
    /** P01 */
    @Serializable data object CargarPerfil : NavRoutes

    /** P02 */
    @Serializable data object AsistentePerfil : NavRoutes

    /** P03. Con [modoEdicion] se abre desde Perfil (Premium) y al guardar vuelve atrás. */
    @Serializable data class ConfirmarPerfil(val modoEdicion: Boolean = false) : NavRoutes

    /** P04 */
    @Serializable data object TiempoLibre : NavRoutes

    /** P21. Con [modoEdicion] se abre desde Perfil y al guardar vuelve atrás. */
    @Serializable data class AreaInteres(val modoEdicion: Boolean = false) : NavRoutes

    /** P05 */
    @Serializable data object Autoevaluacion : NavRoutes

    /** P06 */
    @Serializable data object ResultadoAutoevaluacion : NavRoutes

    // ---------- Pestañas de la barra inferior ----------
    /** P07 */
    @Serializable data object InicioPracticante : NavRoutes

    /** P08 */
    @Serializable data object Vacantes : NavRoutes

    /** P14 (Premium) */
    @Serializable data object Pendientes : NavRoutes

    /** P18 */
    @Serializable data object Postulaciones : NavRoutes

    /** P19 */
    @Serializable data object PerfilPracticante : NavRoutes

    // ---------- Uso diario ----------
    /** P20 */
    @Serializable data object Notificaciones : NavRoutes

    /** P09 */
    @Serializable data class DetalleVacante(val vacanteId: String) : NavRoutes

    /** P10 */
    @Serializable data class Diagnostico(val vacanteId: String) : NavRoutes

    /**
     * P11. Si [planVacanteId] no es null (se entró desde el diagnóstico), al activar Premium se
     * abre el plan de esa vacante en lugar de volver atrás.
     */
    @Serializable data class Premium(val planVacanteId: String? = null) : NavRoutes

    /** P12 */
    @Serializable data class Plan(val vacanteId: String) : NavRoutes

    /** P13 */
    @Serializable data class NoViable(val vacanteId: String) : NavRoutes

    /** P15 */
    @Serializable data class MiniTest(val vacanteId: String, val moduloId: String) : NavRoutes

    /** P16. Con [trasMiniTest] muestra el resultado y los botones "Volver al inicio" / "Seguir con mi plan". */
    @Serializable data class MiProgreso(val vacanteId: String, val trasMiniTest: Boolean = false) : NavRoutes

    /** P17 */
    @Serializable data class Postular(val vacanteId: String) : NavRoutes

    /** Evaluación de habilidades desde Perfil (gratis: 1 vez). En el registro se usa Autoevaluacion (P05). */
    @Serializable data object EvaluarHabilidades : NavRoutes

    /** P24 */
    @Serializable data class PreguntasFiltro(val vacanteId: String, val postulacionId: String? = null) : NavRoutes

    /** P22 */
    @Serializable data object EditarTiempoLibre : NavRoutes

    /** P23 */
    @Serializable data object MisEvidencias : NavRoutes
}
