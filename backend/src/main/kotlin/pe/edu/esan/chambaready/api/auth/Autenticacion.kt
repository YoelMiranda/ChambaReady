package pe.edu.esan.chambaready.api.auth

import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Usuarios
import pe.edu.esan.chambaready.api.errores.ErrorApi
import java.util.UUID

enum class Rol { PRACTICANTE, EMPRESA }

/**
 * Quién llama, según su credencial ya verificada. El ROL no viene en el token: se guarda en nuestra
 * base al registrarse (POST /v1/registro) y no se puede cambiar.
 */
data class Identidad(
    val firebaseUid: String,
    val nombre: String = "",
    val correo: String = "",
    /** Solo en modo desarrollo: permite registrarse sin llamar a /v1/registro. */
    val rolDemo: Rol? = null,
)

/** Usuario de nuestra base, ya resuelto. */
data class UsuarioActual(val id: String, val rol: Rol)

/** Verifica la credencial de una solicitud. Devuelve null si no hay credencial válida. */
fun interface Autenticador {
    suspend fun identificar(call: ApplicationCall): Identidad?
}

/** Sin Firebase configurado y sin modo desarrollo: todos los endpoints protegidos responden 401. */
object SinAutenticacion : Autenticador {
    override suspend fun identificar(call: ApplicationCall): Identidad? = null
}

/**
 * SOLO DESARROLLO (MODO_DESARROLLO=true): toma la identidad de `X-Demo-Usuario` y `X-Demo-Rol`.
 * Nunca lo actives en Heroku.
 */
object AutenticadorDesarrollo : Autenticador {
    override suspend fun identificar(call: ApplicationCall): Identidad? {
        val uid = call.request.headers["X-Demo-Usuario"]?.takeIf { it.isNotBlank() } ?: return null
        return Identidad(
            firebaseUid = uid,
            nombre = call.request.headers["X-Demo-Nombre"].orEmpty(),
            rolDemo = Rol.entries.firstOrNull { it.name == call.request.headers["X-Demo-Rol"] },
        )
    }
}

/** Valida un ID token y devuelve la identidad, o null si es falso, venció o es de otro proyecto. */
fun interface VerificadorToken {
    fun verificar(token: String): Identidad?
}

/**
 * Producción: lee `Authorization: Bearer <ID token de Firebase>` y lo valida con [verificador]
 * (Firebase Admin SDK). La validación descarga y cachea las claves públicas de Google: corre en IO.
 */
class AutenticadorFirebase(private val verificador: VerificadorToken) : Autenticador {
    private val log = LoggerFactory.getLogger(AutenticadorFirebase::class.java)

    override suspend fun identificar(call: ApplicationCall): Identidad? {
        val encabezado = call.request.headers["Authorization"] ?: return null
        if (!encabezado.startsWith("Bearer ", ignoreCase = true)) return null
        val token = encabezado.substring(7).trim().takeIf { it.isNotEmpty() } ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { verificador.verificar(token) }
                .onFailure { log.info("Token rechazado: {}", it.javaClass.simpleName) } // nunca se registra el token
                .getOrNull()
        }
    }
}

/** Identidad de quien llama o 401. */
suspend fun ApplicationCall.identidad(autenticador: Autenticador): Identidad =
    autenticador.identificar(this) ?: throw ErrorApi.NoAutenticado()

/**
 * Devuelve el usuario que llama y exige que tenga [rolRequerido] (null = cualquier rol, para rutas
 * compartidas como archivos y notificaciones). Si todavía no se registró responde 403
 * REGISTRO_PENDIENTE (el cliente debe llamar a POST /v1/registro). En modo desarrollo, con
 * `X-Demo-Rol`, se registra solo.
 */
suspend fun ApplicationCall.usuario(autenticador: Autenticador, bd: BaseDatos, rolRequerido: Rol?): UsuarioActual {
    val identidad = identidad(autenticador)
    val usuario = bd.tx { buscar(identidad.firebaseUid) }
        ?: identidad.rolDemo?.let { registrar(bd, identidad, it) }
        ?: throw ErrorApi.RegistroPendiente()
    if (rolRequerido != null && usuario.rol != rolRequerido) {
        throw ErrorApi.Prohibido("Esta sección es solo para cuentas de ${rolRequerido.name.lowercase()}.")
    }
    return usuario
}

private fun buscar(firebaseUid: String): UsuarioActual? =
    Usuarios.selectAll().where { Usuarios.firebaseUid eq firebaseUid }.singleOrNull()
        ?.let { UsuarioActual(it[Usuarios.id], Rol.valueOf(it[Usuarios.rol])) }

/**
 * POST /v1/registro: crea el usuario con su rol. Es idempotente con el mismo rol; con otro rol
 * responde 409 (una cuenta de empresa nunca puede volverse practicante, ni al revés).
 */
suspend fun registrar(bd: BaseDatos, identidad: Identidad, rol: Rol): UsuarioActual = bd.tx {
    val existente = buscar(identidad.firebaseUid)
    when {
        existente == null -> {
            val nuevoId = UUID.randomUUID().toString()
            Usuarios.insert {
                it[id] = nuevoId
                it[firebaseUid] = identidad.firebaseUid
                it[Usuarios.rol] = rol.name
                it[nombre] = identidad.nombre
                it[correo] = identidad.correo
            }
            UsuarioActual(nuevoId, rol)
        }
        existente.rol == rol -> existente
        else -> throw ErrorApi.Conflicto("Esta cuenta ya está registrada como ${existente.rol.name.lowercase()}.")
    }
}

/** Busca el usuario sin exigir rol (para GET /v1/sesion). */
suspend fun usuarioRegistrado(bd: BaseDatos, identidad: Identidad): UsuarioActual? = bd.tx { buscar(identidad.firebaseUid) }
