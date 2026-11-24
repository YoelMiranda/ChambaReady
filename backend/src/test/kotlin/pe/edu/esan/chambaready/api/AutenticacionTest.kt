package pe.edu.esan.chambaready.api

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.junit.AfterClass
import pe.edu.esan.chambaready.api.auth.Identidad
import pe.edu.esan.chambaready.api.auth.VerificadorToken
import pe.edu.esan.chambaready.api.config.ConexionBd
import pe.edu.esan.chambaready.api.config.Configuracion
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.dto.ErrorDtoPrueba
import pe.edu.esan.chambaready.api.dto.RegistroDto
import pe.edu.esan.chambaready.api.dto.SesionDto
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Login con Firebase (F5), en modo producción (sin MODO_DESARROLLO). Un verificador falso reemplaza a
 * Firebase Admin SDK: así se prueba todo el flujo sin un proyecto real.
 */
class AutenticacionTest {

    companion object {
        private val postgres: EmbeddedPostgres = EmbeddedPostgres.start()

        @JvmStatic
        @AfterClass
        fun apagar() = postgres.close()
    }

    /** Solo estos "tokens" son válidos; cualquier otro lanza error, como Firebase con un token falso. */
    private val verificadorFalso = VerificadorToken { token ->
        when (token) {
            "token-ana" -> Identidad("uid-ana", nombre = "Ana", correo = "ana@ejemplo.com")
            "token-empresa" -> Identidad("uid-empresa", nombre = "Talento", correo = "talento@ejemplo.com")
            else -> error("token inválido")
        }
    }

    private fun conApiProduccion(bloque: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val nombre = "auth_" + UUID.randomUUID().toString().replace("-", "")
        postgres.postgresDatabase.connection.use { it.createStatement().execute("CREATE DATABASE $nombre") }
        val bd = BaseDatos(postgres.getDatabase("postgres", nombre)).also { it.migrar(conDatosDemo = false) }
        val cfg = Configuracion(0, ConexionBd("", null, null), 3, modoDesarrollo = false, tokenAdmin = null, origenesCors = emptyList(), cargarDatosDemo = false)
        application { modulo(cfg, crearDependencias(cfg, bd, verificador = verificadorFalso)) }
        bloque(createClient { install(ContentNegotiation) { json() } })
    }

    @Test
    fun sinTokenOTokenFalso_es401() = conApiProduccion { cliente ->
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/v1/yo/perfil").status)
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/v1/yo/perfil") { bearerAuth("token-robado") }.status)
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/v1/yo/perfil") { header("Authorization", "Basic abc") }.status)
    }

    @Test
    fun encabezadosDeDemo_seIgnoranEnProduccion() = conApiProduccion { cliente ->
        val r = cliente.get("/v1/yo/perfil") {
            header("X-Demo-Usuario", "intruso")
            header("X-Demo-Rol", "PRACTICANTE")
        }
        assertEquals(HttpStatusCode.Unauthorized, r.status)
    }

    @Test
    fun tokenValidoSinRegistro_pideRegistrarse_yLuegoEntra() = conApiProduccion { cliente ->
        assertFalse(cliente.get("/v1/sesion") { bearerAuth("token-ana") }.body<SesionDto>().registrado)
        val antes = cliente.get("/v1/yo/perfil") { bearerAuth("token-ana") }
        assertEquals(HttpStatusCode.Forbidden, antes.status)
        assertEquals("REGISTRO_PENDIENTE", antes.body<ErrorDtoPrueba>().codigo)

        val registro = cliente.post("/v1/registro") {
            bearerAuth("token-ana")
            contentType(ContentType.Application.Json)
            setBody(RegistroDto("PRACTICANTE"))
        }
        assertEquals(HttpStatusCode.Created, registro.status)
        assertEquals("PRACTICANTE", cliente.get("/v1/sesion") { bearerAuth("token-ana") }.body<SesionDto>().rol)
        assertEquals(HttpStatusCode.OK, cliente.get("/v1/yo/perfil") { bearerAuth("token-ana") }.status)
    }

    @Test
    fun elRolNoSePuedeCambiar_niUsarElDelOtro() = conApiProduccion { cliente ->
        suspend fun registrar(token: String, rol: String) = cliente.post("/v1/registro") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RegistroDto(rol))
        }
        assertEquals(HttpStatusCode.Created, registrar("token-ana", "PRACTICANTE").status)
        assertEquals(HttpStatusCode.Created, registrar("token-ana", "PRACTICANTE").status) // idempotente
        assertEquals(HttpStatusCode.Conflict, registrar("token-ana", "EMPRESA").status)
        assertEquals(HttpStatusCode.Forbidden, cliente.get("/v1/empresa/vacantes") { bearerAuth("token-ana") }.status)
        assertEquals(HttpStatusCode.BadRequest, registrar("token-empresa", "ADMIN").status)
    }
}
