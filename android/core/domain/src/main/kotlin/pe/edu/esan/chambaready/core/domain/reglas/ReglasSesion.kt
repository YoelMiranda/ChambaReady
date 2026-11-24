package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.RolUsuario

/** A dónde va el usuario después de iniciar sesión (F5). */
enum class DestinoTrasLogin {
    /** Ya es practicante registrado: entra a la app. */
    ENTRAR,

    /** Tiene cuenta de Firebase, pero nunca completó el registro en ChambaReady (C02). */
    COMPLETAR_REGISTRO,

    /** La cuenta es de una empresa: en la app no entra, usa la web de empresas. */
    USAR_WEB_EMPRESA,
}

/**
 * Reglas del inicio de sesión y de la contraseña. Funciones puras, probadas en `ReglasSesionTest`.
 * El rol lo guarda el backend en POST /v1/registro y nunca se cambia desde el cliente.
 */
object ReglasSesion {

    /** Firebase acepta 6 caracteres; pedimos 8 con letras y números para que sea más segura. */
    const val CONTRASENA_MINIMA = 8

    /** Mensaje de error de la contraseña, o null si está bien. */
    fun validarContrasena(contrasena: String): String? = when {
        contrasena.length < CONTRASENA_MINIMA -> "Usa al menos $CONTRASENA_MINIMA caracteres."
        contrasena.none { it.isLetter() } || contrasena.none { it.isDigit() } -> "Combina letras y números."
        else -> null
    }

    /**
     * Decide el destino con la respuesta de GET /v1/sesion.
     * @param rol el que guardó el backend; null si la cuenta aún no se registró.
     */
    fun destinoTrasLogin(registrado: Boolean, rol: RolUsuario?): DestinoTrasLogin = when {
        !registrado || rol == null -> DestinoTrasLogin.COMPLETAR_REGISTRO
        rol == RolUsuario.EMPRESA -> DestinoTrasLogin.USAR_WEB_EMPRESA
        else -> DestinoTrasLogin.ENTRAR
    }

    /** Convierte el texto del backend ("PRACTICANTE", "EMPRESA") en el enum; null si no lo reconoce. */
    fun rolDesdeTexto(texto: String?): RolUsuario? = RolUsuario.entries.firstOrNull { it.name == texto }
}
