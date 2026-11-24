package pe.edu.esan.chambaready.api.config

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Datos de conexión JDBC (separados de la URL porque el driver no acepta usuario:clave@host). */
data class ConexionBd(
    val jdbcUrl: String,
    val usuario: String?,
    val clave: String?,
)

/**
 * Toda la configuración sale de variables de entorno (nada secreto en el repo). Ver `.env.example`.
 * En Heroku se definen en Settings → Config Vars.
 */
data class Configuracion(
    val puerto: Int,
    val baseDatos: ConexionBd,
    /** Tamaño del pool: chico, porque el plan gratis de Neon limita conexiones y la memoria es poca. */
    val maxConexiones: Int,
    /**
     * Solo para desarrollo local: acepta los encabezados X-Demo-Usuario / X-Demo-Rol en lugar del token
     * de Firebase (que llega en F5). Apagado por defecto para que un deploy nunca quede abierto.
     */
    val modoDesarrollo: Boolean,
    /** Token para el endpoint de administración (costos de IA). Sin él, ese endpoint responde 404. */
    val tokenAdmin: String?,
    /** Orígenes permitidos para la web de empresa (CORS), separados por coma. */
    val origenesCors: List<String>,
    /** Carga datos de prueba (empresa, vacantes) al iniciar. Solo para desarrollo. */
    val cargarDatosDemo: Boolean,
    /** Proyecto de Firebase (F5). Con las credenciales, la API valida los ID tokens. */
    val firebaseProyectoId: String? = null,
    /** JSON de la cuenta de servicio de Firebase, en una línea. Secreto: solo en variables de entorno. */
    val firebaseCredencialesJson: String? = null,
    /** Clave de Gemini (F6). Sin ella, la IA usa contenido simulado y todo sigue funcionando. Secreta. */
    val geminiApiKey: String? = null,
    /** Modelo para leer CVs. */
    val geminiModelo: String = "gemini-3.5-flash-lite",
    /** Modelo más barato para preguntas cortas. */
    val geminiModeloSimple: String = "gemini-2.5-flash-lite",
) {
    companion object {
        fun desdeEntorno(entorno: Map<String, String> = System.getenv()): Configuracion {
            val databaseUrl = entorno["DATABASE_URL"]
                ?: error("Falta la variable DATABASE_URL (la cadena de conexión de Neon).")
            return Configuracion(
                puerto = entorno["PORT"]?.toIntOrNull() ?: 8080,
                baseDatos = convertirDatabaseUrl(databaseUrl),
                maxConexiones = entorno["DB_MAX_CONEXIONES"]?.toIntOrNull() ?: 3,
                modoDesarrollo = entorno["MODO_DESARROLLO"] == "true",
                tokenAdmin = entorno["ADMIN_TOKEN"]?.takeIf { it.length >= 24 },
                origenesCors = entorno["CORS_ORIGENES"].orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() },
                cargarDatosDemo = entorno["CARGAR_DATOS_DEMO"] == "true",
                firebaseProyectoId = entorno["FIREBASE_PROJECT_ID"]?.takeIf { it.isNotBlank() },
                firebaseCredencialesJson = entorno["FIREBASE_CREDENCIALES_JSON"]?.takeIf { it.isNotBlank() },
                geminiApiKey = entorno["GEMINI_API_KEY"]?.takeIf { it.isNotBlank() },
                geminiModelo = entorno["GEMINI_MODEL"]?.takeIf { it.isNotBlank() } ?: "gemini-3.5-flash-lite",
                geminiModeloSimple = entorno["GEMINI_MODEL_SIMPLE"]?.takeIf { it.isNotBlank() } ?: "gemini-2.5-flash-lite",
            )
        }

        /**
         * Neon y Heroku entregan `postgresql://usuario:clave@host/base?sslmode=require`. El driver JDBC
         * necesita `jdbc:postgresql://host/base?sslmode=require` y el usuario y la clave aparte.
         * Si ya viene como `jdbc:...` se usa tal cual. Sin `sslmode`, se fuerza `require`.
         */
        fun convertirDatabaseUrl(url: String): ConexionBd {
            if (url.startsWith("jdbc:")) return ConexionBd(url, null, null)
            val uri = URI(url.replaceFirst("postgres://", "postgresql://"))
            require(uri.scheme == "postgresql") { "DATABASE_URL debe empezar con postgresql:// o jdbc:postgresql://" }
            val (usuario, clave) = uri.rawUserInfo?.split(":", limit = 2)?.let { partes ->
                decodificar(partes[0]) to partes.getOrNull(1)?.let(::decodificar)
            } ?: (null to null)
            val puerto = if (uri.port > 0) ":${uri.port}" else ""
            val parametros = uri.rawQuery?.split("&")?.filter { it.isNotBlank() }.orEmpty().toMutableList()
            if (parametros.none { it.startsWith("sslmode=") }) parametros += "sslmode=require"
            return ConexionBd("jdbc:postgresql://${uri.host}$puerto${uri.rawPath}?${parametros.joinToString("&")}", usuario, clave)
        }

        private fun decodificar(texto: String) = URLDecoder.decode(texto, StandardCharsets.UTF_8)
    }
}
