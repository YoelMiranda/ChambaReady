package pe.edu.esan.chambaready.core.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import pe.edu.esan.chambaready.core.data.remoto.ApiChambaReady
import pe.edu.esan.chambaready.core.data.remoto.CuentaRemotaRepository
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.remoto.InterceptorToken
import pe.edu.esan.chambaready.core.data.vacantes.RemotoVacanteRepository
import pe.edu.esan.chambaready.core.domain.reglas.DestinoTrasLogin
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Prueba el cliente HTTP contra un servidor falso (MockWebServer): que mande el token, que lea el
 * JSON del backend y que convierta los errores en mensajes en español.
 */
class ApiRemotaTest {

    private val servidor = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private var token: String? = "token-de-prueba"
    private lateinit var api: ApiChambaReady

    @Before
    fun preparar() {
        servidor.start()
        val cliente = OkHttpClient.Builder().addInterceptor(InterceptorToken { token }).build()
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/v1/"))
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiChambaReady::class.java)
    }

    @After
    fun cerrar() = servidor.close()

    private fun responder(codigo: Int, cuerpo: String) =
        servidor.enqueue(MockResponse.Builder().code(codigo).addHeader("Content-Type", "application/json").body(cuerpo).build())

    @Test
    fun `cada pedido lleva el token de Firebase`() = runTest {
        responder(200, """{"registrado":true,"rol":"PRACTICANTE","campoNuevo":1}""")
        val destino = CuentaRemotaRepository(api, json).destinoTrasLogin()
        val pedido = servidor.takeRequest()
        assertEquals("Bearer token-de-prueba", pedido.headers["Authorization"])
        assertEquals("/v1/sesion", pedido.url.encodedPath)
        assertEquals(DestinoTrasLogin.ENTRAR, destino)
    }

    @Test
    fun `sin sesion no se manda el encabezado`() = runTest {
        token = null
        responder(200, """{"registrado":false}""")
        assertEquals(DestinoTrasLogin.COMPLETAR_REGISTRO, CuentaRemotaRepository(api, json).destinoTrasLogin())
        assertNull(servidor.takeRequest().headers["Authorization"])
    }

    @Test
    fun `una cuenta de empresa no entra a la app`() = runTest {
        responder(200, """{"registrado":true,"rol":"EMPRESA"}""")
        assertEquals(DestinoTrasLogin.USAR_WEB_EMPRESA, CuentaRemotaRepository(api, json).destinoTrasLogin())
    }

    @Test
    fun `el error del backend llega con su mensaje`() = runTest {
        responder(409, """{"codigo":"CONFLICTO","mensaje":"Ya postulaste a esta vacante."}""")
        try {
            RemotoVacanteRepository(api, json).listarVacantes()
            fail("Debió lanzar ErrorRemoto")
        } catch (e: ErrorRemoto) {
            assertEquals("CONFLICTO", e.codigo)
            assertEquals(409, e.estadoHttp)
            assertEquals("Ya postulaste a esta vacante.", e.message)
        }
    }

    @Test
    fun `un 401 sin cuerpo pide volver a iniciar sesion`() = runTest {
        responder(401, "")
        try {
            RemotoVacanteRepository(api, json).listarVacantes()
            fail("Debió lanzar ErrorRemoto")
        } catch (e: ErrorRemoto) {
            assertTrue(e.message!!.contains("iniciar sesión"))
        }
    }

    @Test
    fun `registrarse dos veces como practicante no es un error`() = runTest {
        responder(409, """{"codigo":"CONFLICTO","mensaje":"Esta cuenta ya está registrada."}""")
        responder(200, """{"registrado":true,"rol":"PRACTICANTE"}""")
        CuentaRemotaRepository(api, json).registrarPracticante("Ana")
        assertEquals("""{"nombre":"Ana","rol":"PRACTICANTE"}""", servidor.takeRequest().body?.utf8())
    }

    @Test
    fun `las vacantes se leen y el detalle sale de la lista sin otro pedido`() = runTest {
        responder(
            200,
            """[{"id":"v1","puesto":"Analista","empresa":"Andes","area":"FINANZAS","modalidad":"REMOTA","sueldo":null,
               "fechaCierre":"2026-12-15","diasRestantes":21,"descripcion":"d","queHara":["a"],
               "requisitos":[{"id":"r1","habilidadId":"excel","habilidad":"Excel","nivel":"BASICO","tipo":"OBLIGATORIO"}],
               "evaluacionActiva":true,"probabilidad":62}]""",
        )
        val repositorio = RemotoVacanteRepository(api, json)
        val lista = repositorio.listarVacantes()
        assertEquals(62, lista.single().resumen.probabilidad)
        assertEquals("Andes", repositorio.obtenerVacante("v1")?.resumen?.empresa)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun `una vacante inexistente es null, no un error`() = runTest {
        responder(404, """{"codigo":"NO_ENCONTRADO","mensaje":"Esta vacante no existe."}""")
        assertNull(RemotoVacanteRepository(api, json).obtenerVacante("nada"))
    }

    @Test
    fun `la evaluacion trae un mini-test por habilidad sin respuestas`() = runTest {
        responder(
            200,
            """{"id":"ev1","pruebas":[{"id":"mt1","habilidadId":"sql","habilidad":"SQL","nivel":"INTERMEDIO",
               "preguntas":[{"enunciado":"P1","opciones":["a","b","c","d"]}]}]}""",
        )
        val pruebas = CuentaRemotaRepository(api, json).iniciarEvaluacion(listOf("sql"))
        assertEquals("""{"habilidades":["sql"]}""", servidor.takeRequest().body?.utf8())
        val prueba = pruebas.single()
        assertEquals(pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad.INTERMEDIO, prueba.nivel)
        assertEquals(pe.edu.esan.chambaready.core.data.remoto.SIN_RESPUESTA, prueba.preguntas.single().indiceCorrecto)
    }

    @Test
    fun `el asistente devuelve las habilidades que entendio`() = runTest {
        responder(200, """{"respuesta":"Anotado","habilidades":[{"habilidadId":"excel","nombre":"Excel","nivel":"AVANZADO"}],"conIa":true}""")
        val r = CuentaRemotaRepository(api, json).conversarAsistente("Sé Excel avanzado", emptyMap())
        assertEquals("Anotado", r.respuesta)
        assertEquals(mapOf("excel" to pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad.AVANZADO), r.habilidades)
    }
}
