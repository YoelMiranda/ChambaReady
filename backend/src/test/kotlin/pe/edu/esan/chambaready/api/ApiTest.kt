package pe.edu.esan.chambaready.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.AfterClass
import pe.edu.esan.chambaready.api.config.ConexionBd
import pe.edu.esan.chambaready.api.config.Configuracion
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.PreguntasFiltro
import pe.edu.esan.chambaready.api.db.RequisitosVacante
import pe.edu.esan.chambaready.api.db.SaldoEvaluacionesTabla
import pe.edu.esan.chambaready.api.dto.ActualizarPerfilDto
import pe.edu.esan.chambaready.api.dto.AgendarEntrevistaDto
import pe.edu.esan.chambaready.api.dto.CambiarEstadoDto
import pe.edu.esan.chambaready.api.dto.CrearRequisitoDto
import pe.edu.esan.chambaready.api.dto.CrearVacanteDto
import pe.edu.esan.chambaready.api.dto.DiagnosticoDto
import pe.edu.esan.chambaready.api.dto.EmpresaDto
import pe.edu.esan.chambaready.api.dto.ErrorDtoPrueba
import pe.edu.esan.chambaready.api.dto.HorasLibresDto
import pe.edu.esan.chambaready.api.dto.PostulacionDto
import pe.edu.esan.chambaready.api.dto.PostulanteDto
import pe.edu.esan.chambaready.api.dto.PostularDto
import pe.edu.esan.chambaready.api.dto.PreguntaFiltroDto
import pe.edu.esan.chambaready.api.dto.PreguntaNuevaDto
import pe.edu.esan.chambaready.api.dto.RespuestaFiltroDto
import pe.edu.esan.chambaready.api.dto.SaldoDto
import pe.edu.esan.chambaready.api.dto.SaludDto
import pe.edu.esan.chambaready.api.dto.VacanteDto
import pe.edu.esan.chambaready.api.dto.VacanteEmpresaDto
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de la API contra una PostgreSQL real (embebida): migraciones de Flyway, consultas,
 * seguridad entre usuarios y reglas de negocio. Cada prueba usa su propia base de datos vacía.
 */
class ApiTest {

    companion object {
        private val postgres: EmbeddedPostgres = EmbeddedPostgres.start()

        @JvmStatic
        @AfterClass
        fun apagar() = postgres.close()

        /** Martes 24/11/2026, 8:00 en Lima. */
        private val reloj: Clock = Clock.fixed(
            LocalDate.of(2026, 11, 24).atTime(8, 0).atZone(ZoneId.of("America/Lima")).toInstant(),
            ZoneId.of("America/Lima"),
        )
        const val TOKEN_ADMIN = "token-admin-de-prueba-123456789"
    }

    private fun baseNueva(): BaseDatos {
        val nombre = "prueba_" + UUID.randomUUID().toString().replace("-", "")
        postgres.postgresDatabase.connection.use { it.createStatement().execute("CREATE DATABASE $nombre") }
        val bd = BaseDatos(postgres.getDatabase("postgres", nombre))
        bd.migrar(conDatosDemo = false)
        return bd
    }

    private fun config(modoDesarrollo: Boolean = true) = Configuracion(
        puerto = 0,
        baseDatos = ConexionBd("", null, null),
        maxConexiones = 3,
        modoDesarrollo = modoDesarrollo,
        tokenAdmin = TOKEN_ADMIN,
        origenesCors = listOf("http://localhost:5173"),
        cargarDatosDemo = false,
    )

    /** Levanta la API con una base nueva y ejecuta [bloque] con un cliente JSON. */
    private fun conApi(modoDesarrollo: Boolean = true, bloque: suspend ApplicationTestBuilder.(HttpClient, BaseDatos) -> Unit) = testApplication {
        val bd = baseNueva()
        val cfg = config(modoDesarrollo)
        application { modulo(cfg, crearDependencias(cfg, bd, reloj)) }
        val cliente = createClient { install(ContentNegotiation) { json() } }
        bloque(cliente, bd)
    }

    // ---------- Ayudantes: solicitudes como practicante, empresa y administrador ----------

    private suspend fun HttpClient.como(uid: String, rol: String, metodo: String, ruta: String, cuerpo: Any? = null): HttpResponse {
        val configurar: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {
            header("X-Demo-Usuario", uid)
            header("X-Demo-Rol", rol)
            header("X-Demo-Nombre", "Nombre de $uid")
            if (cuerpo != null) {
                contentType(ContentType.Application.Json)
                setBody(cuerpo)
            }
        }
        return when (metodo) {
            "GET" -> get(ruta, configurar)
            "POST" -> post(ruta, configurar)
            "PUT" -> put(ruta, configurar)
            "PATCH" -> patch(ruta, configurar)
            else -> error(metodo)
        }
    }

    private val rucValido = "20131312955"
    private val otroRucValido = "20100123453"

    private fun empresaDto(ruc: String = rucValido) = EmpresaDto(
        razonSocial = "Empresa de Prueba S.A.C.", ruc = ruc, contactoCorreo = "talento@ejemplo.com",
        descripcion = "Empresa ficticia para pruebas automáticas.",
    )

    private fun vacanteDto() = CrearVacanteDto(
        puesto = "Practicante de Analítica de Datos",
        area = "DATA_ANALYTICS",
        modalidad = "HIBRIDA",
        fechaCierre = "2026-12-15",
        descripcion = "Apoyo al equipo de Inteligencia Comercial.",
        queHara = listOf("Preparar reportes con SQL."),
        requisitos = listOf(
            CrearRequisitoDto("SQL", "INTERMEDIO", "OBLIGATORIO", filtro = true),
            CrearRequisitoDto("Excel", "AVANZADO", "OBLIGATORIO"),
            CrearRequisitoDto("Power BI", "BASICO", "DESEABLE"),
        ),
        // La pregunta filtro que la empresa revisó en E03 (para el requisito 0, SQL). Correcta: 1.
        preguntasFiltro = listOf(
            PreguntaNuevaDto(0, "¿Qué cláusula filtra filas antes de agrupar?", listOf("HAVING", "WHERE", "ORDER BY", "No lo sé"), 1),
        ),
    )

    /** Empresa verificada con una vacante publicada. Devuelve la vacante. */
    private suspend fun HttpClient.empresaConVacante(uid: String = "empresa-a", ruc: String = rucValido): VacanteEmpresaDto {
        assertEquals(HttpStatusCode.OK, como(uid, "EMPRESA", "PUT", "/v1/empresa/perfil", empresaDto(ruc)).status)
        assertEquals(HttpStatusCode.NoContent, post("/v1/admin/empresas/$ruc/verificar") { header("X-Admin-Token", TOKEN_ADMIN) }.status)
        val respuesta = como(uid, "EMPRESA", "POST", "/v1/empresa/vacantes", vacanteDto())
        assertEquals(HttpStatusCode.Created, respuesta.status)
        return respuesta.body()
    }

    /** Id de la pregunta filtro que la vacante de prueba trae desde que se publicó (respuesta correcta: 1). */
    private suspend fun agregarPreguntaFiltro(bd: BaseDatos, vacanteId: String): String = bd.tx {
        val pregunta = PreguntasFiltro.selectAll().where { PreguntasFiltro.vacanteId eq vacanteId }.single()
        val requisito = RequisitosVacante.selectAll().where { RequisitosVacante.id eq pregunta[PreguntasFiltro.requisitoId] }.single()
        assertEquals("FILTRO", requisito[RequisitosVacante.tipo])
        pregunta[PreguntasFiltro.id]
    }

    // ---------- Pruebas ----------

    @Test
    fun salud_y_catalogos_sonPublicos() = conApi { cliente, _ ->
        val salud = cliente.get("/v1/salud")
        assertEquals(HttpStatusCode.OK, salud.status)
        assertEquals("ok", salud.body<SaludDto>().baseDatos)
        assertEquals(HttpStatusCode.OK, cliente.get("/v1/catalogo/areas").status)
        assertEquals(HttpStatusCode.OK, cliente.get("/v1/configuracion/precios").status)
    }

    @Test
    fun datosDemo_seCarganYSonCoherentes() = testApplication {
        val nombre = "demo_" + UUID.randomUUID().toString().replace("-", "")
        postgres.postgresDatabase.connection.use { it.createStatement().execute("CREATE DATABASE $nombre") }
        val bd = BaseDatos(postgres.getDatabase("postgres", nombre))
        bd.migrar(conDatosDemo = true)
        val cfg = config()
        application { modulo(cfg, crearDependencias(cfg, bd, reloj)) }
        val cliente = createClient { install(ContentNegotiation) { json() } }

        val vacantes = cliente.como("demo-practicante", "PRACTICANTE", "GET", "/v1/vacantes").body<List<VacanteDto>>()
        assertEquals(setOf("v1", "v4", "v5"), vacantes.map { it.id }.toSet())
        assertEquals(2, cliente.como("demo-practicante", "PRACTICANTE", "GET", "/v1/vacantes/v1/preguntas-filtro").body<List<PreguntaFiltroDto>>().size)
        assertEquals(4, cliente.como("demo-empresa", "EMPRESA", "GET", "/v1/empresa/saldo").body<SaldoDto>().gratisRestantes)
    }

    @Test
    fun sinModoDesarrollo_losEndpointsProtegidosResponden401()= conApi(modoDesarrollo = false) { cliente, _ ->
        // Aunque mande los encabezados de demo: en producción se ignoran (hasta F5, nadie entra).
        assertEquals(HttpStatusCode.Unauthorized, cliente.como("x", "PRACTICANTE", "GET", "/v1/yo/perfil").status)
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/v1/vacantes").status)
    }

    @Test
    fun unPracticanteNoEntraAEmpresa_yAdminSinTokenEs404() = conApi { cliente, _ ->
        assertEquals(HttpStatusCode.Forbidden, cliente.como("p1", "PRACTICANTE", "GET", "/v1/empresa/vacantes").status)
        assertEquals(HttpStatusCode.NotFound, cliente.get("/v1/admin/costos-ia").status)
        assertEquals(HttpStatusCode.OK, cliente.get("/v1/admin/costos-ia") { header("X-Admin-Token", TOKEN_ADMIN) }.status)
    }

    @Test
    fun empresa_rucInvalido_yPublicarExigeVerificacion() = conApi { cliente, _ ->
        assertEquals(HttpStatusCode.BadRequest, cliente.como("e", "EMPRESA", "PUT", "/v1/empresa/perfil", empresaDto("20131312956")).status)
        val respuesta = cliente.como("e", "EMPRESA", "PUT", "/v1/empresa/perfil", empresaDto())
        // El JSON debe traer el campo aunque sea el valor por defecto: la web no rellena defaults.
        assertTrue(respuesta.bodyAsText().contains("\"verificacion\":\"EN_REVISION\""))
        val perfil = respuesta.body<EmpresaDto>()
        assertEquals("EN_REVISION", perfil.verificacion)
        assertEquals(20, cliente.como("e", "EMPRESA", "GET", "/v1/empresa/saldo").body<SaldoDto>().gratisRestantes)

        val sinVerificar = cliente.como("e", "EMPRESA", "POST", "/v1/empresa/vacantes", vacanteDto())
        assertEquals(HttpStatusCode.UnprocessableEntity, sinVerificar.status)
        assertEquals("EMPRESA_NO_VERIFICADA", sinVerificar.body<ErrorDtoPrueba>().codigo)

        cliente.post("/v1/admin/empresas/$rucValido/verificar") { header("X-Admin-Token", TOKEN_ADMIN) }
        assertEquals(HttpStatusCode.Created, cliente.como("e", "EMPRESA", "POST", "/v1/empresa/vacantes", vacanteDto()).status)

        // Otra cuenta no puede registrar el mismo RUC.
        assertEquals(HttpStatusCode.Conflict, cliente.como("otra", "EMPRESA", "PUT", "/v1/empresa/perfil", empresaDto()).status)
    }

    @Test
    fun practicante_horasInvalidas_yProbabilidadSegunNiveles() = conApi { cliente, _ ->
        cliente.empresaConVacante()
        val malas = cliente.como("p1", "PRACTICANTE", "PUT", "/v1/yo/horas-libres", HorasLibresDto(mapOf("MONDAY" to 8.5)))
        assertEquals(HttpStatusCode.BadRequest, malas.status)
        assertEquals(HttpStatusCode.OK, cliente.como("p1", "PRACTICANTE", "PUT", "/v1/yo/horas-libres", HorasLibresDto(mapOf("SATURDAY" to 4.0, "SUNDAY" to 2.0))).status)

        val antes = cliente.como("p1", "PRACTICANTE", "GET", "/v1/vacantes").body<List<VacanteDto>>().single().probabilidad
        cliente.como("p1", "PRACTICANTE", "PUT", "/v1/yo/perfil", ActualizarPerfilDto(nivelesDeclarados = mapOf("sql" to "INTERMEDIO", "excel" to "AVANZADO")))
        val despues = cliente.como("p1", "PRACTICANTE", "GET", "/v1/vacantes").body<List<VacanteDto>>().single().probabilidad
        assertEquals(0, antes)
        assertEquals(83, despues) // (3 + 2) / 6 = 83 %: le falta solo el deseable
    }

    @Test
    fun diagnostico_detalleBloqueadoSinPremium() = conApi { cliente, _ ->
        val vacante = cliente.empresaConVacante()
        cliente.como("p1", "PRACTICANTE", "PUT", "/v1/yo/horas-libres", HorasLibresDto(mapOf("SATURDAY" to 4.0)))
        val d = cliente.como("p1", "PRACTICANTE", "GET", "/v1/yo/diagnosticos/${vacante.id}").body<DiagnosticoDto>()
        assertTrue(d.detalleBloqueado)
        assertEquals(3, d.brechas.size)
        assertTrue(d.brechas.all { it.curso == null }, "Sin Premium no se envían los cursos")
    }

    @Test
    fun postular_filtroNoCumplidoBloquea() = conApi { cliente, _ ->
        val vacante = cliente.empresaConVacante()
        val filtro = vacante.requisitos.first { it.tipo == "FILTRO" }
        val r = cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, mapOf(filtro.id to false), declaracionJurada = true))
        assertEquals(HttpStatusCode.UnprocessableEntity, r.status)
        assertEquals("FILTRO_NO_CUMPLIDO", r.body<ErrorDtoPrueba>().codigo)
        // Sin declaración jurada tampoco.
        assertEquals(HttpStatusCode.BadRequest, cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, declaracionJurada = false)).status)
    }

    @Test
    fun postular_conPreguntasDescuentaUnaEvaluacionYConfirmaElSello() = conApi { cliente, bd ->
        val vacante = cliente.empresaConVacante()
        val preguntaId = agregarPreguntaFiltro(bd, vacante.id)
        val preguntas = cliente.como("p1", "PRACTICANTE", "GET", "/v1/vacantes/${vacante.id}/preguntas-filtro").body<List<PreguntaFiltroDto>>()
        assertEquals(listOf(preguntaId), preguntas.map { it.id })

        val postulacion = cliente.como(
            "p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones",
            PostularDto(vacante.id, respuestasFiltro = listOf(RespuestaFiltroDto(preguntaId, 1)), declaracionJurada = true),
        ).body<PostulacionDto>()

        val filtro = vacante.requisitos.first { it.tipo == "FILTRO" }
        assertTrue(postulacion.evaluada)
        assertEquals("CONFIRMADO", postulacion.sellos[filtro.id])
        assertEquals("DECLARADO", postulacion.sellos.values.last())
        assertEquals(19, cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/saldo").body<SaldoDto>().gratisRestantes)
        assertEquals(HttpStatusCode.Conflict, cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, declaracionJurada = true)).status)
    }

    @Test
    fun sinSaldo_postulaSinEvaluar_yConPlanSeEvalua() = conApi { cliente, bd ->
        val vacante = cliente.empresaConVacante()
        val preguntaId = agregarPreguntaFiltro(bd, vacante.id)
        bd.tx { SaldoEvaluacionesTabla.update { it[gratisRestantes] = 0 } }

        assertTrue(cliente.como("p1", "PRACTICANTE", "GET", "/v1/vacantes/${vacante.id}/preguntas-filtro").body<List<PreguntaFiltroDto>>().isEmpty())
        val sinEvaluar = cliente.como(
            "p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones",
            PostularDto(vacante.id, respuestasFiltro = listOf(RespuestaFiltroDto(preguntaId, 1)), declaracionJurada = true),
        ).body<PostulacionDto>()
        assertFalse(sinEvaluar.evaluada)
        assertTrue(sinEvaluar.sellos.values.all { it == "DECLARADO" })

        cliente.post("/v1/admin/empresas/$rucValido/plan-de-prueba") { header("X-Admin-Token", TOKEN_ADMIN) }
        val conPlan = cliente.como(
            "p2", "PRACTICANTE", "POST", "/v1/yo/postulaciones",
            PostularDto(vacante.id, respuestasFiltro = listOf(RespuestaFiltroDto(preguntaId, 1)), declaracionJurada = true),
        ).body<PostulacionDto>()
        assertTrue(conPlan.evaluada)
        assertEquals(0, cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/saldo").body<SaldoDto>().gratisRestantes)
    }

    @Test
    fun conUnaEvaluacion_dosPostulacionesSimultaneas_soloUnaSeEvalua() = conApi { cliente, bd ->
        val vacante = cliente.empresaConVacante()
        val preguntaId = agregarPreguntaFiltro(bd, vacante.id)
        bd.tx { SaldoEvaluacionesTabla.update { it[gratisRestantes] = 1 } }

        val resultados = coroutineScope {
            (1..4).map { i ->
                async {
                    cliente.como(
                        "simultaneo-$i", "PRACTICANTE", "POST", "/v1/yo/postulaciones",
                        PostularDto(vacante.id, respuestasFiltro = listOf(RespuestaFiltroDto(preguntaId, 1)), declaracionJurada = true),
                    ).body<PostulacionDto>()
                }
            }.awaitAll()
        }
        assertEquals(1, resultados.count { it.evaluada }, "El saldo bloqueado (FOR UPDATE) evita que dos usen la misma evaluación")
        assertEquals(0, bd.tx { SaldoEvaluacionesTabla.selectAll().single()[SaldoEvaluacionesTabla.gratisRestantes] })
    }

    @Test
    fun unaEmpresaNoVeLosPostulantesDeOtra() = conApi { cliente, _ ->
        val vacanteA = cliente.empresaConVacante("empresa-a", rucValido)
        cliente.empresaConVacante("empresa-b", otroRucValido)
        cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacanteA.id, declaracionJurada = true))

        assertEquals(1, cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/vacantes/${vacanteA.id}/postulantes").body<List<PostulanteDto>>().size)
        assertEquals(HttpStatusCode.NotFound, cliente.como("empresa-b", "EMPRESA", "GET", "/v1/empresa/vacantes/${vacanteA.id}/postulantes").status)
    }

    @Test
    fun descartarExigeMotivo_yElPracticanteLoVe() = conApi { cliente, _ ->
        val vacante = cliente.empresaConVacante()
        val postulacion = cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, declaracionJurada = true)).body<PostulacionDto>()

        val sinMotivo = cliente.como("empresa-a", "EMPRESA", "PATCH", "/v1/empresa/postulaciones/${postulacion.id}", CambiarEstadoDto("DESCARTADA", "corto"))
        assertEquals(HttpStatusCode.UnprocessableEntity, sinMotivo.status)
        val conMotivo = cliente.como("empresa-a", "EMPRESA", "PATCH", "/v1/empresa/postulaciones/${postulacion.id}", CambiarEstadoDto("DESCARTADA", "Buscábamos SQL intermedio confirmado."))
        assertEquals("DESCARTADA", conMotivo.body<PostulanteDto>().estado)

        val vistaPracticante = cliente.como("p1", "PRACTICANTE", "GET", "/v1/yo/postulaciones").body<List<PostulacionDto>>().single()
        assertEquals("Buscábamos SQL intermedio confirmado.", vistaPracticante.motivoDescarte)
        // Otra empresa no puede cambiar esa postulación.
        cliente.empresaConVacante("empresa-b", otroRucValido)
        assertEquals(HttpStatusCode.NotFound, cliente.como("empresa-b", "EMPRESA", "PATCH", "/v1/empresa/postulaciones/${postulacion.id}", CambiarEstadoDto("REVISION")).status)
    }

    @Test
    fun vistasDeLaWeb_entrevistasActividadYEnviarPreguntas() = conApi { cliente, _ ->
        val vacante = cliente.empresaConVacante()
        val p1 = cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, declaracionJurada = true)).body<PostulacionDto>()
        cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/entrevistas", AgendarEntrevistaDto(p1.id, "2026-11-25", "10:00", 45, "VIDEOLLAMADA"))

        val entrevistas = cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/entrevistas").body<List<pe.edu.esan.chambaready.api.dto.EntrevistaEmpresaDto>>()
        assertEquals("Nombre de p1", entrevistas.single().nombrePostulante)
        assertEquals("10:00", entrevistas.single().hora)
        assertEquals(1, cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/actividad").body<List<pe.edu.esan.chambaready.api.dto.ActividadDto>>().size)
        assertEquals("ENTREVISTA", cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/postulaciones/${p1.id}").body<PostulanteDto>().estado)

        // Regla 13: enviar preguntas a un "Sin evaluar" exige el Plan Empresa.
        val sinPlan = cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/postulaciones/${p1.id}/enviar-preguntas")
        assertEquals(HttpStatusCode.UnprocessableEntity, sinPlan.status)
        cliente.post("/v1/admin/empresas/$rucValido/plan-de-prueba") { header("X-Admin-Token", TOKEN_ADMIN) }
        assertEquals(HttpStatusCode.NoContent, cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/postulaciones/${p1.id}/enviar-preguntas").status)

        // Otra empresa no ve nada de esto.
        cliente.empresaConVacante("empresa-b", otroRucValido)
        assertEquals(HttpStatusCode.NotFound, cliente.como("empresa-b", "EMPRESA", "GET", "/v1/empresa/postulaciones/${p1.id}").status)
        assertTrue(cliente.como("empresa-b", "EMPRESA", "GET", "/v1/empresa/entrevistas").body<List<pe.edu.esan.chambaready.api.dto.EntrevistaEmpresaDto>>().isEmpty())
    }

    @Test
    fun entrevistas_domingoYCrucesSeRechazan()= conApi { cliente, _ ->
        val vacante = cliente.empresaConVacante()
        val p1 = cliente.como("p1", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, declaracionJurada = true)).body<PostulacionDto>()
        val p2 = cliente.como("p2", "PRACTICANTE", "POST", "/v1/yo/postulaciones", PostularDto(vacante.id, declaracionJurada = true)).body<PostulacionDto>()

        val domingo = cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/entrevistas", AgendarEntrevistaDto(p1.id, "2026-11-29", "10:00", 30, "VIDEOLLAMADA"))
        assertEquals(HttpStatusCode.UnprocessableEntity, domingo.status)
        val almuerzo = cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/entrevistas", AgendarEntrevistaDto(p1.id, "2026-11-25", "12:30", 60, "VIDEOLLAMADA"))
        assertEquals(HttpStatusCode.UnprocessableEntity, almuerzo.status)

        assertEquals(HttpStatusCode.Created, cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/entrevistas", AgendarEntrevistaDto(p1.id, "2026-11-25", "10:00", 45, "VIDEOLLAMADA")).status)
        val cruce = cliente.como("empresa-a", "EMPRESA", "POST", "/v1/empresa/entrevistas", AgendarEntrevistaDto(p2.id, "2026-11-25", "10:30", 30, "PRESENCIAL"))
        assertEquals(HttpStatusCode.Conflict, cruce.status)

        val postulantes = cliente.como("empresa-a", "EMPRESA", "GET", "/v1/empresa/vacantes/${vacante.id}/postulantes").body<List<PostulanteDto>>()
        assertEquals("ENTREVISTA", postulantes.first { it.id == p1.id }.estado)
        assertNull(postulantes.first { it.id == p1.id }.motivoDescarte)
    }
}
