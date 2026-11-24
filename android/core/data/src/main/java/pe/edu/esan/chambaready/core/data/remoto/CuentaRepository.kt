package pe.edu.esan.chambaready.core.data.remoto

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.domain.modelo.RolUsuario
import pe.edu.esan.chambaready.core.domain.reglas.DestinoTrasLogin
import pe.edu.esan.chambaready.core.domain.reglas.ReglasSesion
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/** Mini-test del backend (F6): las preguntas llegan SIN la respuesta; corrige el servidor. */
data class MiniTestRemoto(val id: String, val nivel: NivelHabilidad, val preguntas: List<PreguntaOpcion>)

data class ResultadoMiniTestRemoto(
    val correctas: Int,
    val total: Int,
    val nivelAntes: NivelHabilidad,
    val nivelDespues: NivelHabilidad,
)

/** P02: habilidades del CV (id → nivel sugerido) que detectó la IA del backend. */
data class CvAnalizado(val habilidades: Map<String, NivelHabilidad>, val proyectos: List<String>, val conIa: Boolean)

data class NotificacionRemota(val notificacion: Notificacion, val leida: Boolean)

/**
 * Lo que la cuenta del practicante guarda en el backend: rol, perfil y postulaciones.
 * En MODO DEMO ([CuentaLocalRepository]) nada sale del teléfono.
 */
interface CuentaRepository {

    /** Tras iniciar sesión en Firebase: ¿entra, completa el registro o es una empresa? */
    suspend fun destinoTrasLogin(): DestinoTrasLogin

    /** POST /v1/registro con rol PRACTICANTE. Si ya estaba registrado como practicante, no hace nada. */
    suspend fun registrarPracticante(nombre: String)

    /** Sube los datos del registro, el área, los niveles declarados y las horas libres. */
    suspend fun subirPerfil(estado: EstadoUsuario)

    /**
     * Trae el perfil guardado en el backend y lo mezcla con el estado local (al iniciar sesión en un
     * teléfono nuevo). Lo que el backend no guarda todavía (plan, metas…) se conserva.
     */
    suspend fun traerPerfil(actual: EstadoUsuario, correo: String): EstadoUsuario

    /**
     * Envía la postulación. Devuelve la que guardó el backend (con los sellos que él calculó) o, en
     * modo demo, la misma [local].
     */
    suspend fun postular(local: Postulacion, declaraciones: Map<String, Boolean>, declaracionJurada: Boolean): Postulacion

    /** Postulaciones con su estado actual (la empresa las mueve en E04); null en modo demo. */
    suspend fun postulaciones(): List<Postulacion>?

    // ---------- F6 a F9. En MODO DEMO devuelven null/false y las pantallas usan los datos locales. ----------

    /** P24: preguntas filtro sin la respuesta, o null en modo demo. */
    suspend fun preguntasFiltro(vacanteId: String): List<PreguntaOpcion>? = null

    /** P17 con respuestas: id de pregunta → opción elegida (null = sin responder). */
    suspend fun postular(local: Postulacion, declaraciones: Map<String, Boolean>, declaracionJurada: Boolean, respuestasFiltro: Map<String, Int?>): Postulacion =
        postular(local, declaraciones, declaracionJurada)

    /** Regla 13: responde las preguntas que pidió la empresa. null en modo demo. */
    suspend fun responderFiltroPendiente(postulacionId: String, respuestas: Map<String, Int?>): Postulacion? = null

    suspend fun miniTest(habilidadId: String): MiniTestRemoto? = null

    suspend fun responderMiniTest(miniTestId: String, respuestas: List<Int?>, vacanteId: String?): ResultadoMiniTestRemoto? = null

    /** Sube el CV (F7) y lo analiza con IA (F6). null en modo demo. */
    suspend fun analizarCv(nombre: String, mime: String, bytes: ByteArray): CvAnalizado? = null

    /** Ranking privado calculado por el backend (Top X %), o null en modo demo. */
    suspend fun autoevaluacion(area: AreaInteres, correctas: Int, total: Int): Int? = null

    /** F8: pago SIMULADO de Premium. false en modo demo (se activa solo en el teléfono). */
    suspend fun pagarPremium(): Boolean = false

    suspend fun notificaciones(): List<NotificacionRemota>? = null

    suspend fun marcarNotificacionLeida(id: String) = Unit

    suspend fun marcarTodasLeidas() = Unit
}

/** Implementación con la API /v1. */
@Singleton
class CuentaRemotaRepository @Inject constructor(
    private val api: ApiChambaReady,
    private val json: Json,
) : CuentaRepository {

    override suspend fun destinoTrasLogin(): DestinoTrasLogin {
        val sesion = llamarApi(json) { api.sesion() }
        return ReglasSesion.destinoTrasLogin(sesion.registrado, ReglasSesion.rolDesdeTexto(sesion.rol))
    }

    override suspend fun registrarPracticante(nombre: String) {
        try {
            llamarApi(json) { api.registrar(RegistroDto(nombre = nombre.trim(), rol = RolUsuario.PRACTICANTE.name)) }
        } catch (e: ErrorRemoto) {
            // 409 = la cuenta ya estaba registrada. Si es como practicante, seguimos; si es empresa, no.
            if (e.estadoHttp != 409 || destinoTrasLogin() != DestinoTrasLogin.ENTRAR) throw e
        }
    }

    override suspend fun subirPerfil(estado: EstadoUsuario) {
        val perfil = actualizacionDePerfil(estado.registro, estado.area, estado.niveles, estado.onboardingCompleto)
        llamarApi(json) { api.actualizarPerfil(perfil) }
        llamarApi(json) { api.guardarHorasLibres(estado.horasLibres.aDto()) }
    }

    override suspend fun traerPerfil(actual: EstadoUsuario, correo: String): EstadoUsuario {
        val perfil = llamarApi(json) { api.perfil() }
        val postulaciones = postulaciones()
        return actual.copy(
            registro = DatosRegistro(perfil.nombre, correo, perfil.universidad, perfil.carrera, perfil.ciclo ?: 0),
            onboardingCompleto = perfil.onboardingCompleto,
            area = AreaInteres.entries.firstOrNull { it.name == perfil.area } ?: actual.area,
            premium = perfil.premium,
            niveles = perfil.niveles(),
            horasLibres = perfil.horas(),
            postulaciones = postulaciones,
        )
    }

    override suspend fun postular(local: Postulacion, declaraciones: Map<String, Boolean>, declaracionJurada: Boolean): Postulacion =
        postular(local, declaraciones, declaracionJurada, emptyMap())

    override suspend fun postular(
        local: Postulacion,
        declaraciones: Map<String, Boolean>,
        declaracionJurada: Boolean,
        respuestasFiltro: Map<String, Int?>,
    ): Postulacion {
        val cuerpo = PostularDto(
            vacanteId = local.vacanteId,
            declaraciones = declaraciones,
            respuestasFiltro = respuestasFiltro.map { (preguntaId, indice) -> RespuestaFiltroDto(preguntaId, indice) },
            declaracionJurada = declaracionJurada,
        )
        return llamarApi(json) { api.postular(cuerpo) }.aDominio()
    }

    override suspend fun postulaciones(): List<Postulacion> = llamarApi(json) { api.postulaciones() }.map { it.aDominio() }

    override suspend fun preguntasFiltro(vacanteId: String): List<PreguntaOpcion> =
        llamarApi(json) { api.preguntasFiltro(vacanteId) }.map { it.aDominio() }

    override suspend fun responderFiltroPendiente(postulacionId: String, respuestas: Map<String, Int?>): Postulacion {
        val cuerpo = ResponderFiltroDto(respuestas.map { (preguntaId, indice) -> RespuestaFiltroDto(preguntaId, indice) })
        return llamarApi(json) { api.responderFiltro(postulacionId, cuerpo) }.aDominio()
    }

    override suspend fun miniTest(habilidadId: String): MiniTestRemoto {
        val dto = llamarApi(json) { api.miniTest(habilidadId) }
        return MiniTestRemoto(
            id = dto.id,
            nivel = NivelHabilidad.entries.firstOrNull { it.name == dto.nivel } ?: NivelHabilidad.BASICO,
            preguntas = dto.preguntas.mapIndexed { i, p ->
                PreguntaOpcion(id = "${dto.id}-$i", enunciado = p.enunciado, opciones = p.opciones, indiceCorrecto = SIN_RESPUESTA, habilidadId = habilidadId)
            },
        )
    }

    override suspend fun responderMiniTest(miniTestId: String, respuestas: List<Int?>, vacanteId: String?): ResultadoMiniTestRemoto {
        val r = llamarApi(json) { api.responderMiniTest(miniTestId, ResponderMiniTestDto(respuestas, vacanteId)) }
        fun nivel(texto: String) = NivelHabilidad.entries.firstOrNull { it.name == texto } ?: NivelHabilidad.NO_LO_HE_USADO
        return ResultadoMiniTestRemoto(r.correctas, r.total, nivel(r.nivelAntes), nivel(r.nivelDespues))
    }

    override suspend fun analizarCv(nombre: String, mime: String, bytes: ByteArray): CvAnalizado {
        val nombreCodificado = URLEncoder.encode(nombre, StandardCharsets.UTF_8.name()).replace("+", "%20")
        val archivo = llamarApi(json) { api.subirArchivo("CV", nombreCodificado, bytes.toRequestBody(mime.toMediaTypeOrNull())) }
        val cv = llamarApi(json) { api.analizarCv(AnalizarCvDto(archivo.id)) }
        return CvAnalizado(
            habilidades = cv.habilidades.associate { h -> h.habilidadId to (NivelHabilidad.entries.firstOrNull { it.name == h.nivel } ?: NivelHabilidad.BASICO) },
            proyectos = cv.proyectos,
            conIa = cv.conIa,
        )
    }

    override suspend fun autoevaluacion(area: AreaInteres, correctas: Int, total: Int): Int =
        llamarApi(json) { api.autoevaluacion(AutoevaluacionDto(area.name, correctas, total)) }.topPorcentaje

    override suspend fun pagarPremium(): Boolean = llamarApi(json) { api.pagarPremium() }.activo

    override suspend fun notificaciones(): List<NotificacionRemota> =
        llamarApi(json) { api.notificaciones() }.mapNotNull { dto -> dto.aDominio()?.let { NotificacionRemota(it, dto.leida) } }

    override suspend fun marcarNotificacionLeida(id: String) {
        llamarApi(json) { api.marcarLeida(id) }
    }

    override suspend fun marcarTodasLeidas() {
        llamarApi(json) { api.marcarTodas() }
    }
}

/** MODO DEMO: no hay backend. Todo queda en el estado local del teléfono (DataStore). */
@Singleton
class CuentaLocalRepository @Inject constructor() : CuentaRepository {
    override suspend fun destinoTrasLogin() = DestinoTrasLogin.ENTRAR
    override suspend fun registrarPracticante(nombre: String) = Unit
    override suspend fun subirPerfil(estado: EstadoUsuario) = Unit
    override suspend fun traerPerfil(actual: EstadoUsuario, correo: String) = actual
    override suspend fun postular(local: Postulacion, declaraciones: Map<String, Boolean>, declaracionJurada: Boolean) = local
    override suspend fun postulaciones(): List<Postulacion>? = null
}
