package pe.edu.esan.chambaready.core.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.data.local.EstadoGuardado
import pe.edu.esan.chambaready.core.data.local.aDominio
import pe.edu.esan.chambaready.core.data.local.aGuardado
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import java.time.LocalDate

/** Lo que se guarda en DataStore se lee igual: horas, área, Premium, tareas, evidencias, notificaciones… */
class EstadoGuardadoTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun idaYVuelta(estado: EstadoUsuario): EstadoUsuario {
        val texto = json.encodeToString(EstadoGuardado.serializer(), estado.aGuardado())
        return json.decodeFromString(EstadoGuardado.serializer(), texto).aDominio()
    }

    @Test
    fun estadoCompleto_idaYVuelta() {
        val original = MockDatos.estadoDemo(LocalDate.of(2026, 11, 18)).copy(
            premium = true,
            tareasCompletadas = setOf("v1-sql-1"),
            planesActivos = setOf("v1"),
            metas = setOf("v2"),
            notificacionesLeidas = setOf("n1"),
            respuestasFiltro = mapOf("v1" to listOf(true, false)),
            probabilidadInicial = mapOf("v1" to 42),
            avisosActivos = TipoNotificacion.entries.associateWith { it != TipoNotificacion.RANKING },
        )
        assertEquals(original, idaYVuelta(original))
    }

    @Test
    fun estadoVacio_idaYVuelta() {
        assertEquals(EstadoUsuario(), idaYVuelta(EstadoUsuario()))
    }

    @Test
    fun valoresDesconocidos_seIgnoranSinRomper() {
        // Simula un dato guardado por otra versión: un nivel y un día que ya no existen.
        val guardado = EstadoGuardado(
            nivelesDeclarados = mapOf("sql" to "EXPERTO"),
            horasPorDia = mapOf("FERIADO" to 2.0, "MONDAY" to 9.5, "TUESDAY" to 1.5),
            area = "ASTRONAUTA",
        )
        val estado = guardado.aDominio()
        assertEquals(NivelHabilidad.NO_LO_HE_USADO, estado.niveles.declarado("sql"))
        assertEquals(1.5, estado.horasLibres.totalSemanal, 0.0)
        assertTrue(estado.area == null)
    }

    @Test
    fun evidenciaValidada_subeElNivelDemostrado() {
        val demo = MockDatos.estadoDemo(LocalDate.of(2026, 11, 18))
        // La demo tiene Excel avanzado validado y Python en revisión.
        assertEquals(NivelHabilidad.AVANZADO, demo.nivelesEfectivos.demostrado("excel"))
        assertEquals(NivelHabilidad.NO_LO_HE_USADO, demo.nivelesEfectivos.demostrado("python"))
    }
}
