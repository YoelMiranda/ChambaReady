package pe.edu.esan.chambaready.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * Reglas de navegación del brief: "atrás" vuelve a la pantalla anterior real y las pestañas no se
 * apilan. Usa las mismas rutas y ayudantes que la app ([irAPestana], [abrirNotificacion]) sobre
 * pantallas vacías, para no depender de Hilt ni de los datos.
 *
 * Se ejecuta en un emulador: `./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: TestNavHostController

    @Before
    fun preparar() {
        compose.setContent {
            nav = TestNavHostController(LocalContext.current)
            nav.navigatorProvider.addNavigator(ComposeNavigator())
            NavHost(navController = nav, startDestination = NavRoutes.InicioPracticante) {
                composable<NavRoutes.InicioPracticante> { Text("Inicio") }
                composable<NavRoutes.Vacantes> { Text("Vacantes") }
                composable<NavRoutes.Pendientes> { Text("Pendientes") }
                composable<NavRoutes.Postulaciones> { Text("Postulaciones") }
                composable<NavRoutes.PerfilPracticante> { Text("Perfil") }
                composable<NavRoutes.Notificaciones> { Text("Notificaciones") }
                composable<NavRoutes.Plan> { Text("Plan") }
                composable<NavRoutes.Premium> { Text("Premium") }
                composable<NavRoutes.Diagnostico> { Text("Diagnóstico") }
                composable<NavRoutes.DetalleVacante> { Text("Detalle") }
            }
        }
    }

    private fun estaEn(ruta: KClass<*>): Boolean = nav.currentDestination?.hasRoute(ruta) == true

    private fun enUi(bloque: () -> Unit) {
        compose.runOnUiThread(bloque)
        compose.waitForIdle()
    }

    @Test
    fun pendientes_plan_atras_vuelveAPendientes() {
        enUi { nav.irAPestana(NavRoutes.Pendientes) }
        enUi { nav.navigate(NavRoutes.Plan("v1")) }
        enUi { nav.popBackStack() }
        assertTrue(estaEn(NavRoutes.Pendientes::class))
    }

    @Test
    fun notificaciones_plan_atras_vuelveANotificaciones() {
        enUi { nav.navigate(NavRoutes.Notificaciones) }
        enUi {
            nav.abrirNotificacion(
                Notificacion("n2", TipoNotificacion.PLAN_SEMANAL, "Plan", "Listo", LocalDateTime.now(), vacanteId = "v1"),
            )
        }
        assertTrue(estaEn(NavRoutes.Plan::class))
        enUi { nav.popBackStack() }
        assertTrue(estaEn(NavRoutes.Notificaciones::class))
    }

    @Test
    fun pestanas_noSeApilan() {
        enUi { nav.irAPestana(NavRoutes.Vacantes) }
        enUi { nav.irAPestana(NavRoutes.Pendientes) }
        enUi { nav.irAPestana(NavRoutes.Vacantes) }
        enUi { nav.irAPestana(NavRoutes.Vacantes) }
        // Pila: Inicio (raíz de pestañas) → Vacantes. Atrás vuelve a Inicio, no a Pendientes.
        val rutas = nav.currentBackStack.value.mapNotNull { it.destination.route }
        assertEquals(1, rutas.count { it.contains("Vacantes") })
        enUi { nav.popBackStack() }
        assertTrue(estaEn(NavRoutes.InicioPracticante::class))
    }

    @Test
    fun premiumDesdeDiagnostico_seReemplazaPorElPlan() {
        enUi { nav.navigate(NavRoutes.Diagnostico("v1")) }
        enUi { nav.navigate(NavRoutes.Premium(planVacanteId = "v1")) }
        // Lo mismo que hace la app al activar Premium (GrafoPracticante).
        enUi { nav.navigate(NavRoutes.Plan("v1")) { popUpTo<NavRoutes.Premium> { inclusive = true } } }
        enUi { nav.popBackStack() }
        assertTrue(estaEn(NavRoutes.Diagnostico::class))
    }
}
