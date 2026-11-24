package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.empresa.ReglasEmpresa
import pe.edu.esan.chambaready.core.domain.empresa.RequisitoConSello
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello

/** E01 (RUC) y E04 (% de ajuste). Mismos casos que las pruebas de la web, para que ambas den igual. */
class ReglasEmpresaTest {

    @Test
    fun ruc_validos() {
        assertTrue(ReglasEmpresa.rucValido("20131312955"))
        assertTrue(ReglasEmpresa.rucValido(" 20100123453 "))
    }

    @Test
    fun ruc_invalidos() {
        assertFalse(ReglasEmpresa.rucValido("2013131295")) // 10 dígitos
        assertFalse(ReglasEmpresa.rucValido("2013131295A"))
        assertFalse(ReglasEmpresa.rucValido("30131312955")) // prefijo
        assertFalse(ReglasEmpresa.rucValido("20131312956")) // dígito verificador
    }

    @Test
    fun ajuste_ponderaYDeclaradoValeLaMitad() {
        val requisitos = listOf(
            RequisitoConSello(TipoRequisito.FILTRO, TipoSello.CONFIRMADO),
            RequisitoConSello(TipoRequisito.OBLIGATORIO, TipoSello.DECLARADO),
            RequisitoConSello(TipoRequisito.DESEABLE, TipoSello.NO_CUMPLE),
        )
        assertEquals(67, ReglasEmpresa.ajuste(requisitos))
        assertEquals(100, ReglasEmpresa.ajuste(requisitos.map { it.copy(sello = TipoSello.CONFIRMADO) }))
        assertEquals(100, ReglasEmpresa.ajuste(emptyList()))
    }
}
