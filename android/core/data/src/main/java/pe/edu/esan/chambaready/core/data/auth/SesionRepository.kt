package pe.edu.esan.chambaready.core.data.auth

import pe.edu.esan.chambaready.core.data.remoto.ProveedorToken

/** Error de inicio de sesión con un mensaje en español listo para mostrar. */
class ErrorAuth(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/**
 * Cuenta del usuario en Firebase Auth (correo y contraseña). Solo sabe QUIÉN es; el rol
 * y los datos los guarda el backend (ver `CuentaRepository`).
 *
 * También es el [ProveedorToken] que usa el interceptor de OkHttp.
 */
interface SesionRepository : ProveedorToken {

    /** false en MODO DEMO (sin google-services.json): no hay login. */
    val disponible: Boolean

    /** ¿Hay una cuenta con la sesión iniciada en este teléfono? */
    fun haySesion(): Boolean

    /** Correo de la cuenta actual (o "" si no hay). */
    fun correoActual(): String

    suspend fun crearCuenta(correo: String, contrasena: String)

    suspend fun iniciarSesion(correo: String, contrasena: String)

    suspend fun enviarCorreoDeRecuperacion(correo: String)

    fun cerrarSesion()
}

/** MODO DEMO: sin Firebase. Las pantallas de login muestran cómo configurarlo. */
class SesionDemo : SesionRepository {
    override val disponible = false
    override fun haySesion() = false
    override fun correoActual() = ""
    override suspend fun token(): String? = null
    override suspend fun crearCuenta(correo: String, contrasena: String) {
        sinFirebase()
    }
    override suspend fun iniciarSesion(correo: String, contrasena: String) {
        sinFirebase()
    }
    override suspend fun enviarCorreoDeRecuperacion(correo: String) {
        sinFirebase()
    }
    override fun cerrarSesion() = Unit

    private fun sinFirebase(): Nothing =
        throw ErrorAuth("El login no está configurado en esta versión. Agrega app/google-services.json (ver docs/despliegue.md).")
}
