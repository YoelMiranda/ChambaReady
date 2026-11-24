package pe.edu.esan.chambaready.core.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * [SesionRepository] con Firebase Auth. `await()` (de kotlinx-coroutines-play-services) convierte
 * las Task de Firebase en funciones suspend, así no hay callbacks anidados.
 */
class FirebaseSesionRepository(private val auth: FirebaseAuth) : SesionRepository {

    override val disponible = true

    override fun haySesion(): Boolean = auth.currentUser != null

    override fun correoActual(): String = auth.currentUser?.email.orEmpty()

    /** false = usa el token guardado si sigue vigente; Firebase lo renueva solo cuando vence (1 h). */
    override suspend fun token(): String? = auth.currentUser?.getIdToken(false)?.await()?.token

    override suspend fun crearCuenta(correo: String, contrasena: String) = traducirErrores {
        auth.createUserWithEmailAndPassword(correo.trim(), contrasena).await()
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String) = traducirErrores {
        auth.signInWithEmailAndPassword(correo.trim(), contrasena).await()
    }

    override suspend fun enviarCorreoDeRecuperacion(correo: String) = traducirErrores {
        auth.sendPasswordResetEmail(correo.trim()).await()
    }

    override fun cerrarSesion() = auth.signOut()

    /** Ejecuta [bloque] y cambia los errores de Firebase (en inglés) por [ErrorAuth] en español. */
    private suspend fun traducirErrores(bloque: suspend () -> Any?) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            throw ErrorAuth(mensajePara(e.errorCode), e)
        } catch (e: FirebaseNetworkException) {
            throw ErrorAuth("No hay conexión. Revisa tu internet e inténtalo otra vez.", e)
        } catch (e: FirebaseTooManyRequestsException) {
            throw ErrorAuth("Demasiados intentos. Espera unos minutos.", e)
        }
    }

    internal companion object {
        /** Códigos de FirebaseAuthException → mensaje para el usuario. */
        fun mensajePara(codigo: String): String = when (codigo) {
            "ERROR_INVALID_EMAIL" -> "Ese correo no es válido."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo. Inicia sesión."
            "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil."
            // Firebase ya no distingue "usuario no existe" de "contraseña mala" (protege contra
            // la enumeración de correos), así que el mensaje es el mismo.
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Correo o contraseña incorrectos."
            "ERROR_USER_DISABLED" -> "Esta cuenta está desactivada."
            else -> "No pudimos iniciar sesión ($codigo)."
        }
    }
}
