package pe.edu.esan.chambaready.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.path
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import pe.edu.esan.chambaready.api.archivos.ServicioArchivos
import pe.edu.esan.chambaready.api.auth.Autenticador
import pe.edu.esan.chambaready.api.auth.AutenticadorDesarrollo
import pe.edu.esan.chambaready.api.auth.AutenticadorFirebase
import pe.edu.esan.chambaready.api.auth.SinAutenticacion
import pe.edu.esan.chambaready.api.auth.VerificadorFirebaseAdmin
import pe.edu.esan.chambaready.api.auth.VerificadorToken
import pe.edu.esan.chambaready.api.config.Configuracion
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.errores.instalarManejoDeErrores
import pe.edu.esan.chambaready.api.ia.GeminiHttp
import pe.edu.esan.chambaready.api.ia.ProveedorIa
import pe.edu.esan.chambaready.api.ia.ServicioIa
import pe.edu.esan.chambaready.api.notificaciones.Notificador
import pe.edu.esan.chambaready.api.notificaciones.ServicioNotificaciones
import pe.edu.esan.chambaready.api.pagos.ServicioPagos
import pe.edu.esan.chambaready.api.rutas.Dependencias
import pe.edu.esan.chambaready.api.rutas.rutasAdmin
import pe.edu.esan.chambaready.api.rutas.rutasCompartidas
import pe.edu.esan.chambaready.api.rutas.rutasEmpresa
import pe.edu.esan.chambaready.api.rutas.rutasPracticante
import pe.edu.esan.chambaready.api.rutas.rutasPublicas
import pe.edu.esan.chambaready.api.rutas.rutasSesion
import pe.edu.esan.chambaready.api.servicios.ServicioAdmin
import pe.edu.esan.chambaready.api.servicios.ServicioEmpresa
import pe.edu.esan.chambaready.api.servicios.ServicioPracticante
import java.time.Clock
import java.time.ZoneId

private val log = LoggerFactory.getLogger("ChambaReadyApi")

/** Punto de entrada: lee la configuración del entorno, migra la base y levanta el servidor. */
fun main() {
    val config = Configuracion.desdeEntorno()
    val pool = BaseDatos.crearPool(config.baseDatos, config.maxConexiones)
    val bd = BaseDatos(pool)
    bd.migrar(conDatosDemo = config.cargarDatosDemo)
    if (config.modoDesarrollo) log.warn("MODO_DESARROLLO activo: se aceptan los encabezados X-Demo-*. NUNCA uses esto en producción.")
    embeddedServer(Netty, port = config.puerto, host = "0.0.0.0") {
        modulo(config, crearDependencias(config, bd))
    }.start(wait = true)
}

/** Hora del Perú para las reglas de fechas (cuenta regresiva, viabilidad, entrevistas). */
val relojPeru: Clock = Clock.system(ZoneId.of("America/Lima"))

/**
 * Arma las dependencias. [verificador] permite a las pruebas reemplazar a Firebase; si es null y hay
 * credenciales de Firebase en el entorno, se usa Firebase Admin SDK.
 */
fun crearDependencias(
    config: Configuracion,
    bd: BaseDatos,
    reloj: Clock = relojPeru,
    verificador: VerificadorToken? = null,
    proveedorIa: ProveedorIa? = config.geminiApiKey?.let { GeminiHttp(it) },
): Dependencias {
    if (proveedorIa == null) log.warn("GEMINI_API_KEY no está configurada: la IA usará contenido simulado.")
    val ia = ServicioIa(bd, proveedorIa, config.geminiModelo, config.geminiModeloSimple)
    val archivos = ServicioArchivos(bd)
    val notificador = Notificador(reloj)
    return Dependencias(
        bd = bd,
        autenticador = elegirAutenticador(config, verificador),
        practicante = ServicioPracticante(bd, reloj, ia, archivos, notificador),
        empresa = ServicioEmpresa(bd, reloj, ia, archivos, notificador),
        admin = ServicioAdmin(bd, reloj, notificador),
        archivos = archivos,
        pagos = ServicioPagos(bd, reloj, notificador),
        notificaciones = ServicioNotificaciones(bd),
        tokenAdmin = config.tokenAdmin,
    )
}

/**
 * Producción: solo tokens de Firebase. Desarrollo: tokens de Firebase (si está configurado) y, si no
 * hay token, los encabezados X-Demo-*. Sin Firebase y sin modo desarrollo: nadie entra (401).
 */
private fun elegirAutenticador(config: Configuracion, verificador: VerificadorToken?): Autenticador {
    val firebase = (verificador ?: verificadorDesdeEntorno(config))?.let { AutenticadorFirebase(it) }
    if (firebase == null && !config.modoDesarrollo) {
        log.warn("Firebase no está configurado (FIREBASE_PROJECT_ID / FIREBASE_CREDENCIALES_JSON): los endpoints protegidos responderán 401.")
    }
    return when {
        firebase != null && config.modoDesarrollo -> Autenticador { call -> firebase.identificar(call) ?: AutenticadorDesarrollo.identificar(call) }
        firebase != null -> firebase
        config.modoDesarrollo -> AutenticadorDesarrollo
        else -> SinAutenticacion
    }
}

private fun verificadorDesdeEntorno(config: Configuracion): VerificadorToken? {
    val proyecto = config.firebaseProyectoId ?: return null
    val credenciales = config.firebaseCredencialesJson ?: return null
    return VerificadorFirebaseAdmin(credenciales, proyecto)
}

/** Configura Ktor. Los tests lo llaman con una base de datos de prueba. */
fun Application.modulo(config: Configuracion, d: Dependencias) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                // Siempre envía TODOS los campos, aunque tengan su valor por defecto. Sin esto,
                // kotlinx.serialization omite por ejemplo verificacion = "EN_REVISION", y la web
                // (TypeScript) recibe undefined.
                encodeDefaults = true
            },
        )
    }
    install(CallLogging) {
        // Solo método, ruta y estado: nunca el cuerpo ni los encabezados (pueden tener tokens o datos personales).
        filter { it.request.path().startsWith("/v1") }
    }
    install(CORS) {
        config.origenesCors.forEach { origen ->
            val (esquema, host) = origen.split("://", limit = 2).let { if (it.size == 2) it[0] to it[1] else "https" to it[0] }
            allowHost(host, schemes = listOf(esquema))
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Nombre-Archivo")
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
    }
    instalarManejoDeErrores()
    routing {
        route("/v1") {
            rutasPublicas(d)
            rutasSesion(d)
            rutasPracticante(d)
            rutasEmpresa(d)
            rutasCompartidas(d)
            rutasAdmin(d)
        }
    }
}
