package pe.edu.esan.chambaready.api.ia

import kotlinx.coroutines.CancellationException
import org.jetbrains.exposed.v1.jdbc.insert
import org.slf4j.LoggerFactory
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.UsoIa
import java.util.UUID

/**
 * Todo lo que ChambaReady le pide a la IA (F6). Si no hay clave de Gemini ([proveedor] = null) o la
 * llamada falla, usa [ContenidoSimulado]: la app nunca se queda sin preguntas por culpa de la IA.
 *
 * Las llamadas a Gemini se hacen FUERA de las transacciones de la base: pueden tardar varios segundos
 * y no deben tener una conexión ocupada mientras tanto (el plan gratis de Neon da pocas).
 *
 * @param modelo para tareas que necesitan "pensar" (leer un CV).
 * @param modeloSimple más barato, para tareas cortas (preguntas).
 */
class ServicioIa(
    private val bd: BaseDatos,
    private val proveedor: ProveedorIa?,
    private val modelo: String,
    private val modeloSimple: String,
) {
    private val log = LoggerFactory.getLogger(ServicioIa::class.java)

    /** true si hay Gemini configurado (lo muestran la web y la app para avisar que es simulado). */
    val conIa: Boolean get() = proveedor != null

    /**
     * P02: habilidades y proyectos del CV. Gemini lee el PDF directamente; de un .docx se le manda el
     * texto. Sin IA, busca los nombres del catálogo en el texto del .docx (del PDF no puede).
     */
    suspend fun leerCv(usuarioId: String, mime: String, bytes: ByteArray, catalogo: Map<String, String>): CvLeido {
        val textoDocx = if (mime == MIME_DOCX) Textos.textoDeDocx(bytes) else null
        val parte = when {
            mime == MIME_PDF -> ParteIa.Archivo(MIME_PDF, bytes)
            textoDocx != null -> ParteIa.Texto(textoDocx)
            else -> null
        }
        if (parte != null) {
            val pedido = PedidoIa(
                operacion = "LEER_CV",
                modelo = modelo,
                instrucciones = """
                    Eres un asistente de empleabilidad para estudiantes universitarios del Perú.
                    Lee el CV y devuelve SOLO habilidades de esta lista (usa el nombre exacto): ${catalogo.values.joinToString(", ")}.
                    Para cada una estima el nivel (BASICO, INTERMEDIO o AVANZADO) según la evidencia del CV
                    (cursos, proyectos, experiencia). Si no hay evidencia clara, usa BASICO. No inventes.
                    En "proyectos" pon hasta 5 proyectos o experiencias, en frases cortas en español.
                """.trimIndent(),
                partes = listOf(ParteIa.Texto("CV del estudiante:"), parte),
                esquema = Esquemas.cv,
            )
            llamar(pedido, usuarioId = usuarioId) { ValidacionIa.cv(it, catalogo) }?.let { return it }
        }
        return textoDocx?.let { CvLeido(Textos.habilidadesEnTexto(it, catalogo), emptyList()) } ?: ContenidoSimulado.cvVacio
    }

    /** P15: 3 preguntas para comprobar el [nivel] en una habilidad. */
    suspend fun preguntasMiniTest(habilidadId: String, nombre: String, nivel: String, usuarioId: String?): List<PreguntaIa> {
        val pedido = PedidoIa(
            operacion = "MINI_TEST",
            modelo = modeloSimple,
            instrucciones = """
                Crea exactamente 3 preguntas de opción múltiple en español para comprobar si un estudiante
                universitario tiene nivel $nivel en "$nombre". Cada pregunta tiene 4 opciones, una sola correcta,
                sin opciones del tipo "todas las anteriores". Preguntas prácticas, de situaciones reales de una práctica
                preprofesional. Varía la posición de la respuesta correcta.
            """.trimIndent(),
            partes = listOf(ParteIa.Texto("Habilidad: $nombre. Nivel: $nivel.")),
            esquema = Esquemas.preguntas,
        )
        return llamar(pedido, usuarioId = usuarioId) { ValidacionIa.preguntas(it, 3) }
            ?: ContenidoSimulado.miniTest(habilidadId, nombre)
    }

    /**
     * E03: preguntas filtro sugeridas, una por requisito (máximo 2). La empresa las revisa y puede
     * editarlas antes de publicar.
     * @param requisitos (id de habilidad, nombre y nivel exigido), en orden de importancia.
     */
    suspend fun preguntasFiltro(puesto: String, requisitos: List<Triple<String, String, String>>, empresaId: String?): List<PreguntaIa> {
        val elegidos = requisitos.take(2)
        if (elegidos.isEmpty()) return emptyList()
        val pedido = PedidoIa(
            operacion = "PREGUNTAS_FILTRO",
            modelo = modeloSimple,
            instrucciones = """
                Crea exactamente ${elegidos.size} preguntas de opción múltiple en español para filtrar postulantes a una
                práctica preprofesional de "$puesto". Una pregunta por requisito, en el mismo orden. Cada una con 4 opciones
                y una sola correcta. Que se pueda responder en 45 segundos. Varía la posición de la respuesta correcta.
            """.trimIndent(),
            partes = listOf(ParteIa.Texto(elegidos.joinToString("\n") { (_, nombre, nivel) -> "- $nombre (nivel $nivel)" })),
            esquema = Esquemas.preguntas,
        )
        return llamar(pedido, empresaId = empresaId) { ValidacionIa.preguntas(it, elegidos.size) }
            ?: ContenidoSimulado.preguntasFiltro(elegidos.map { it.first to it.second })
    }

    /**
     * Hace la llamada, valida la respuesta con [validar] y registra los tokens y el costo (regla 14).
     * Devuelve null si no hay IA o algo falló: el que llama usa entonces el contenido simulado.
     */
    private suspend fun <T> llamar(pedido: PedidoIa, usuarioId: String? = null, empresaId: String? = null, validar: (String) -> T): T? {
        val ia = proveedor ?: return null
        return try {
            val respuesta = ia.generar(pedido)
            registrarUso(pedido, respuesta, usuarioId, empresaId)
            validar(respuesta.json)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorIa) {
            // Sin el detalle del pedido: puede contener datos personales del CV.
            log.warn("IA no disponible para ${pedido.operacion} (${e.message}); se usa contenido simulado.")
            null
        }
    }

    private suspend fun registrarUso(pedido: PedidoIa, respuesta: RespuestaIa, usuarioId: String?, empresaId: String?) = bd.tx {
        UsoIa.insert {
            it[id] = UUID.randomUUID().toString()
            it[UsoIa.usuarioId] = usuarioId
            it[UsoIa.empresaId] = empresaId
            it[operacion] = pedido.operacion
            it[modelo] = pedido.modelo
            it[tokensEntrada] = respuesta.tokensEntrada
            it[tokensSalida] = respuesta.tokensSalida
            it[costoUsd] = CostosIa.costoUsd(pedido.modelo, respuesta.tokensEntrada, respuesta.tokensSalida)
        }
    }

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
}
