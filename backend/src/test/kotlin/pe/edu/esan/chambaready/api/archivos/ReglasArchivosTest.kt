package pe.edu.esan.chambaready.api.archivos

import pe.edu.esan.chambaready.api.auth.Rol
import pe.edu.esan.chambaready.api.pagos.ReglasPagos
import pe.edu.esan.chambaready.api.servicios.ServicioEmpresa
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReglasArchivosTest {

    private val pdf = "%PDF-1.7 contenido".toByteArray()
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)

    @Test
    fun unCvValidoSePuedeGuardar() {
        assertNull(ReglasArchivos.validar(TipoArchivo.CV, Rol.PRACTICANTE, "application/pdf", pdf))
        assertNull(ReglasArchivos.validar(TipoArchivo.LOGO, Rol.EMPRESA, "image/png; charset=binary", png))
    }

    @Test
    fun seRechazanFormatoRolTamanoYFirmaFalsa() {
        assertNotNull(ReglasArchivos.validar(TipoArchivo.CV, Rol.PRACTICANTE, "application/x-msdownload", pdf))
        assertNotNull(ReglasArchivos.validar(TipoArchivo.CV, Rol.EMPRESA, "application/pdf", pdf), "Una empresa no sube CVs")
        assertNotNull(ReglasArchivos.validar(TipoArchivo.LOGO, Rol.EMPRESA, "image/png", ByteArray(2 * 1024 * 1024) { 0x50 }))
        assertNotNull(ReglasArchivos.validar(TipoArchivo.CV, Rol.PRACTICANTE, "application/pdf", ByteArray(0)))
        // Un ejecutable renombrado como PDF: el tipo dice PDF, pero los bytes no.
        assertNotNull(ReglasArchivos.validar(TipoArchivo.CV, Rol.PRACTICANTE, "application/pdf", "MZ\u0090\u0000".toByteArray()))
    }

    @Test
    fun elNombreNoPuedeTraerCarpetas() {
        assertEquals("cv.pdf", ReglasArchivos.nombreSeguro("../../etc/cv.pdf", TipoArchivo.CV))
        assertEquals("cv.pdf", ReglasArchivos.nombreSeguro("C:\\Users\\ana\\cv.pdf", TipoArchivo.CV))
        assertEquals("logo", ReglasArchivos.nombreSeguro("   ", TipoArchivo.LOGO))
    }

    @Test
    fun quienPuedeVerCadaArchivo() {
        assertTrue(ReglasArchivos.puedeVer(TipoArchivo.CV, "ana", "ana", Rol.PRACTICANTE, false), "El dueño siempre")
        assertFalse(ReglasArchivos.puedeVer(TipoArchivo.CV, "ana", "beto", Rol.PRACTICANTE, false), "Otro practicante nunca")
        assertFalse(ReglasArchivos.puedeVer(TipoArchivo.CV, "ana", "empresa", Rol.EMPRESA, false), "Una empresa a la que no postuló, no")
        assertTrue(ReglasArchivos.puedeVer(TipoArchivo.EVIDENCIA, "ana", "empresa", Rol.EMPRESA, true), "La empresa a la que postuló, sí")
        assertTrue(ReglasArchivos.puedeVer(TipoArchivo.LOGO, "empresa", "ana", Rol.PRACTICANTE, false), "Los logos son públicos")
    }
}

class ReglasPagosTest {

    private val ahora = Instant.parse("2026-11-24T13:00:00Z")

    @Test
    fun pagarAntesDeQueVenzaSumaDias() {
        val vence = Instant.parse("2026-12-10T13:00:00Z")
        assertEquals(Instant.parse("2027-01-09T13:00:00Z"), ReglasPagos.nuevoVencimiento(vence, ahora, 30))
    }

    @Test
    fun siYaVencioCuentaDesdeHoy() {
        assertEquals(Instant.parse("2026-12-24T13:00:00Z"), ReglasPagos.nuevoVencimiento(Instant.parse("2026-11-01T00:00:00Z"), ahora, 30))
        assertEquals(Instant.parse("2026-12-24T13:00:00Z"), ReglasPagos.nuevoVencimiento(null, ahora, 30))
    }

    @Test
    fun unPlanSinFechaNoVence() {
        assertTrue(ReglasPagos.vigente(null, ahora))
        assertFalse(ReglasPagos.vigente(Instant.parse("2026-11-24T12:59:59Z"), ahora))
    }

    @Test
    fun lasPreguntasFiltroVanPrimeroALosRequisitosFiltro() {
        // (es filtro, tipo): deseable, obligatorio, filtro, obligatorio
        val requisitos = listOf(false to "DESEABLE", false to "OBLIGATORIO", true to "OBLIGATORIO", false to "OBLIGATORIO")
        assertEquals(listOf(2, 1), ServicioEmpresa.indicesParaPreguntas(requisitos))
        assertEquals(listOf(0), ServicioEmpresa.indicesParaPreguntas(listOf(false to "DESEABLE")))
    }
}
