package pe.edu.esan.chambaready.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.AfterClass
import pe.edu.esan.chambaready.api.config.ConexionBd
import pe.edu.esan.chambaready.api.config.Configuracion
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Evidencias
import pe.edu.esan.chambaready.api.db.SuscripcionesEmpresa
import pe.edu.esan.chambaready.api.db.UsoIa
import pe.edu.esan.chambaready.api.dto.AnalizarCvDto
import pe.edu.esan.chambaready.api.dto.ArchivoDto
import pe.edu.esan.chambaready.api.dto.AutoevaluacionDto
import pe.edu.esan.chambaready.api.dto.CostoIaDto
import pe.edu.esan.chambaready.api.dto.CrearEvidenciaDto
import pe.edu.esan.chambaready.api.dto.CrearRequisitoDto
import pe.edu.esan.chambaready.api.dto.CrearVacanteDto
import pe.edu.esan.chambaready.api.dto.CvAnalizadoDto
import pe.edu.esan.chambaready.api.dto.DiagnosticoDto
import pe.edu.esan.chambaready.api.dto.EmpresaDto
import pe.edu.esan.chambaready.api.dto.EstadoPlanDto
import pe.edu.esan.chambaready.api.dto.EvidenciaDto
import pe.edu.esan.chambaready.api.dto.LogoDto
import pe.edu.esan.chambaready.api.dto.MetricasDto
import pe.edu.esan.chambaready.api.dto.MiniTestDto
import pe.edu.esan.chambaready.api.dto.NotificacionDto
import pe.edu.esan.chambaready.api.dto.PerfilPracticanteDto
import pe.edu.esan.chambaready.api.dto.PostulacionDto
import pe.edu.esan.chambaready.api.dto.PostulanteDto
import pe.edu.esan.chambaready.api.dto.PostularDto
import pe.edu.esan.chambaready.api.dto.PreguntaFiltroDto
import pe.edu.esan.chambaready.api.dto.PreguntaFiltroEmpresaDto
import pe.edu.esan.chambaready.api.dto.PreguntaNuevaDto
import pe.edu.esan.chambaready.api.dto.PreguntasSugeridasDto
import pe.edu.esan.chambaready.api.dto.RankingDto
import pe.edu.esan.chambaready.api.dto.ResponderFiltroDto
import pe.edu.esan.chambaready.api.dto.ResponderMiniTestDto
import pe.edu.esan.chambaready.api.dto.RespuestaFiltroDto
import pe.edu.esan.chambaready.api.dto.ResultadoMiniTestDto
import pe.edu.esan.chambaready.api.dto.ResumenNotificacionesDto
import pe.edu.esan.chambaready.api.dto.SaldoDto
import pe.edu.esan.chambaready.api.dto.SugerirPreguntasDto
import pe.edu.esan.chambaready.api.dto.VacanteEmpresaDto
import pe.edu.esan.chambaready.api.ia.ErrorIa
import pe.edu.esan.chambaready.api.ia.ProveedorIa
import pe.edu.esan.chambaready.api.ia.RespuestaIa
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * F6 a F9 contra una PostgreSQL embebida: IA (con un proveedor falso, sin red), archivos guardados
 * en la base, pagos simulados, regla 13 y notificaciones.
 */
class Fases6a9Test {

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
        const val RUC = "20131312955"

        /** 3 preguntas cuya respuesta correcta es siempre la opción 0. */
        val PREGUNTAS_IA = """
            {"preguntas": [
              {"enunciado": "Pregunta generada número uno", "opciones": ["bien", "mal 1", "mal 2", "mal 3"], "indiceCorrecto": 0},
              {"enunciado": "Pregunta generada número dos", "opciones": ["bien", "mal 1", "mal 2", "mal 3"], "indiceCorrecto": 0},
              {"enunciado": "Pregunta generada número tres", "opciones": ["bien", "mal 1", "mal 2", "mal 3"], "indiceCorrecto": 0}
            ]}
        """.trimIndent()

        val PDF = "%PDF-1.4 CV de prueba".toByteArray()
    }

    /** IA falsa: responde preguntas o un CV según la operación, y cuenta las llamadas. */
    private class IaFalsa(private val falla: Boolean = false) : ProveedorIa {
        var llamadas = 0
        override suspend fun generar(pedido: pe.edu.esan.chambaready.api.ia.PedidoIa): RespuestaIa {
            llamadas++
            if (falla) throw ErrorIa("Sin conexión (simulado)")
            val json = when (pedido.operacion) {
                "LEER_CV" -> """{"habilidades": [{"habilidad": "SQL", "nivel": "INTERMEDIO"}, {"habilidad": "Brujería", "nivel": "AVANZADO"}], "proyectos": ["Tablero de ventas"]}"""
                "ASISTENTE" -> """{"respuesta": "Anotado: Excel avanzado.", "habilidades": [{"habilidad": "Excel", "nivel": "AVANZADO"}, {"habilidad": "Cocina", "nivel": "AVANZADO"}]}"""
                else -> PREGUNTAS_IA
            }
            return RespuestaIa(json, tokensEntrada = 1000, tokensSalida = 200)
        }
    }

    private fun baseNueva(): BaseDatos {
        val nombre = "fases_" + UUID.randomUUID().toString().replace("-", "")
        postgres.postgresDatabase.connection.use { it.createStatement().execute("CREATE DATABASE $nombre") }
        val bd = BaseDatos(postgres.getDatabase("postgres", nombre))
        bd.migrar(conDatosDemo = false)
        return bd
    }

    private fun conApi(ia: ProveedorIa? = null, bloque: suspend ApplicationTestBuilder.(HttpClient, BaseDatos) -> Unit) = testApplication {
        val bd = baseNueva()
        val cfg = Configuracion(
            puerto = 0, baseDatos = ConexionBd("", null, null), maxConexiones = 3, modoDesarrollo = true,
            tokenAdmin = TOKEN_ADMIN, origenesCors = emptyList(), cargarDatosDemo = false,
        )
        application { modulo(cfg, crearDependencias(cfg, bd, reloj, proveedorIa = ia)) }
        bloque(createClient { install(ContentNegotiation) { json() } }, bd)
    }

    // ---------- Ayudantes ----------

    private fun io.ktor.client.request.HttpRequestBuilder.como(uid: String, rol: String) {
        header("X-Demo-Usuario", uid)
        header("X-Demo-Rol", rol)
        header("X-Demo-Nombre", "Nombre de $uid")
    }

    private suspend fun HttpClient.getComo(uid: String, rol: String, ruta: String) = get(ruta) { como(uid, rol) }

    private suspend fun HttpClient.postComo(uid: String, rol: String, ruta: String, cuerpo: Any? = null) = post(ruta) {
        como(uid, rol)
        if (cuerpo != null) {
            contentType(ContentType.Application.Json)
            setBody(cuerpo)
        }
    }

    private suspend fun HttpClient.subir(uid: String, rol: String, tipo: String, mime: String, bytes: ByteArray): HttpResponse =
        post("/v1/archivos?tipo=$tipo") {
            como(uid, rol)
            header("X-Nombre-Archivo", "mi%20cv.pdf")
            contentType(ContentType.parse(mime))
            setBody(bytes)
        }

    private suspend fun HttpClient.admin(ruta: String) = post(ruta) { header("X-Admin-Token", TOKEN_ADMIN) }

    private fun vacante(preguntas: List<PreguntaNuevaDto> = emptyList(), evalua: Boolean = true) = CrearVacanteDto(
        puesto = "Practicante de Analítica de Datos", area = "DATA_ANALYTICS", modalidad = "HIBRIDA", fechaCierre = "2026-12-15",
        descripcion = "Apoyo al equipo de Inteligencia Comercial.", queHara = listOf("Reportes con SQL."),
        requisitos = listOf(
            CrearRequisitoDto("Power BI", "BASICO", "DESEABLE"),
            CrearRequisitoDto("SQL", "INTERMEDIO", "OBLIGATORIO", filtro = true),
            CrearRequisitoDto("Excel", "AVANZADO", "OBLIGATORIO"),
        ),
        evaluacionActiva = evalua,
        preguntasFiltro = preguntas,
    )

    /** Empresa verificada con una vacante publicada. */
    private suspend fun HttpClient.empresaConVacante(datos: CrearVacanteDto = vacante()): VacanteEmpresaDto {
        put("/v1/empresa/perfil") {
            como("empresa", "EMPRESA")
            contentType(ContentType.Application.Json)
            setBody(EmpresaDto("Empresa de Prueba S.A.C.", RUC, contactoCorreo = "talento@ejemplo.com", descripcion = "Empresa ficticia para pruebas automáticas."))
        }
        admin("/v1/admin/empresas/$RUC/verificar")
        val respuesta = postComo("empresa", "EMPRESA", "/v1/empresa/vacantes", datos)
        assertEquals(HttpStatusCode.Created, respuesta.status, respuesta.bodyAsText())
        return respuesta.body()
    }

    // ---------- F6: IA ----------

    @Test
    fun vacanteSinPreguntas_seGeneranSimuladas_yElPracticanteNoVeLaRespuesta() = conApi { cliente, _ ->
        val v = cliente.empresaConVacante()
        val deEmpresa = cliente.getComo("empresa", "EMPRESA", "/v1/empresa/vacantes/${v.id}/preguntas-filtro").body<List<PreguntaFiltroEmpresaDto>>()
        assertEquals(2, deEmpresa.size)
        // Primero el requisito filtro (SQL), luego el obligatorio (Excel).
        val sql = v.requisitos.first { it.habilidad == "SQL" }
        assertEquals(sql.id, deEmpresa[0].requisitoId)
        val textoPracticante = cliente.getComo("p1", "PRACTICANTE", "/v1/vacantes/${v.id}/preguntas-filtro").bodyAsText()
        assertFalse(textoPracticante.contains("indiceCorrecto"), "El practicante nunca recibe la respuesta")
        assertEquals(2, cliente.getComo("p1", "PRACTICANTE", "/v1/vacantes/${v.id}/preguntas-filtro").body<List<PreguntaFiltroDto>>().size)
        assertTrue(cliente.getComo("p1", "PRACTICANTE", "/v1/vacantes/${v.id}").body<pe.edu.esan.chambaready.api.dto.VacanteDto>().conPreguntasFiltro)
    }

    @Test
    fun sugerirPreguntas_conIa_registraElCosto() {
        val ia = IaFalsa()
        conApi(ia) { cliente, _ ->
            cliente.empresaConVacante(vacante(evalua = false))
            val sugeridas = cliente.postComo(
                "empresa", "EMPRESA", "/v1/empresa/preguntas-filtro/sugerir",
                SugerirPreguntasDto("Analista", vacante().requisitos),
            ).body<PreguntasSugeridasDto>()
            assertTrue(sugeridas.conIa)
            assertEquals(listOf(1, 2), sugeridas.preguntas.map { it.requisitoIndice })
            val costos = cliente.get("/v1/admin/costos-ia") { header("X-Admin-Token", TOKEN_ADMIN) }.body<List<CostoIaDto>>()
            assertEquals("PREGUNTAS_FILTRO", costos.single().operacion)
            assertEquals(1, ia.llamadas)
        }
    }

    @Test
    fun siLaIaFalla_seUsaElContenidoSimulado() = conApi(IaFalsa(falla = true)) { cliente, bd ->
        val v = cliente.empresaConVacante()
        assertEquals(2, cliente.getComo("empresa", "EMPRESA", "/v1/empresa/vacantes/${v.id}/preguntas-filtro").body<List<PreguntaFiltroEmpresaDto>>().size)
        assertEquals(0L, bd.tx { UsoIa.selectAll().count() }, "Una llamada fallida no se cobra")
    }

    @Test
    fun preguntasPropiasInvalidas_seRechazan() = conApi { cliente, _ ->
        val mala = PreguntaNuevaDto(0, "¿Pregunta con opciones repetidas?", listOf("a", "a", "b", "c"), 0)
        cliente.put("/v1/empresa/perfil") {
            como("empresa", "EMPRESA")
            contentType(ContentType.Application.Json)
            setBody(EmpresaDto("Empresa de Prueba S.A.C.", RUC, descripcion = "Empresa ficticia para pruebas automáticas."))
        }
        cliente.admin("/v1/admin/empresas/$RUC/verificar")
        assertEquals(HttpStatusCode.BadRequest, cliente.postComo("empresa", "EMPRESA", "/v1/empresa/vacantes", vacante(listOf(mala))).status)
        val fueraDeRango = PreguntaNuevaDto(9, "¿Requisito que no existe en la lista?", listOf("a", "b", "c", "d"), 0)
        assertEquals(HttpStatusCode.BadRequest, cliente.postComo("empresa", "EMPRESA", "/v1/empresa/vacantes", vacante(listOf(fueraDeRango))).status)
    }

    @Test
    fun miniTest_seCorrigeEnElServidor_ySoloAprobarSubeElNivel() = conApi(IaFalsa()) { cliente, _ ->
        val test = cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/sql").body<MiniTestDto>()
        assertEquals("BASICO", test.nivel)
        assertEquals(3, test.preguntas.size)
        assertFalse(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/sql").bodyAsText().contains("indiceCorrecto"))

        val reprobado = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/${test.id}/respuestas", ResponderMiniTestDto(listOf(0, 1, null))).body<ResultadoMiniTestDto>()
        assertFalse(reprobado.aprobado)
        assertEquals("NO_LO_HE_USADO", reprobado.nivelDespues)

        val aprobado = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/${test.id}/respuestas", ResponderMiniTestDto(listOf(0, 0, 3))).body<ResultadoMiniTestDto>()
        assertTrue(aprobado.aprobado)
        assertEquals("BASICO", aprobado.nivelDespues)
        assertEquals(listOf(0, 0, 0), aprobado.indicesCorrectos)
        assertEquals("BASICO", cliente.getComo("p1", "PRACTICANTE", "/v1/yo/perfil").body<PerfilPracticanteDto>().nivelesDemostrados["sql"])
        // El siguiente mini-test ya es de nivel intermedio.
        assertEquals("INTERMEDIO", cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/sql").body<MiniTestDto>().nivel)
    }

    @Test
    fun miniTest_preguntaDelNivelDeclarado() = conApi { cliente, _ ->
        cliente.put("/v1/yo/perfil") {
            como("p1", "PRACTICANTE")
            contentType(ContentType.Application.Json)
            setBody(pe.edu.esan.chambaready.api.dto.ActualizarPerfilDto(nivelesDeclarados = mapOf("sql" to "INTERMEDIO")))
        }
        assertEquals("INTERMEDIO", cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/sql").body<MiniTestDto>().nivel)
    }

    @Test
    fun miniTest_seGeneraUnaSolaVezPorNivel() {
        val ia = IaFalsa()
        conApi(ia) { cliente, _ ->
            cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/excel")
            cliente.getComo("p2", "PRACTICANTE", "/v1/yo/mini-tests/excel")
            assertEquals(1, ia.llamadas, "El segundo estudiante reutiliza el mini-test guardado")
        }
    }

    @Test
    fun ranking_esPrivadoYDentroDelArea() = conApi { cliente, _ ->
        val solo = cliente.postComo("a", "PRACTICANTE", "/v1/yo/autoevaluacion", AutoevaluacionDto("DATA_ANALYTICS", 3, 3)).body<RankingDto>()
        assertEquals(100, solo.topPorcentaje)
        cliente.postComo("b", "PRACTICANTE", "/v1/yo/autoevaluacion", AutoevaluacionDto("DATA_ANALYTICS", 1, 3))
        cliente.postComo("c", "PRACTICANTE", "/v1/yo/autoevaluacion", AutoevaluacionDto("FINANZAS", 3, 3))
        val otraVez = cliente.postComo("a", "PRACTICANTE", "/v1/yo/autoevaluacion", AutoevaluacionDto("DATA_ANALYTICS", 3, 3)).body<RankingDto>()
        assertEquals(50, otraVez.topPorcentaje, "1.º de 2 en su área (el de Finanzas no cuenta)")
        assertEquals(100, otraVez.topAnterior)
        assertEquals(2, otraVez.estudiantesEnArea)
        assertEquals(HttpStatusCode.Forbidden, cliente.postComo("empresa", "EMPRESA", "/v1/yo/autoevaluacion", AutoevaluacionDto("DATA_ANALYTICS", 3, 3)).status)
    }

    // ---------- F7: archivos ----------

    @Test
    fun cv_seSubeSeAnalizaYSoloLoVeLaEmpresaALaQuePostulo() = conApi(IaFalsa()) { cliente, _ ->
        val v = cliente.empresaConVacante()
        val subida = cliente.subir("p1", "PRACTICANTE", "CV", "application/pdf", PDF)
        assertEquals(HttpStatusCode.Created, subida.status, subida.bodyAsText())
        val archivo = subida.body<ArchivoDto>()
        assertEquals("mi cv.pdf", archivo.nombre)

        val cv = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/cv", AnalizarCvDto(archivo.id)).body<CvAnalizadoDto>()
        assertEquals(listOf("sql"), cv.habilidades.map { it.habilidadId }, "La habilidad inventada se descarta")
        assertEquals(archivo.id, cliente.getComo("p1", "PRACTICANTE", "/v1/yo/perfil").body<PerfilPracticanteDto>().cvArchivoId)

        assertEquals(HttpStatusCode.NotFound, cliente.getComo("empresa", "EMPRESA", "/v1/archivos/${archivo.id}").status, "Aún no postuló")
        assertEquals(HttpStatusCode.NotFound, cliente.getComo("p2", "PRACTICANTE", "/v1/archivos/${archivo.id}").status)
        cliente.postComo("p1", "PRACTICANTE", "/v1/yo/postulaciones", PostularDto(v.id, declaracionJurada = true))
        val postulante = cliente.getComo("empresa", "EMPRESA", "/v1/empresa/vacantes/${v.id}/postulantes").body<List<PostulanteDto>>().single()
        assertEquals(archivo.id, postulante.cvArchivoId)
        val descarga = cliente.getComo("empresa", "EMPRESA", "/v1/archivos/${archivo.id}")
        assertEquals(HttpStatusCode.OK, descarga.status)
        assertTrue(descarga.readRawBytes().contentEquals(PDF))
    }

    @Test
    fun archivos_seValidanTipoRolYContenido() = conApi { cliente, _ ->
        assertEquals(HttpStatusCode.BadRequest, cliente.subir("p1", "PRACTICANTE", "CV", "application/pdf", "MZ ejecutable".toByteArray()).status)
        assertEquals(HttpStatusCode.BadRequest, cliente.subir("p1", "PRACTICANTE", "LOGO", "image/png", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)).status)
        assertEquals(HttpStatusCode.BadRequest, cliente.subir("p1", "PRACTICANTE", "OTRO", "application/pdf", PDF).status)
        // Un CV ajeno no se puede asignar al propio perfil.
        val ajeno = cliente.subir("p1", "PRACTICANTE", "CV", "application/pdf", PDF).body<ArchivoDto>()
        assertEquals(HttpStatusCode.BadRequest, cliente.postComo("p2", "PRACTICANTE", "/v1/yo/cv", AnalizarCvDto(ajeno.id)).status)
    }

    @Test
    fun logo_yEvidencia_conValidacionDelEquipo() = conApi { cliente, bd ->
        cliente.empresaConVacante()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val logo = cliente.subir("empresa", "EMPRESA", "LOGO", "image/png", png).body<ArchivoDto>()
        val empresa = cliente.put("/v1/empresa/logo") {
            como("empresa", "EMPRESA")
            contentType(ContentType.Application.Json)
            setBody(LogoDto(logo.id))
        }.body<EmpresaDto>()
        assertEquals(logo.id, empresa.logoArchivoId)
        assertEquals(HttpStatusCode.OK, cliente.getComo("p1", "PRACTICANTE", "/v1/archivos/${logo.id}").status, "Los logos son públicos")

        val certificado = cliente.subir("p1", "PRACTICANTE", "EVIDENCIA", "application/pdf", PDF).body<ArchivoDto>()
        val evidencia = cliente.postComo(
            "p1", "PRACTICANTE", "/v1/yo/evidencias",
            CrearEvidenciaDto("powerbi", "Certificado de Power BI", "CERTIFICADO", "INTERMEDIO", archivoId = certificado.id),
        ).body<EvidenciaDto>()
        assertEquals("EN_REVISION", evidencia.estado)
        assertNull(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/perfil").body<PerfilPracticanteDto>().nivelesDemostrados["powerbi"], "En revisión no sube el nivel")
        assertEquals(HttpStatusCode.BadRequest, cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evidencias", CrearEvidenciaDto("sql", "Mi repo", "LINK", "BASICO", url = "no-es-link")).status)

        cliente.admin("/v1/admin/evidencias/${evidencia.id}/validar")
        assertEquals("VALIDADA", bd.tx { Evidencias.selectAll().where { Evidencias.id eq evidencia.id }.single()[Evidencias.estado] })
        assertEquals("INTERMEDIO", cliente.getComo("p1", "PRACTICANTE", "/v1/yo/perfil").body<PerfilPracticanteDto>().nivelesDemostrados["powerbi"])
        assertTrue(cliente.getComo("p1", "PRACTICANTE", "/v1/notificaciones").body<List<NotificacionDto>>().any { it.tipo == "EVIDENCIA" })
    }

    // ---------- F8: pagos simulados ----------

    @Test
    fun planEmpresa_pagoSimulado_dura30DiasYSeAcumula() = conApi { cliente, bd ->
        cliente.empresaConVacante()
        val primero = cliente.postComo("empresa", "EMPRESA", "/v1/empresa/plan/pago-simulado").body<EstadoPlanDto>()
        assertTrue(primero.activo)
        assertEquals("2026-12-24", primero.venceEn)
        assertEquals(14900, primero.precioCentimos)
        val segundo = cliente.postComo("empresa", "EMPRESA", "/v1/empresa/plan/pago-simulado").body<EstadoPlanDto>()
        assertEquals("2027-01-23", segundo.venceEn, "Pagar antes de que venza suma 30 días")
        assertEquals(2, segundo.pagos.size)
        val saldo = cliente.getComo("empresa", "EMPRESA", "/v1/empresa/saldo").body<SaldoDto>()
        assertTrue(saldo.planActivo)
        assertEquals("2027-01-23", saldo.planVenceEn)
        assertTrue(cliente.getComo("empresa", "EMPRESA", "/v1/notificaciones").body<List<NotificacionDto>>().any { it.tipo == "PAGO" })

        // Si todas las suscripciones vencen, el plan se apaga solo.
        bd.tx { SuscripcionesEmpresa.update({ SuscripcionesEmpresa.estado eq "ACTIVA" }) { it[venceEn] = Instant.parse("2026-11-01T00:00:00Z") } }
        assertFalse(cliente.getComo("empresa", "EMPRESA", "/v1/empresa/saldo").body<SaldoDto>().planActivo)
    }

    @Test
    fun premium_pagoSimulado_desbloqueaElDetalle() = conApi { cliente, _ ->
        val v = cliente.empresaConVacante()
        assertTrue(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/diagnosticos/${v.id}").body<DiagnosticoDto>().detalleBloqueado)
        val premium = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/premium/pago-simulado").body<EstadoPlanDto>()
        assertTrue(premium.activo)
        assertEquals(999, premium.precioCentimos)
        assertTrue(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/perfil").body<PerfilPracticanteDto>().premium)
        assertFalse(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/diagnosticos/${v.id}").body<DiagnosticoDto>().detalleBloqueado)
        assertEquals(HttpStatusCode.Forbidden, cliente.postComo("empresa", "EMPRESA", "/v1/yo/premium/pago-simulado").status)
    }

    // ---------- Regla 13 y F9: notificaciones ----------

    @Test
    fun regla13_laEmpresaPidePreguntas_yElPracticanteLasRespondeDespues() = conApi { cliente, _ ->
        val v = cliente.empresaConVacante()
        // Sin respuestas: entra "Sin evaluar" y no gasta evaluaciones.
        val postulacion = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/postulaciones", PostularDto(v.id, declaracionJurada = true)).body<PostulacionDto>()
        assertFalse(postulacion.evaluada)
        assertTrue(cliente.getComo("empresa", "EMPRESA", "/v1/notificaciones").body<List<NotificacionDto>>().any { it.tipo == "NUEVO_POSTULANTE" })

        // Sin plan no se puede pedir; con el pago simulado, sí.
        assertEquals(HttpStatusCode.UnprocessableEntity, cliente.postComo("empresa", "EMPRESA", "/v1/empresa/postulaciones/${postulacion.id}/enviar-preguntas").status)
        cliente.postComo("empresa", "EMPRESA", "/v1/empresa/plan/pago-simulado")
        assertEquals(HttpStatusCode.NoContent, cliente.postComo("empresa", "EMPRESA", "/v1/empresa/postulaciones/${postulacion.id}/enviar-preguntas").status)

        val pendiente = cliente.getComo("p1", "PRACTICANTE", "/v1/yo/postulaciones").body<List<PostulacionDto>>().single()
        assertTrue(pendiente.preguntasPendientes)
        val avisos = cliente.getComo("p1", "PRACTICANTE", "/v1/notificaciones").body<List<NotificacionDto>>()
        assertTrue(avisos.any { it.tipo == "PREGUNTAS_FILTRO" })

        val preguntas = cliente.getComo("empresa", "EMPRESA", "/v1/empresa/vacantes/${v.id}/preguntas-filtro").body<List<PreguntaFiltroEmpresaDto>>()
        val respuestas = preguntas.map { RespuestaFiltroDto(it.id, it.indiceCorrecto) }
        val respondida = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/postulaciones/${postulacion.id}/respuestas-filtro", ResponderFiltroDto(respuestas)).body<PostulacionDto>()
        assertTrue(respondida.evaluada)
        assertFalse(respondida.preguntasPendientes)
        assertTrue(preguntas.all { respondida.sellos[it.requisitoId] == "CONFIRMADO" })
        // No se puede responder dos veces.
        assertEquals(
            HttpStatusCode.UnprocessableEntity,
            cliente.postComo("p1", "PRACTICANTE", "/v1/yo/postulaciones/${postulacion.id}/respuestas-filtro", ResponderFiltroDto(respuestas)).status,
        )
    }

    @Test
    fun notificaciones_cadaUnoVeLasSuyas() = conApi { cliente, _ ->
        cliente.empresaConVacante()
        cliente.postComo("p1", "PRACTICANTE", "/v1/yo/premium/pago-simulado")
        val mias = cliente.getComo("p1", "PRACTICANTE", "/v1/notificaciones").body<List<NotificacionDto>>()
        assertEquals(1, cliente.getComo("p1", "PRACTICANTE", "/v1/notificaciones/resumen").body<ResumenNotificacionesDto>().noLeidas)
        assertTrue(cliente.getComo("p2", "PRACTICANTE", "/v1/notificaciones").body<List<NotificacionDto>>().isEmpty())
        assertEquals(HttpStatusCode.NotFound, cliente.postComo("p2", "PRACTICANTE", "/v1/notificaciones/${mias[0].id}/leida").status, "No puede marcar las de otro")
        assertEquals(HttpStatusCode.NoContent, cliente.postComo("p1", "PRACTICANTE", "/v1/notificaciones/${mias[0].id}/leida").status)
        assertEquals(0, cliente.getComo("p1", "PRACTICANTE", "/v1/notificaciones/resumen").body<ResumenNotificacionesDto>().noLeidas)
    }

    @Test
    fun metricas_resumenParaElEquipo() = conApi { cliente, _ ->
        val v = cliente.empresaConVacante()
        cliente.postComo("p1", "PRACTICANTE", "/v1/yo/postulaciones", PostularDto(v.id, declaracionJurada = true))
        val metricas = cliente.get("/v1/admin/metricas") { header("X-Admin-Token", TOKEN_ADMIN) }.body<MetricasDto>()
        assertEquals(1, metricas.postulaciones)
        assertEquals(1, metricas.vacantesAbiertas)
        assertEquals(1, metricas.eventos30Dias["VACANTE_PUBLICADA"])
        assertEquals(1, metricas.eventos30Dias["POSTULACION"])
        assertEquals(HttpStatusCode.NotFound, cliente.get("/v1/admin/metricas").status)
    }

    // ---------- Evaluación de habilidades y asistente ----------

    private suspend fun HttpClient.declarar(uid: String, niveles: Map<String, String>) = put("/v1/yo/perfil") {
        como(uid, "PRACTICANTE")
        contentType(ContentType.Application.Json)
        setBody(pe.edu.esan.chambaready.api.dto.ActualizarPerfilDto(nivelesDeclarados = niveles))
    }

    @Test
    fun evaluacion_alNivelDeclarado_yGratisUnaSolaVez() = conApi { cliente, _ ->
        cliente.declarar("p1", mapOf("sql" to "INTERMEDIO", "equipo" to "BASICO"))
        assertTrue(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/evaluacion").body<pe.edu.esan.chambaready.api.dto.EstadoEvaluacionDto>().puedeEvaluar)

        val evaluacion = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evaluacion", pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto(listOf("sql", "equipo")))
            .body<pe.edu.esan.chambaready.api.dto.EvaluacionDto>()
        assertEquals(listOf("INTERMEDIO", "BASICO"), evaluacion.pruebas.map { it.nivel })
        assertTrue(evaluacion.pruebas.all { it.preguntas.size == 3 })

        // Gratis: una sola vez.
        val otra = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evaluacion", pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto(listOf("sql")))
        assertEquals(HttpStatusCode.UnprocessableEntity, otra.status)
        assertFalse(cliente.getComo("p1", "PRACTICANTE", "/v1/yo/evaluacion").body<pe.edu.esan.chambaready.api.dto.EstadoEvaluacionDto>().puedeEvaluar)
        // Con Premium, sin límite.
        cliente.postComo("p1", "PRACTICANTE", "/v1/yo/premium/pago-simulado")
        assertEquals(HttpStatusCode.OK, cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evaluacion", pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto(listOf("sql"))).status)
    }

    @Test
    fun evaluacion_soloDeHabilidadesDeclaradas() = conApi { cliente, _ ->
        cliente.declarar("p1", mapOf("sql" to "BASICO"))
        val r = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evaluacion", pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto(listOf("python")))
        assertEquals(HttpStatusCode.BadRequest, r.status)
        val muchas = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evaluacion", pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto(listOf("a", "b", "c", "d", "e")))
        assertEquals(HttpStatusCode.BadRequest, muchas.status)
    }

    @Test
    fun aprobarLaEvaluacion_confirmaElSelloAlPostular() = conApi(IaFalsa()) { cliente, _ ->
        val v = cliente.empresaConVacante()
        cliente.declarar("p1", mapOf("sql" to "INTERMEDIO"))
        val prueba = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/evaluacion", pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto(listOf("sql")))
            .body<pe.edu.esan.chambaready.api.dto.EvaluacionDto>().pruebas.single()
        // La IA falsa siempre pone la correcta en la opción 0.
        cliente.postComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/${prueba.id}/respuestas", ResponderMiniTestDto(listOf(0, 0, 0)))
        val postulacion = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/postulaciones", PostularDto(v.id, declaracionJurada = true)).body<PostulacionDto>()
        val sql = v.requisitos.first { it.habilidad == "SQL" }
        assertEquals("CONFIRMADO", postulacion.sellos[sql.id], "SQL intermedio demostrado: de Declarado pasa a Confirmado")
    }

    @Test
    fun quienRepiteElMiniTest_recibePreguntasNuevas() {
        val ia = IaFalsa()
        conApi(ia) { cliente, _ ->
            val primero = cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/excel").body<MiniTestDto>()
            cliente.postComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/${primero.id}/respuestas", ResponderMiniTestDto(listOf(1, 1, 1)))
            val segundo = cliente.getComo("p1", "PRACTICANTE", "/v1/yo/mini-tests/excel").body<MiniTestDto>()
            assertTrue(primero.id != segundo.id)
            assertEquals(2, ia.llamadas)
        }
    }

    @Test
    fun asistente_sinIa_entiendeHabilidadesYNivel() = conApi { cliente, _ ->
        val r = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/asistente", pe.edu.esan.chambaready.api.dto.MensajeAsistenteDto("También sé Excel intermedio y algo de Python"))
            .body<pe.edu.esan.chambaready.api.dto.RespuestaAsistenteDto>()
        assertFalse(r.conIa)
        assertEquals(setOf("excel", "python"), r.habilidades.map { it.habilidadId }.toSet())
        assertTrue(r.habilidades.all { it.nivel == "INTERMEDIO" })
    }

    @Test
    fun asistente_conIa_soloHabilidadesDelCatalogo() = conApi(IaFalsa()) { cliente, _ ->
        val r = cliente.postComo("p1", "PRACTICANTE", "/v1/yo/asistente", pe.edu.esan.chambaready.api.dto.MensajeAsistenteDto("Sé Excel avanzado y cocina"))
            .body<pe.edu.esan.chambaready.api.dto.RespuestaAsistenteDto>()
        assertTrue(r.conIa)
        assertEquals(listOf("excel" to "AVANZADO"), r.habilidades.map { it.habilidadId to it.nivel })
        assertEquals("Anotado: Excel avanzado.", r.respuesta)
    }
}
