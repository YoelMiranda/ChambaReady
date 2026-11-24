package pe.edu.esan.chambaready.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** Barra inferior: 5 pestañas en el orden de los mockups. */
class MenuPrincipalTest {

    @Test
    fun cincoPestanasEnOrden() {
        assertEquals(
            listOf(
                NavRoutes.InicioPracticante,
                NavRoutes.Vacantes,
                NavRoutes.Pendientes,
                NavRoutes.Postulaciones,
                NavRoutes.PerfilPracticante,
            ),
            MenuPrincipal.pestanas,
        )
    }

    @Test
    fun inicio_esLaPrimeraPestana() {
        assertEquals(NavRoutes.InicioPracticante, MenuPrincipal.inicio)
    }
}
