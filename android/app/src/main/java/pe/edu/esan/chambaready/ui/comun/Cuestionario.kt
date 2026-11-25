package pe.edu.esan.chambaready.ui.comun

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.ui.autoevaluacion.PreguntaConOpciones

/** Segundos por pregunta: el mismo formato que las preguntas filtro de las empresas (P24). */
const val SEGUNDOS_PREGUNTA = 45

/** Dónde va el cuestionario: qué pregunta, cuánto tiempo queda y qué opción eligió. */
data class EstadoCronometro(
    val indice: Int = 0,
    val total: Int = 0,
    val segundos: Int = SEGUNDOS_PREGUNTA,
    val elegida: Int? = null,
    val terminado: Boolean = false,
) {
    val esUltima: Boolean get() = indice >= total - 1
}

/**
 * Recorre [EstadoCronometro.total] preguntas de una en una, con [segundos] para cada una. Si se acaba
 * el tiempo, pasa a la siguiente sin respuesta. Al final llama a [alTerminar] con la opción elegida
 * en cada pregunta (null = sin responder). Lo usan la evaluación de habilidades y el mini-test.
 */
class CronometroPreguntas(
    private val alcance: CoroutineScope,
    private val onCambio: (EstadoCronometro) -> Unit,
    private val alTerminar: (List<Int?>) -> Unit,
    private val segundos: Int = SEGUNDOS_PREGUNTA,
) {
    private var estado = EstadoCronometro(segundos = segundos)
    private val respuestas = mutableListOf<Int?>()
    private var temporizador: Job? = null

    fun empezar(total: Int) {
        respuestas.clear()
        estado = EstadoCronometro(total = total, segundos = segundos)
        iniciarPregunta(0)
    }

    fun elegir(opcion: Int) {
        if (estado.terminado) return
        estado = estado.copy(elegida = opcion)
        onCambio(estado)
    }

    /** "Siguiente" o tiempo agotado: guarda la respuesta (o su ausencia) y avanza. */
    fun siguiente() {
        if (estado.terminado || estado.total == 0) return
        respuestas += estado.elegida
        if (respuestas.size >= estado.total) {
            temporizador?.cancel()
            estado = estado.copy(terminado = true)
            onCambio(estado)
            alTerminar(respuestas.toList())
        } else {
            iniciarPregunta(estado.indice + 1)
        }
    }

    fun detener() {
        temporizador?.cancel()
    }

    private fun iniciarPregunta(indice: Int) {
        estado = estado.copy(indice = indice, segundos = segundos, elegida = null)
        onCambio(estado)
        temporizador?.cancel()
        temporizador = alcance.launch {
            while (estado.segundos > 0) {
                delay(1_000)
                estado = estado.copy(segundos = estado.segundos - 1)
                onCambio(estado)
            }
            siguiente()
        }
    }
}

/** Temporizador + barra + la pregunta. [etiqueta] dice de qué habilidad y nivel es (por ejemplo "SQL · Intermedio"). */
@Composable
fun PreguntaCronometrada(estado: EstadoCronometro, pregunta: PreguntaOpcion, onElegir: (Int) -> Unit, etiqueta: String? = null) {
    val urgente = estado.segundos <= 10
    etiqueta?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Quedan ${estado.segundos} segundos" },
    ) {
        Text(
            "${estado.segundos} s",
            style = MaterialTheme.typography.headlineMedium,
            color = if (urgente) ChambaTema.colores.avisoRojoTexto else MaterialTheme.colorScheme.onBackground,
        )
        Text(
            if (urgente) "  ¡Se acaba el tiempo!" else "  para responder",
            style = MaterialTheme.typography.bodyMedium,
            color = ChambaTema.colores.textoSecundario,
        )
    }
    LinearProgressIndicator(
        progress = { estado.segundos / SEGUNDOS_PREGUNTA.toFloat() },
        color = if (urgente) ChambaTema.colores.avisoRojoTexto else MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
    PreguntaConOpciones(estado.indice + 1, pregunta, estado.elegida, onElegir)
    Text(
        "Si se acaba el tiempo pasamos a la siguiente pregunta.",
        style = MaterialTheme.typography.bodySmall,
        color = ChambaTema.colores.textoSecundario,
    )
}
