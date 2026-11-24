package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.reglas.CampoRegistro
import pe.edu.esan.chambaready.core.domain.reglas.PlanActivo
import pe.edu.esan.chambaready.core.domain.reglas.ReglasPrioridad
import pe.edu.esan.chambaready.core.domain.reglas.ReglasRegistro
import pe.edu.esan.chambaready.core.domain.reglas.ResultadoCv

/** C02 (registro), P01 (CV) y P14 (prioridad de pendientes). */
class ReglasRegistroYPrioridadTest {

    private val valido = DatosRegistro("Ana Torres", "ana@ue.edu.pe", "Universidad ESAN", "Ingeniería de Información", 6)

    @Test
    fun registro_validoNoTieneErrores() {
        assertTrue(ReglasRegistro.validar(valido).isEmpty())
    }

    @Test
    fun registro_marcaCadaCampo() {
        val errores = ReglasRegistro.validar(DatosRegistro(nombre = " ", correo = "ana@", ciclo = 15))
        assertEquals(
            setOf(CampoRegistro.NOMBRE, CampoRegistro.CORREO, CampoRegistro.UNIVERSIDAD, CampoRegistro.CARRERA, CampoRegistro.CICLO),
            errores.keys,
        )
    }

    @Test
    fun cv_formatosYTamano() {
        assertEquals(ResultadoCv.VALIDO, ReglasRegistro.validarCv("application/pdf", 300_000))
        assertEquals(
            ResultadoCv.VALIDO,
            ReglasRegistro.validarCv("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 5L * 1024 * 1024),
        )
        assertEquals(ResultadoCv.MUY_PESADO, ReglasRegistro.validarCv("application/pdf", 5L * 1024 * 1024 + 1))
        assertEquals(ResultadoCv.FORMATO_INVALIDO, ReglasRegistro.validarCv("image/png", 1_000))
        assertEquals(ResultadoCv.FORMATO_INVALIDO, ReglasRegistro.validarCv(null, 1_000))
        assertEquals(ResultadoCv.VACIO, ReglasRegistro.validarCv("application/pdf", 0))
    }

    @Test
    fun prioridad_cierreCercanoPrimeroLuegoMenosAvance() {
        val planes = listOf(
            PlanActivo("a", diasRestantes = 10, avance = 0.2),
            PlanActivo("b", diasRestantes = 3, avance = 0.9),
            PlanActivo("c", diasRestantes = 10, avance = 0.0),
            PlanActivo("d", diasRestantes = -2, avance = 0.0),
        )
        assertEquals(listOf("b", "c", "a", "d"), ReglasPrioridad.ordenar(planes).map { it.vacanteId })
    }
}
