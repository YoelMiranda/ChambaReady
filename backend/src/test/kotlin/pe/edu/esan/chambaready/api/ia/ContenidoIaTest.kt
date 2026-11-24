package pe.edu.esan.chambaready.api.ia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Lo que responde la IA nunca se usa sin validar. */
class ContenidoIaTest {

    private val catalogo = mapOf("sql" to "SQL", "powerbi" to "Power BI", "estadistica" to "Estadística")

    @Test
    fun preguntas_seDescartanLasMalFormadas() {
        val texto = """
            {"preguntas": [
              {"enunciado": "¿Qué cláusula filtra filas?", "opciones": ["WHERE", "HAVING", "ORDER BY", "LIMIT"], "indiceCorrecto": 0},
              {"enunciado": "Solo tres opciones aquí", "opciones": ["a", "b", "c"], "indiceCorrecto": 0},
              {"enunciado": "Opciones repetidas, no sirve", "opciones": ["a", "A", "b", "c"], "indiceCorrecto": 1},
              {"enunciado": "Índice correcto que no existe", "opciones": ["a", "b", "c", "d"], "indiceCorrecto": 7},
              {"enunciado": "  Con espacios de más   ", "opciones": [" uno ", "dos", "tres", "cuatro"], "indiceCorrecto": 3}
            ]}
        """.trimIndent()
        val validas = ValidacionIa.preguntas(texto, 2)
        assertEquals(2, validas.size)
        assertEquals("Con espacios de más", validas[1].enunciado)
        assertEquals("uno", validas[1].opciones[0])
    }

    @Test
    fun preguntas_sinSuficientesValidasEsError() {
        assertFailsWith<ErrorIa> { ValidacionIa.preguntas("""{"preguntas": []}""", 1) }
        assertFailsWith<ErrorIa> { ValidacionIa.preguntas("no es json", 1) }
    }

    @Test
    fun cv_soloAceptaHabilidadesDelCatalogo() {
        val texto = """
            {"habilidades": [
              {"habilidad": "power bi", "nivel": "INTERMEDIO"},
              {"habilidad": "Estadistica", "nivel": "EXPERTO"},
              {"habilidad": "Astrología", "nivel": "AVANZADO"},
              {"habilidad": "SQL", "nivel": "BASICO"},
              {"habilidad": "sql", "nivel": "AVANZADO"}
            ], "proyectos": ["  Tablero de ventas  ", "", "Tablero de ventas"]}
        """.trimIndent()
        val cv = ValidacionIa.cv(texto, catalogo)
        assertEquals(listOf("powerbi", "estadistica", "sql"), cv.habilidades.map { it.habilidadId })
        assertEquals("INTERMEDIO", cv.habilidades[0].nivel)
        assertEquals("BASICO", cv.habilidades[1].nivel, "Un nivel inventado se cambia por BASICO")
        assertEquals(listOf("Tablero de ventas"), cv.proyectos)
    }

    @Test
    fun costos_segunElModelo() {
        assertEquals(BigDecimal("0.000550"), CostosIa.costoUsd("gemini-3.5-flash-lite", 1000, 100))
        assertEquals(BigDecimal.ZERO, CostosIa.costoUsd("modelo-desconocido", 1000, 100))
    }

    @Test
    fun docx_seLeeElTextoYSinIaSeBuscanLasHabilidades() {
        val xml = "<w:document><w:body><w:p><w:r><w:t>Manejo SQL y Power BI</w:t></w:r></w:p><w:p><w:t>Curso de Estadística &amp; datos</w:t></w:p></w:body></w:document>"
        val docx = ByteArrayOutputStream().also { salida ->
            ZipOutputStream(salida).use { zip ->
                zip.putNextEntry(ZipEntry("word/document.xml"))
                zip.write(xml.toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val texto = Textos.textoDeDocx(docx)
        assertEquals("Manejo SQL y Power BI\nCurso de Estadística & datos", texto)
        assertEquals(setOf("sql", "powerbi", "estadistica"), Textos.habilidadesEnTexto(texto!!, catalogo).map { it.habilidadId }.toSet())
        assertEquals(null, Textos.textoDeDocx("no es un zip".toByteArray()))
    }

    @Test
    fun habilidadesEnTexto_noConfundePalabrasParecidas() {
        // "SQLite" no es "SQL": se comparan palabras completas.
        assertTrue(Textos.habilidadesEnTexto("Usé SQLite en un proyecto", catalogo).isEmpty())
    }

    @Test
    fun gemini_elPedidoLlevaElEsquemaYElArchivoPeroNoLaClave() {
        val gemini = GeminiHttp("CLAVE-SECRETA")
        val pedido = PedidoIa("LEER_CV", "gemini-3.5-flash-lite", "Instrucciones", listOf(ParteIa.Archivo("application/pdf", "%PDF".toByteArray())), Esquemas.cv)
        val cuerpo = gemini.cuerpo(pedido).toString()
        assertTrue(cuerpo.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(cuerpo.contains("\"inlineData\":{\"mimeType\":\"application/pdf\",\"data\":\"JVBERg==\"}"))
        assertFalse(cuerpo.contains("CLAVE-SECRETA"), "La clave va en un encabezado, nunca en el cuerpo")
        assertNotNull(Json.parseToJsonElement(cuerpo).jsonObject["systemInstruction"])
    }

    @Test
    fun gemini_seLeenElTextoYLosTokens() {
        val respuesta = GeminiHttp("x").leerRespuesta(
            """{"candidates":[{"content":{"parts":[{"text":"{\"preguntas\":[]}"}]}}],"usageMetadata":{"promptTokenCount":120,"candidatesTokenCount":45}}""",
        )
        assertEquals("""{"preguntas":[]}""", respuesta.json)
        assertEquals(120, respuesta.tokensEntrada)
        assertEquals(45, respuesta.tokensSalida)
        assertFailsWith<ErrorIa> { GeminiHttp("x").leerRespuesta("""{"promptFeedback":{"blockReason":"SAFETY"}}""") }
    }

    @Test
    fun contenidoSimulado_todasLasPreguntasPasanLaValidacion() {
        val ids = listOf("sql", "excel", "powerbi", "python", "estadistica", "kotlin", "git", "marketing", "ads", "finanzas", "comunicacion", "equipo", "nueva-habilidad")
        ids.forEach { id ->
            val preguntas = ContenidoSimulado.miniTest(id, "Habilidad $id")
            val json = Json.encodeToString(PreguntasIa.serializer(), PreguntasIa(preguntas))
            assertEquals(3, ValidacionIa.preguntas(json, 3).size, "Banco de $id")
        }
        // La posición de la respuesta correcta varía (no siempre la primera).
        assertTrue(ContenidoSimulado.miniTest("sql", "SQL").map { it.indiceCorrecto }.toSet().size > 1)
    }
}
