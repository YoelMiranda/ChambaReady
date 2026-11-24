package pe.edu.esan.chambaready.api.ia

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.zip.ZipInputStream

/*
 * Lo que la IA debe devolver, cómo se valida y cuánto cuesta. Todo es puro (sin red ni base de
 * datos) y está probado en ContenidoIaTest. NUNCA se confía en la IA sin validar: puede inventar
 * habilidades, repetir opciones o marcar una respuesta correcta que no existe.
 */

@Serializable data class PreguntaIa(val enunciado: String, val opciones: List<String>, val indiceCorrecto: Int)

@Serializable data class PreguntasIa(val preguntas: List<PreguntaIa>)

@Serializable data class HabilidadCvIa(val habilidad: String, val nivel: String)

@Serializable data class CvIa(val habilidades: List<HabilidadCvIa>, val proyectos: List<String>)

/** Habilidad del catálogo encontrada en el CV, con el nivel que sugiere la IA (el usuario lo confirma en P03). */
data class HabilidadDetectada(val habilidadId: String, val nombre: String, val nivel: String)

data class CvLeido(val habilidades: List<HabilidadDetectada>, val proyectos: List<String>)

/** Esquemas de respuesta (formato OpenAPI que entiende Gemini en responseSchema). */
object Esquemas {
    private fun texto() = buildJsonObject { put("type", "STRING") }

    private fun lista(items: JsonObject) = buildJsonObject {
        put("type", "ARRAY")
        put("items", items)
    }

    private fun objeto(vararg campos: Pair<String, JsonObject>) = buildJsonObject {
        put("type", "OBJECT")
        put("properties", buildJsonObject { campos.forEach { (nombre, tipo) -> put(nombre, tipo) } })
        put("required", JsonArray(campos.map { JsonPrimitive(it.first) }))
    }

    /** Lista de preguntas de opción múltiple con 4 opciones y el índice de la correcta. */
    val preguntas: JsonObject = objeto(
        "preguntas" to lista(
            objeto(
                "enunciado" to texto(),
                "opciones" to lista(texto()),
                "indiceCorrecto" to buildJsonObject { put("type", "INTEGER") },
            ),
        ),
    )

    /** Habilidades (nombre + nivel) y proyectos que aparecen en el CV. */
    val cv: JsonObject = objeto(
        "habilidades" to lista(
            objeto(
                "habilidad" to texto(),
                "nivel" to buildJsonObject {
                    put("type", "STRING")
                    put("enum", JsonArray(listOf("BASICO", "INTERMEDIO", "AVANZADO").map { JsonPrimitive(it) }))
                },
            ),
        ),
        "proyectos" to lista(texto()),
    )
}

object ValidacionIa {

    private val json = Json { ignoreUnknownKeys = true }
    private val nivelesValidos = setOf("BASICO", "INTERMEDIO", "AVANZADO")

    const val MAX_HABILIDADES_CV = 15
    const val MAX_PROYECTOS_CV = 5

    /**
     * Lee y limpia las preguntas. Descarta las que no tengan exactamente 4 opciones distintas, un
     * índice correcto válido o un enunciado razonable.
     * @throws ErrorIa si después de limpiar quedan menos de [cantidad].
     */
    fun preguntas(texto: String, cantidad: Int): List<PreguntaIa> {
        val leidas = try {
            json.decodeFromString<PreguntasIa>(texto).preguntas
        } catch (e: Exception) {
            throw ErrorIa("La IA no devolvió preguntas con el formato pedido.", e)
        }
        val validas = leidas.mapNotNull { p ->
            val enunciado = p.enunciado.trim()
            val opciones = p.opciones.map { it.trim() }
            val ok = enunciado.length in 10..300 &&
                opciones.size == 4 &&
                opciones.all { it.isNotEmpty() && it.length <= 150 } &&
                opciones.map { it.lowercase() }.toSet().size == 4 &&
                p.indiceCorrecto in 0..3
            if (ok) PreguntaIa(enunciado, opciones, p.indiceCorrecto) else null
        }
        if (validas.size < cantidad) throw ErrorIa("La IA devolvió ${validas.size} preguntas válidas de $cantidad.")
        return validas.take(cantidad)
    }

    /**
     * Lee el CV analizado. Solo se aceptan habilidades del catálogo (por nombre, sin tildes ni
     * mayúsculas): así la IA no puede inventar habilidades que luego no existan en las vacantes.
     * @param catalogo id de habilidad → nombre.
     */
    fun cv(texto: String, catalogo: Map<String, String>): CvLeido {
        val leido = try {
            json.decodeFromString<CvIa>(texto)
        } catch (e: Exception) {
            throw ErrorIa("La IA no devolvió el CV con el formato pedido.", e)
        }
        val porNombre = catalogo.entries.associateBy({ Textos.normalizar(it.value) }, { it.key })
        val habilidades = leido.habilidades.mapNotNull { h ->
            val id = porNombre[Textos.normalizar(h.habilidad)] ?: catalogo.keys.firstOrNull { it == Textos.normalizar(h.habilidad) }
            id?.let { HabilidadDetectada(it, catalogo.getValue(it), h.nivel.takeIf { n -> n in nivelesValidos } ?: "BASICO") }
        }.distinctBy { it.habilidadId }.take(MAX_HABILIDADES_CV)
        val proyectos = leido.proyectos.map { it.trim().take(120) }.filter { it.isNotEmpty() }.distinct().take(MAX_PROYECTOS_CV)
        return CvLeido(habilidades, proyectos)
    }
}

/**
 * Regla 14: costo de cada llamada. Precios en US$ por millón de tokens (entrada, salida), tomados
 * de https://ai.google.dev/gemini-api/docs/pricing. Un modelo que no está aquí se registra con
 * costo 0 y se debe agregar.
 */
object CostosIa {
    private val precios = mapOf(
        "gemini-3.5-flash-lite" to (0.30 to 2.50),
        "gemini-2.5-flash-lite" to (0.10 to 0.40),
    )

    fun costoUsd(modelo: String, tokensEntrada: Int, tokensSalida: Int): BigDecimal {
        val (entrada, salida) = precios[modelo] ?: return BigDecimal.ZERO
        val total = (tokensEntrada * entrada + tokensSalida * salida) / 1_000_000.0
        return BigDecimal.valueOf(total).setScale(6, RoundingMode.HALF_UP)
    }
}

object Textos {
    /** "Power BI" → "power-bi", "Estadística" → "estadistica". Sirve para comparar nombres. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").replace(Regex("[^a-z0-9]+"), "-").trim('-')

    /**
     * Texto de un .docx (es un ZIP con word/document.xml). Gemini lee PDF directamente, pero no
     * Word, así que de Word se le manda el texto. Devuelve null si el archivo no es un .docx válido.
     */
    fun textoDeDocx(bytes: ByteArray): String? = try {
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.firstOrNull { it.name == "word/document.xml" } ?: return null
            val xml = zip.readBytes().toString(Charsets.UTF_8)
            xml.replace(Regex("</w:p>"), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
                .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
                .take(20_000)
        }
    } catch (_: java.io.IOException) {
        null
    }

    /** Sin IA: busca los nombres del catálogo dentro del texto del CV (solo para el modo simulado). */
    fun habilidadesEnTexto(texto: String, catalogo: Map<String, String>): List<HabilidadDetectada> {
        val normalizado = "-" + normalizar(texto) + "-"
        return catalogo.filter { (_, nombre) -> normalizado.contains("-" + normalizar(nombre) + "-") }
            .map { (id, nombre) -> HabilidadDetectada(id, nombre, "BASICO") }
    }
}
