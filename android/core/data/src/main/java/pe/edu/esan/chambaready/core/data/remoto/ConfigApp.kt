package pe.edu.esan.chambaready.core.data.remoto

/**
 * Configuración que entrega el módulo :app (sale de BuildConfig y local.properties).
 *
 * @property usaBackend true si la app tiene google-services.json: login real con Firebase y datos
 *   de la API. false = MODO DEMO, todo con datos de prueba en el teléfono (como en F1–F4).
 * @property depuracion true en builds de depuración: registra los pedidos HTTP en Logcat
 *   (sin el token, que se oculta).
 */
data class ConfigApp(
    val apiBaseUrl: String,
    val usaBackend: Boolean,
    val depuracion: Boolean,
)
