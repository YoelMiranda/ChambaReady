package pe.edu.esan.chambaready.api.ia

import kotlinx.serialization.json.JsonObject

/** Un trozo del mensaje para la IA: texto o un archivo (por ejemplo, el CV en PDF). */
sealed interface ParteIa {
    data class Texto(val texto: String) : ParteIa

    class Archivo(val mime: String, val bytes: ByteArray) : ParteIa
}

/**
 * Lo que se le pide a la IA.
 * @property operacion nombre corto para el registro de costos (regla 14): LEER_CV, MINI_TEST…
 * @property esquema forma exacta del JSON que debe responder (Gemini lo respeta con responseSchema).
 */
data class PedidoIa(
    val operacion: String,
    val modelo: String,
    val instrucciones: String,
    val partes: List<ParteIa>,
    val esquema: JsonObject,
)

/** JSON que respondió la IA y los tokens que usó (para calcular el costo). */
data class RespuestaIa(val json: String, val tokensEntrada: Int, val tokensSalida: Int)

/** Falla al hablar con la IA. Quien la llama decide qué hacer (normalmente, usar el contenido simulado). */
class ErrorIa(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/**
 * Quien genera el contenido. La implementación real es [GeminiHttp]; las pruebas usan una falsa.
 * IMPORTANTE: solo el backend habla con Gemini. La app y la web nunca tienen la clave.
 */
fun interface ProveedorIa {
    suspend fun generar(pedido: PedidoIa): RespuestaIa
}
