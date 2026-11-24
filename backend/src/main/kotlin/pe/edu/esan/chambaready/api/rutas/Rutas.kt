package pe.edu.esan.chambaready.api.rutas

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.v1.jdbc.selectAll
import pe.edu.esan.chambaready.api.archivos.ServicioArchivos
import pe.edu.esan.chambaready.api.archivos.TipoArchivo
import pe.edu.esan.chambaready.api.auth.Autenticador
import pe.edu.esan.chambaready.api.auth.Rol
import pe.edu.esan.chambaready.api.auth.identidad
import pe.edu.esan.chambaready.api.auth.registrar
import pe.edu.esan.chambaready.api.auth.usuarioRegistrado
import pe.edu.esan.chambaready.api.auth.usuario
import pe.edu.esan.chambaready.api.db.AreasInteres
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Habilidades
import pe.edu.esan.chambaready.api.dto.ActualizarPerfilDto
import pe.edu.esan.chambaready.api.dto.AgendarEntrevistaDto
import pe.edu.esan.chambaready.api.dto.AnalizarCvDto
import pe.edu.esan.chambaready.api.dto.AutoevaluacionDto
import pe.edu.esan.chambaready.api.dto.CrearEvidenciaDto
import pe.edu.esan.chambaready.api.dto.LogoDto
import pe.edu.esan.chambaready.api.dto.ResponderFiltroDto
import pe.edu.esan.chambaready.api.dto.ResponderMiniTestDto
import pe.edu.esan.chambaready.api.dto.SugerirPreguntasDto
import pe.edu.esan.chambaready.api.dto.AreaDto
import pe.edu.esan.chambaready.api.dto.CambiarEstadoDto
import pe.edu.esan.chambaready.api.dto.CrearVacanteDto
import pe.edu.esan.chambaready.api.dto.EmpresaDto
import pe.edu.esan.chambaready.api.dto.HabilidadDto
import pe.edu.esan.chambaready.api.dto.HorasLibresDto
import pe.edu.esan.chambaready.api.dto.PostularDto
import pe.edu.esan.chambaready.api.dto.PreciosDto
import pe.edu.esan.chambaready.api.dto.RegistroDto
import pe.edu.esan.chambaready.api.dto.SesionDto
import pe.edu.esan.chambaready.api.dto.SaludDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import pe.edu.esan.chambaready.api.notificaciones.ServicioNotificaciones
import pe.edu.esan.chambaready.api.pagos.ServicioPagos
import pe.edu.esan.chambaready.api.servicios.Consultas
import pe.edu.esan.chambaready.api.servicios.ServicioAdmin
import pe.edu.esan.chambaready.api.servicios.ServicioEmpresa
import pe.edu.esan.chambaready.api.servicios.ServicioPracticante
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Lo que necesitan las rutas. Se arma una vez en [pe.edu.esan.chambaready.api.modulo]. */
class Dependencias(
    val bd: BaseDatos,
    val autenticador: Autenticador,
    val practicante: ServicioPracticante,
    val empresa: ServicioEmpresa,
    val admin: ServicioAdmin,
    val archivos: ServicioArchivos,
    val pagos: ServicioPagos,
    val notificaciones: ServicioNotificaciones,
    val tokenAdmin: String?,
)

private fun io.ktor.server.application.ApplicationCall.parametro(nombre: String): String =
    parameters[nombre] ?: throw ErrorApi.SolicitudInvalida("Falta el parámetro $nombre.")

// ---------- Públicas ----------

fun Route.rutasPublicas(d: Dependencias) {
    get("/salud") {
        val bdOk = runCatching { d.bd.tx { Consultas.configuracionLong("evaluaciones_gratis") } }.isSuccess
        call.respond(if (bdOk) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable, SaludDto(if (bdOk) "ok" else "degradado", if (bdOk) "ok" else "sin conexión", "0.1.0"))
    }
    get("/catalogo/areas") {
        call.respond(d.bd.tx { AreasInteres.selectAll().map { AreaDto(it[AreasInteres.codigo], it[AreasInteres.nombre]) } })
    }
    get("/catalogo/habilidades") {
        call.respond(d.bd.tx { Habilidades.selectAll().map { HabilidadDto(it[Habilidades.id], it[Habilidades.nombre], it[Habilidades.tipo]) }.sortedBy { it.nombre } })
    }
    get("/configuracion/precios") {
        call.respond(
            d.bd.tx {
                PreciosDto(
                    premiumCentimos = Consultas.configuracionLong("precio_premium_centimos"),
                    planEmpresaCentimos = Consultas.configuracionLong("precio_plan_empresa_centimos"),
                    evaluacionesGratis = Consultas.configuracionLong("evaluaciones_gratis").toInt(),
                )
            },
        )
    }
}

// ---------- Sesión (F5): requieren token, pero no rol ----------

/**
 * GET /v1/sesion: ¿esta cuenta de Firebase ya se registró y con qué rol? Los clientes lo usan al
 * iniciar sesión para saber si mostrar el registro o entrar.
 * POST /v1/registro: fija el rol (la app manda PRACTICANTE y la web EMPRESA). No se puede cambiar.
 */
fun Route.rutasSesion(d: Dependencias) {
    get("/sesion") {
        val usuario = usuarioRegistrado(d.bd, call.identidad(d.autenticador))
        call.respond(SesionDto(registrado = usuario != null, rol = usuario?.rol?.name))
    }
    post("/registro") {
        val identidad = call.identidad(d.autenticador)
        val datos = call.receive<RegistroDto>()
        val rol = Rol.entries.firstOrNull { it.name == datos.rol } ?: throw ErrorApi.SolicitudInvalida("Rol desconocido: ${datos.rol}")
        val nombre = datos.nombre.trim().ifEmpty { identidad.nombre }
        val usuario = registrar(d.bd, identidad.copy(nombre = nombre), rol)
        call.respond(HttpStatusCode.Created, SesionDto(registrado = true, rol = usuario.rol.name))
    }
}

// ---------- Practicante (/v1/yo y /v1/vacantes) ----------

fun Route.rutasPracticante(d: Dependencias) {
    suspend fun io.ktor.server.application.ApplicationCall.yo() = usuario(d.autenticador, d.bd, Rol.PRACTICANTE).id

    route("/yo") {
        get("/perfil") { call.respond(d.practicante.perfil(call.yo())) }
        put("/perfil") { call.respond(d.practicante.actualizarPerfil(call.yo(), call.receive<ActualizarPerfilDto>())) }
        put("/horas-libres") { call.respond(d.practicante.guardarHoras(call.yo(), call.receive<HorasLibresDto>())) }
        get("/diagnosticos/{vacanteId}") { call.respond(d.practicante.diagnostico(call.yo(), call.parametro("vacanteId"))) }
        get("/postulaciones") { call.respond(d.practicante.postulaciones(call.yo())) }
        post("/postulaciones") {
            call.respond(HttpStatusCode.Created, d.practicante.postular(call.yo(), call.receive<PostularDto>()))
        }
        // Regla 13: responder las preguntas que pidió la empresa después de postular.
        post("/postulaciones/{id}/respuestas-filtro") {
            call.respond(d.practicante.responderFiltroPendiente(call.yo(), call.parametro("id"), call.receive<ResponderFiltroDto>()))
        }
        // F6: CV con IA, mini-tests corregidos en el servidor y ranking privado.
        post("/cv") { call.respond(d.practicante.analizarCv(call.yo(), call.receive<AnalizarCvDto>())) }
        get("/mini-tests/{habilidadId}") { call.respond(d.practicante.miniTest(call.yo(), call.parametro("habilidadId"))) }
        post("/mini-tests/{id}/respuestas") {
            call.respond(d.practicante.responderMiniTest(call.yo(), call.parametro("id"), call.receive<ResponderMiniTestDto>()))
        }
        post("/autoevaluacion") { call.respond(d.practicante.autoevaluacion(call.yo(), call.receive<AutoevaluacionDto>())) }
        // F7: evidencias.
        get("/evidencias") { call.respond(d.practicante.evidencias(call.yo())) }
        post("/evidencias") { call.respond(HttpStatusCode.Created, d.practicante.crearEvidencia(call.yo(), call.receive<CrearEvidenciaDto>())) }
        // F8: Premium con pago simulado.
        get("/premium") { call.respond(d.pagos.estadoPremium(call.yo())) }
        post("/premium/pago-simulado") { call.respond(d.pagos.pagarPremium(call.yo())) }
    }
    route("/vacantes") {
        get { call.respond(d.practicante.vacantes(call.yo())) }
        get("/{id}") { call.respond(d.practicante.vacante(call.yo(), call.parametro("id"))) }
        get("/{id}/preguntas-filtro") {
            call.yo()
            call.respond(d.practicante.preguntasFiltro(call.parametro("id")))
        }
    }
}

// ---------- Empresa (/v1/empresa) ----------

fun Route.rutasEmpresa(d: Dependencias) {
    suspend fun io.ktor.server.application.ApplicationCall.yo() = usuario(d.autenticador, d.bd, Rol.EMPRESA).id

    route("/empresa") {
        get("/perfil") { call.respond(d.empresa.perfil(call.yo())) }
        put("/perfil") { call.respond(d.empresa.guardarPerfil(call.yo(), call.receive<EmpresaDto>())) }
        get("/saldo") { call.respond(d.empresa.saldo(call.yo())) }
        get("/vacantes") { call.respond(d.empresa.vacantes(call.yo())) }
        post("/vacantes") { call.respond(HttpStatusCode.Created, d.empresa.crearVacante(call.yo(), call.receive<CrearVacanteDto>())) }
        get("/vacantes/{id}/postulantes") { call.respond(d.empresa.postulantes(call.yo(), call.parametro("id"))) }
        patch("/postulaciones/{id}") {
            call.respond(d.empresa.cambiarEstado(call.yo(), call.parametro("id"), call.receive<CambiarEstadoDto>()))
        }
        post("/entrevistas") { call.respond(HttpStatusCode.Created, d.empresa.agendar(call.yo(), call.receive<AgendarEntrevistaDto>())) }
        get("/entrevistas") { call.respond(d.empresa.entrevistas(call.yo())) }
        get("/actividad") { call.respond(d.empresa.actividad(call.yo())) }
        get("/postulaciones/{id}") { call.respond(d.empresa.postulante(call.yo(), call.parametro("id"))) }
        post("/postulaciones/{id}/enviar-preguntas") {
            d.empresa.enviarPreguntas(call.yo(), call.parametro("id"))
            call.respond(HttpStatusCode.NoContent)
        }
        // F6: preguntas filtro sugeridas por la IA (la empresa las revisa antes de publicar).
        post("/preguntas-filtro/sugerir") { call.respond(d.empresa.sugerirPreguntas(call.yo(), call.receive<SugerirPreguntasDto>())) }
        get("/vacantes/{id}/preguntas-filtro") { call.respond(d.empresa.preguntasDeVacante(call.yo(), call.parametro("id"))) }
        // F7: logo (subido antes con POST /v1/archivos?tipo=LOGO).
        put("/logo") { call.respond(d.empresa.guardarLogo(call.yo(), call.receive<LogoDto>())) }
        // F8: Plan Empresa con pago simulado.
        get("/plan") { call.respond(d.pagos.estadoPlanEmpresa(call.yo())) }
        post("/plan/pago-simulado") { call.respond(d.pagos.pagarPlanEmpresa(call.yo())) }
    }
}

// ---------- Compartidas: archivos y notificaciones (cualquier rol registrado) ----------

/** Tamaño máximo que se acepta leer (el de un CV); cada tipo tiene además su propio límite. */
private const val BYTES_MAXIMOS_SUBIDA = 5L * 1024 * 1024

fun Route.rutasCompartidas(d: Dependencias) {
    suspend fun io.ktor.server.application.ApplicationCall.yo() = usuario(d.autenticador, d.bd, null)

    route("/archivos") {
        /**
         * Sube un archivo. El cuerpo es el archivo tal cual (no multipart), con su Content-Type.
         * El nombre va en el encabezado X-Nombre-Archivo (codificado como en una URL).
         */
        post {
            val usuario = call.yo()
            val largo = call.request.contentLength() ?: throw ErrorApi.SolicitudInvalida("Falta el tamaño del archivo (Content-Length).")
            if (largo > BYTES_MAXIMOS_SUBIDA) throw ErrorApi.SolicitudInvalida("El archivo pesa más de 5 MB.")
            val nombre = call.request.headers["X-Nombre-Archivo"]?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
            val tipo = TipoArchivo.entries.firstOrNull { it.name == call.request.queryParameters["tipo"] }?.name
            val archivo = d.archivos.subir(usuario, tipo, call.request.contentType().withoutParameters().toString(), nombre, call.receive<ByteArray>())
            call.respond(HttpStatusCode.Created, archivo)
        }
        get("/{id}") {
            val archivo = d.archivos.descargar(call.yo(), call.parametro("id"))
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, archivo.nombre).toString(),
            )
            // Son datos personales: el navegador no debe guardarlos en cachés compartidas.
            call.response.header(HttpHeaders.CacheControl, "private, no-store")
            call.respondBytes(archivo.bytes, ContentType.parse(archivo.mime))
        }
    }

    route("/notificaciones") {
        get { call.respond(d.notificaciones.listar(call.yo().id)) }
        get("/resumen") { call.respond(d.notificaciones.resumen(call.yo().id)) }
        post("/leer-todas") {
            d.notificaciones.marcarTodas(call.yo().id)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/{id}/leida") {
            d.notificaciones.marcarLeida(call.yo().id, call.parametro("id"))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// ---------- Administración (/v1/admin) ----------

/**
 * Protegido con el encabezado X-Admin-Token. Sin ADMIN_TOKEN configurado, o con un token incorrecto,
 * responde 404 para no revelar que existe. La comparación es en tiempo constante.
 */
fun Route.rutasAdmin(d: Dependencias) {
    fun io.ktor.server.application.ApplicationCall.exigirAdmin() {
        val esperado = d.tokenAdmin ?: throw ErrorApi.NoEncontrado("No encontrado.")
        val recibido = request.headers["X-Admin-Token"].orEmpty()
        if (!MessageDigest.isEqual(esperado.toByteArray(), recibido.toByteArray())) throw ErrorApi.NoEncontrado("No encontrado.")
    }

    route("/admin") {
        get("/costos-ia") {
            call.exigirAdmin()
            call.respond(d.admin.costosIa())
        }
        post("/empresas/{ruc}/verificar") {
            call.exigirAdmin()
            d.admin.verificarEmpresa(call.parametro("ruc"))
            call.respond(HttpStatusCode.NoContent)
        }
        post("/empresas/{ruc}/plan-de-prueba") {
            call.exigirAdmin()
            d.admin.activarPlanDePrueba(call.parametro("ruc"))
            call.respond(HttpStatusCode.NoContent)
        }
        post("/evidencias/{id}/validar") {
            call.exigirAdmin()
            d.admin.validarEvidencia(call.parametro("id"))
            call.respond(HttpStatusCode.NoContent)
        }
        get("/metricas") {
            call.exigirAdmin()
            call.respond(d.admin.metricas())
        }
    }
}
