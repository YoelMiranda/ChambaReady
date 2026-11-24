package pe.edu.esan.chambaready.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.data.remoto.PerfilPracticanteDto
import pe.edu.esan.chambaready.core.data.remoto.PostulacionDto
import pe.edu.esan.chambaready.core.data.remoto.RequisitoDto
import pe.edu.esan.chambaready.core.data.remoto.VacanteDto
import pe.edu.esan.chambaready.core.data.remoto.aDominio
import pe.edu.esan.chambaready.core.data.remoto.aDto
import pe.edu.esan.chambaready.core.data.remoto.actualizacionDePerfil
import pe.edu.esan.chambaready.core.data.remoto.estadoParaPracticante
import pe.edu.esan.chambaready.core.data.remoto.horas
import pe.edu.esan.chambaready.core.data.remoto.niveles
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.EstadoPostulacion
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import java.time.DayOfWeek
import java.time.LocalDate

/** Las conversiones entre la API y el dominio no deben perder ni inventar datos. */
class MapeosTest {

    private fun vacante(area: String = "DATA_ANALYTICS", modalidad: String = "HIBRIDA", fecha: String = "2026-12-15") = VacanteDto(
        id = "v1", puesto = "Analista", empresa = "Andes", area = area, modalidad = modalidad, sueldo = "S/ 1,300",
        fechaCierre = fecha, diasRestantes = 21, descripcion = "d", queHara = listOf("a"),
        requisitos = listOf(
            RequisitoDto("r1", "sql", "SQL", "INTERMEDIO", "FILTRO"),
            RequisitoDto("r2", "tableau", "Tableau", "EXPERTO", "DESEABLE"), // nivel que la app no conoce
        ),
        evaluacionActiva = true, probabilidad = 140,
    )

    @Test
    fun `una vacante valida se convierte completa`() {
        val v = vacante().aDominio()!!
        assertEquals(AreaInteres.DATA_ANALYTICS, v.area)
        assertEquals(Modalidad.HIBRIDA, v.resumen.modalidad)
        assertEquals(LocalDate.of(2026, 12, 15), v.resumen.fechaCierre)
        assertEquals("S/ 1,300", v.resumen.sueldo)
        // La probabilidad siempre queda entre 0 y 100.
        assertEquals(100, v.resumen.probabilidad)
        // El requisito con nivel desconocido se omite; el filtro se conserva como filtro.
        assertEquals(1, v.requisitos.size)
        assertEquals(TipoRequisito.FILTRO, v.requisitos.single().tipo)
        assertEquals(NivelHabilidad.INTERMEDIO, v.requisitos.single().nivelExigido)
        // Sin preguntas disponibles, se postula sin responder (regla 11).
        assertFalse(v.empresaConEvaluaciones)
        assertTrue(vacante().copy(conPreguntasFiltro = true).aDominio()!!.empresaConEvaluaciones)
    }

    @Test
    fun `una vacante con area, modalidad o fecha desconocida no se muestra`() {
        assertNull(vacante(area = "ASTRONOMIA").aDominio())
        assertNull(vacante(modalidad = "LUNAR").aDominio())
        assertNull(vacante(fecha = "15/12/2026").aDominio())
    }

    @Test
    fun `el practicante ve un estado simple de su postulacion`() {
        assertEquals(EstadoPostulacion.ENVIADA, estadoParaPracticante("NUEVA"))
        assertEquals(EstadoPostulacion.EN_REVISION, estadoParaPracticante("REVISION"))
        assertEquals(EstadoPostulacion.ENTREVISTA, estadoParaPracticante("ENTREVISTA"))
        assertEquals(EstadoPostulacion.SELECCIONADO, estadoParaPracticante("ELEGIDA"))
        assertEquals(EstadoPostulacion.NO_SELECCIONADO, estadoParaPracticante("DESCARTADA"))
        assertEquals(EstadoPostulacion.ENVIADA, estadoParaPracticante("OTRO"))
    }

    @Test
    fun `la postulacion conserva el motivo y los sellos conocidos`() {
        val p = PostulacionDto(
            id = "p1", vacanteId = "v1", puesto = "Analista", empresa = "Andes", estado = "DESCARTADA", fecha = "2026-11-24",
            evaluada = true, motivoDescarte = "Buscamos Power BI avanzado.", sellos = mapOf("r1" to "CONFIRMADO", "r2" to "RARO"),
        ).aDominio()
        assertEquals("Buscamos Power BI avanzado.", p.motivoDescarte)
        assertEquals(LocalDate.of(2026, 11, 24), p.fechaEnvio)
        assertEquals(mapOf("r1" to TipoSello.CONFIRMADO), p.sellos)
    }

    @Test
    fun `el perfil que se sube omite lo vacio y el nivel No lo se`() {
        val dto = actualizacionDePerfil(
            registro = DatosRegistro("Ana Pérez", "ana@uni.pe", "ESAN", "", 0),
            area = AreaInteres.FINANZAS,
            niveles = NivelesUsuario(declarados = mapOf("sql" to NivelHabilidad.BASICO, "excel" to NivelHabilidad.NO_LO_SE)),
            onboardingCompleto = true,
        )
        assertEquals("Ana Pérez", dto.nombre)
        assertNull(dto.carrera)
        assertNull(dto.ciclo)
        assertEquals("FINANZAS", dto.area)
        assertEquals(mapOf("sql" to "BASICO"), dto.nivelesDeclarados)
    }

    @Test
    fun `las horas libres viajan con los siete dias`() {
        val dto = HorasLibres(mapOf(DayOfWeek.MONDAY to 2.0, DayOfWeek.SATURDAY to 4.5)).aDto()
        assertEquals(7, dto.horas.size)
        assertEquals(2.0, dto.horas.getValue("MONDAY"), 0.0)
        assertEquals(0.0, dto.horas.getValue("SUNDAY"), 0.0)
    }

    @Test
    fun `del perfil del backend se descartan niveles y horas invalidas`() {
        val perfil = PerfilPracticanteDto(
            nombre = "Ana", universidad = "ESAN", carrera = "Economía", onboardingCompleto = true, premium = false,
            nivelesDeclarados = mapOf("sql" to "AVANZADO", "x" to "MAESTRO"),
            nivelesDemostrados = mapOf("sql" to "BASICO"),
            horasLibres = mapOf("MONDAY" to 3.0, "TUESDAY" to 3.3, "FUNDAY" to 2.0),
        )
        assertEquals(mapOf("sql" to NivelHabilidad.AVANZADO), perfil.niveles().declarados)
        assertEquals(NivelHabilidad.BASICO, perfil.niveles().demostrado("sql"))
        assertEquals(mapOf(DayOfWeek.MONDAY to 3.0), perfil.horas().porDia)
    }

    @Test
    fun `las preguntas y notificaciones del backend se adaptan sin inventar datos`() {
        val pregunta = pe.edu.esan.chambaready.core.data.remoto.PreguntaFiltroDto("q1", "¿Qué cláusula filtra?", listOf("A", "B", "C", "D")).aDominio()
        assertEquals(pe.edu.esan.chambaready.core.data.remoto.SIN_RESPUESTA, pregunta.indiceCorrecto)
        assertFalse(pregunta.esCorrecta(0))

        val dto = pe.edu.esan.chambaready.core.data.remoto.NotificacionDto("n1", "PREGUNTAS_FILTRO", "Responde", "Cuerpo", "v1", false, "2026-11-24T18:00:00Z")
        val n = dto.aDominio()!!
        assertEquals(pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion.PREGUNTAS_FILTRO, n.tipo)
        assertEquals("18:00 UTC son las 13:00 en Lima", 13, n.fecha.hour)
        assertNull("Un tipo de empresa no se muestra en la app", dto.copy(tipo = "NUEVO_POSTULANTE").aDominio())
    }

    @Test
    fun `la postulacion trae su id y si tiene preguntas pendientes`() {
        val p = PostulacionDto("p9", "v1", "Analista", "Andes", "NUEVA", "2026-11-24", false, preguntasPendientes = true).aDominio()
        assertEquals("p9", p.id)
        assertTrue(p.preguntasPendientes)
    }
}
