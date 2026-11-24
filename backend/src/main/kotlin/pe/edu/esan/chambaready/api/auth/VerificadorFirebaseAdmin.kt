package pe.edu.esan.chambaready.api.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

/**
 * Verifica ID tokens con Firebase Admin SDK. Necesita la cuenta de servicio del proyecto Firebase
 * (variable FIREBASE_CREDENCIALES_JSON, el JSON completo en una línea). Nunca subas ese JSON al repo.
 */
class VerificadorFirebaseAdmin(credencialesJson: String, proyectoId: String) : VerificadorToken {

    private val auth: FirebaseAuth

    init {
        val opciones = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(credencialesJson.byteInputStream()))
            .setProjectId(proyectoId)
            .build()
        val app = FirebaseApp.getApps().firstOrNull { it.name == NOMBRE_APP } ?: FirebaseApp.initializeApp(opciones, NOMBRE_APP)
        auth = FirebaseAuth.getInstance(app)
    }

    /** Lanza excepción si el token es inválido; [AutenticadorFirebase] la convierte en 401. */
    override fun verificar(token: String): Identidad {
        val decodificado = auth.verifyIdToken(token)
        return Identidad(
            firebaseUid = decodificado.uid,
            nombre = decodificado.name.orEmpty(),
            correo = decodificado.email.orEmpty(),
        )
    }

    private companion object {
        const val NOMBRE_APP = "chambaready-api"
    }
}
