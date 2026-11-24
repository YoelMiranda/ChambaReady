package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.reglas.ZonaProbabilidad

/** Regla 3: Baja < 40 %, Media 40–69 %, Alta ≥ 70 %. */
class ZonaProbabilidadTest {

    @Test
    fun de_limitesDeCadaZona() {
        assertEquals(ZonaProbabilidad.BAJA, ZonaProbabilidad.de(0))
        assertEquals(ZonaProbabilidad.BAJA, ZonaProbabilidad.de(39))
        assertEquals(ZonaProbabilidad.MEDIA, ZonaProbabilidad.de(40))
        assertEquals(ZonaProbabilidad.MEDIA, ZonaProbabilidad.de(69))
        assertEquals(ZonaProbabilidad.ALTA, ZonaProbabilidad.de(70))
        assertEquals(ZonaProbabilidad.ALTA, ZonaProbabilidad.de(100))
    }

    @Test
    fun de_valoresFueraDeRangoSeAjustan() {
        assertEquals(ZonaProbabilidad.BAJA, ZonaProbabilidad.de(-5))
        assertEquals(ZonaProbabilidad.ALTA, ZonaProbabilidad.de(140))
    }

    @Test
    fun anchos_sumanCien() {
        assertEquals(100, ZonaProbabilidad.entries.sumOf { it.ancho })
    }
}
