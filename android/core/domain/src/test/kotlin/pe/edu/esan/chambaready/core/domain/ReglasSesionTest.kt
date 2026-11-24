package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.RolUsuario
import pe.edu.esan.chambaready.core.domain.reglas.DestinoTrasLogin
import pe.edu.esan.chambaready.core.domain.reglas.ReglasSesion

class ReglasSesionTest {

    @Test
    fun `la contrasena necesita 8 caracteres con letras y numeros`() {
        assertNotNull(ReglasSesion.validarContrasena("abc12"))
        assertNotNull(ReglasSesion.validarContrasena("solamenteletras"))
        assertNotNull(ReglasSesion.validarContrasena("12345678"))
        assertNull(ReglasSesion.validarContrasena("chamba2026"))
    }

    @Test
    fun `una cuenta sin registrar completa el registro`() {
        assertEquals(DestinoTrasLogin.COMPLETAR_REGISTRO, ReglasSesion.destinoTrasLogin(registrado = false, rol = null))
        // Registrado pero con un rol que no entendemos: no lo dejamos entrar como practicante.
        assertEquals(DestinoTrasLogin.COMPLETAR_REGISTRO, ReglasSesion.destinoTrasLogin(registrado = true, rol = null))
    }

    @Test
    fun `una empresa no entra a la app y el practicante si`() {
        assertEquals(DestinoTrasLogin.USAR_WEB_EMPRESA, ReglasSesion.destinoTrasLogin(true, RolUsuario.EMPRESA))
        assertEquals(DestinoTrasLogin.ENTRAR, ReglasSesion.destinoTrasLogin(true, RolUsuario.PRACTICANTE))
    }

    @Test
    fun `el rol se lee del texto del backend`() {
        assertEquals(RolUsuario.PRACTICANTE, ReglasSesion.rolDesdeTexto("PRACTICANTE"))
        assertNull(ReglasSesion.rolDesdeTexto("ADMIN"))
        assertNull(ReglasSesion.rolDesdeTexto(null))
    }
}
