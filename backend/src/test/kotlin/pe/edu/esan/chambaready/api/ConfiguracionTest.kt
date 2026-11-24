package pe.edu.esan.chambaready.api

import pe.edu.esan.chambaready.api.config.Configuracion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** La cadena de Neon/Heroku se convierte bien a JDBC y la configuración es segura por defecto. */
class ConfiguracionTest {

    @Test
    fun databaseUrlDeNeon_seConvierteAJdbc() {
        val c = Configuracion.convertirDatabaseUrl("postgresql://usuario:cl%40ve@ep-ejemplo-pooler.us-east-2.aws.neon.tech/chambaready?sslmode=require")
        assertEquals("jdbc:postgresql://ep-ejemplo-pooler.us-east-2.aws.neon.tech/chambaready?sslmode=require", c.jdbcUrl)
        assertEquals("usuario", c.usuario)
        assertEquals("cl@ve", c.clave)
    }

    @Test
    fun sinSslmode_seFuerzaRequire() {
        assertEquals("jdbc:postgresql://host:5432/base?sslmode=require", Configuracion.convertirDatabaseUrl("postgres://u:c@host:5432/base").jdbcUrl)
    }

    @Test
    fun urlJdbc_seUsaTalCual() {
        val c = Configuracion.convertirDatabaseUrl("jdbc:postgresql://localhost/base")
        assertEquals("jdbc:postgresql://localhost/base", c.jdbcUrl)
        assertNull(c.usuario)
    }

    @Test
    fun porDefecto_esSegura() {
        val c = Configuracion.desdeEntorno(mapOf("DATABASE_URL" to "postgresql://u:c@host/base", "ADMIN_TOKEN" to "corto"))
        assertFalse(c.modoDesarrollo)
        assertFalse(c.cargarDatosDemo)
        assertNull(c.tokenAdmin, "Un ADMIN_TOKEN corto se ignora")
        assertEquals(8080, c.puerto)
        assertEquals(3, c.maxConexiones)
    }

    @Test
    fun sinDatabaseUrl_fallaConMensajeClaro() {
        assertFailsWith<IllegalStateException> { Configuracion.desdeEntorno(emptyMap()) }
    }
}
