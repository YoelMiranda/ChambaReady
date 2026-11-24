package pe.edu.esan.chambaready.core.data.mock

import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.CursoExterno
import pe.edu.esan.chambaready.core.domain.modelo.Habilidad
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

/** Contenido de prueba (F1–F3). Desde F4 lo reemplaza la versión que llama a la API. */
@Singleton
class MockContenidoRepository @Inject constructor(
    private val reloj: Clock,
) : ContenidoRepository {

    override fun universidades(): List<String> = MockDatos.universidades

    override fun habilidad(id: String): Habilidad? = MockDatos.habilidades.firstOrNull { it.id == id }

    override fun habilidadesDetectadasEnCv(): List<Habilidad> = MockDatos.habilidadesDelCv.mapNotNull { habilidad(it) }

    override fun proyectosDetectadosEnCv(): List<String> = MockDatos.proyectosDelCv

    override fun cursos(): Map<String, CursoExterno> = MockDatos.cursos

    override fun miniTest(moduloId: String): List<PreguntaOpcion> = MockDatos.miniTest(moduloId).map(::mezclar)

    override fun preguntasAutoevaluacion(area: AreaInteres): List<PreguntaOpcion> =
        MockDatos.preguntasAutoevaluacion(area).map(::mezclar)

    override fun preguntasFiltro(vacanteId: String): List<PreguntaOpcion> =
        MockDatos.vacantes(LocalDate.now(reloj)).firstOrNull { it.resumen.id == vacanteId }
            ?.let { MockDatos.preguntasFiltro(it) }
            ?.map(::mezclar)
            .orEmpty()

    override fun notificaciones(ahora: LocalDateTime): List<Notificacion> = MockDatos.notificaciones(ahora)

    override fun precioPremiumCentimos(): Long = 999

    /** Estimado de prueba: más aciertos, mejor puesto. En F4 lo calcula el backend comparando con otros estudiantes del área. */
    override fun topPorcentaje(area: AreaInteres, correctas: Int, total: Int): Int {
        if (total <= 0) return 99
        return (100 - 85.0 * correctas / total).roundToInt().coerceIn(1, 99)
    }

    /**
     * Mezcla las opciones con una semilla fija por pregunta (siempre sale el mismo orden) y deja
     * "No lo sé" al final. En los datos de prueba la correcta suele estar primero; así no es obvio.
     */
    private fun mezclar(pregunta: PreguntaOpcion): PreguntaOpcion {
        val noLoSe = pregunta.opciones.lastOrNull()?.takeIf { it == "No lo sé" }
        val opciones = pregunta.opciones.mapIndexed { i, texto -> i to texto }.filter { it.second != noLoSe }
        val mezcladas = opciones.shuffled(Random(pregunta.id.hashCode()))
        val finales = mezcladas + listOfNotNull(noLoSe?.let { pregunta.opciones.lastIndex to it })
        return pregunta.copy(
            opciones = finales.map { it.second },
            indiceCorrecto = finales.indexOfFirst { it.first == pregunta.indiceCorrecto },
        )
    }
}
