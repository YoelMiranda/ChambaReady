package pe.edu.esan.chambaready.api.ia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * Llama a la API de Gemini (generateContent) con el cliente HTTP que trae Java: no hace falta
 * ninguna librería extra. La clave viaja en el encabezado `x-goog-api-key`, nunca en la URL
 * (las URLs suelen quedar en los registros).
 *
 * Documentación: https://ai.google.dev/api/generate-content
 */
class GeminiHttp(
    private val apiKey: String,
    private val cliente: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : ProveedorIa {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generar(pedido: PedidoIa): RespuestaIa = withContext(Dispatchers.IO) {
        val solicitud = HttpRequest.newBuilder(URI("$URL_BASE/${pedido.modelo}:generateContent"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo(pedido).toString()))
            .build()
        val respuesta = try {
            cliente.send(solicitud, HttpResponse.BodyHandlers.ofString())
        } catch (e: java.io.IOException) {
            throw ErrorIa("No hubo conexión con Gemini.", e)
        }
        if (respuesta.statusCode() != 200) {
            // No se incluye el cuerpo completo: podría repetir partes del pedido (datos del CV).
            throw ErrorIa("Gemini respondió ${respuesta.statusCode()}.")
        }
        leerRespuesta(respuesta.body())
    }

    /** Arma el JSON del pedido: instrucciones, contenido y el esquema de la respuesta. */
    internal fun cuerpo(pedido: PedidoIa): JsonObject = buildJsonObject {
        putJsonObject("systemInstruction") {
            putJsonArray("parts") { add(buildJsonObject { put("text", pedido.instrucciones) }) }
        }
        putJsonArray("contents") {
            add(
                buildJsonObject {
                    put("role", "user")
                    put(
                        "parts",
                        buildJsonArray {
                            pedido.partes.forEach { parte ->
                                when (parte) {
                                    is ParteIa.Texto -> add(buildJsonObject { put("text", parte.texto) })
                                    is ParteIa.Archivo -> add(
                                        buildJsonObject {
                                            putJsonObject("inlineData") {
                                                put("mimeType", parte.mime)
                                                put("data", Base64.getEncoder().encodeToString(parte.bytes))
                                            }
                                        },
                                    )
                                }
                            }
                        },
                    )
                },
            )
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            put("responseSchema", pedido.esquema)
            put("temperature", 0.4)
        }
    }

    /** Saca el texto (que es el JSON pedido) y los tokens usados. */
    internal fun leerRespuesta(texto: String): RespuestaIa {
        try {
            val raiz = json.parseToJsonElement(texto).jsonObject
            val parte = raiz.getValue("candidates").jsonArray.first().jsonObject
                .getValue("content").jsonObject
                .getValue("parts").jsonArray.first().jsonObject
            val uso = raiz["usageMetadata"]?.jsonObject
            return RespuestaIa(
                json = parte.getValue("text").jsonPrimitive.content,
                tokensEntrada = uso?.get("promptTokenCount")?.jsonPrimitive?.int ?: 0,
                tokensSalida = uso?.get("candidatesTokenCount")?.jsonPrimitive?.int ?: 0,
            )
        } catch (e: Exception) {
            // Por ejemplo, Gemini bloqueó la respuesta y no hay "candidates".
            throw ErrorIa("Gemini devolvió una respuesta sin contenido.", e)
        }
    }

    private companion object {
        const val URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
