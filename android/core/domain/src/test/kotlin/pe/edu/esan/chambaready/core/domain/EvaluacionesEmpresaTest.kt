package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.empresa.CalculadoraPrecioEmpresa
import pe.edu.esan.chambaready.core.domain.empresa.FuenteEvaluacion
import pe.edu.esan.chambaready.core.domain.empresa.ReglasEvaluaciones
import pe.edu.esan.chambaready.core.domain.empresa.ResultadoConsumo
import pe.edu.esan.chambaready.core.domain.empresa.SaldoEvaluaciones

/**
 * Reglas 9 a 14: 20 evaluaciones gratis una sola vez y luego Plan Empresa de precio fijo con
 * evaluaciones ilimitadas. El precio de prueba (S/ 149 = 14 900 céntimos) imita la tabla de configuración.
 */
class EvaluacionesEmpresaTest {

    private val precioPlan = 14_900L

    private fun consumida(resultado: ResultadoConsumo) = resultado as ResultadoConsumo.Consumida

    // ---------- Saldo ----------

    @Test
    fun pruebaGratis_veinteUnaSolaVez() {
        var saldo = ReglasEvaluaciones.saldoInicial()
        assertEquals(20, saldo.gratisRestantes)
        repeat(20) {
            val resultado = consumida(ReglasEvaluaciones.consumir(saldo))
            assertEquals(FuenteEvaluacion.PRUEBA_GRATIS, resultado.fuente)
            saldo = resultado.nuevoSaldo
        }
        assertEquals(0, saldo.gratisRestantes)
        assertSame(ResultadoConsumo.SinSaldo, ReglasEvaluaciones.consumir(saldo))
    }

    @Test
    fun planActivo_esIlimitadoYConservaLasGratis() {
        val saldo = SaldoEvaluaciones(gratisRestantes = 7, planActivo = true)
        var actual = saldo
        repeat(1_000) {
            val resultado = consumida(ReglasEvaluaciones.consumir(actual))
            assertEquals(FuenteEvaluacion.PLAN, resultado.fuente)
            actual = resultado.nuevoSaldo
        }
        assertEquals(7, actual.gratisRestantes)
    }

    @Test
    fun planActivo_sinGratisIgualEvalua() {
        val resultado = consumida(ReglasEvaluaciones.consumir(SaldoEvaluaciones(gratisRestantes = 0, planActivo = true)))
        assertEquals(FuenteEvaluacion.PLAN, resultado.fuente)
    }

    @Test
    fun disponiblesYAvisoDePocas() {
        assertEquals(20, ReglasEvaluaciones.disponibles(ReglasEvaluaciones.saldoInicial()))
        assertNull(ReglasEvaluaciones.disponibles(SaldoEvaluaciones(0, planActivo = true)))
        assertFalse(ReglasEvaluaciones.quedanPocas(ReglasEvaluaciones.saldoInicial()))
        assertTrue(ReglasEvaluaciones.quedanPocas(SaldoEvaluaciones(gratisRestantes = 5)))
        assertFalse(ReglasEvaluaciones.quedanPocas(SaldoEvaluaciones(gratisRestantes = 0, planActivo = true)))
    }

    @Test
    fun evaluarPendientes_soloConPlan() {
        assertFalse(ReglasEvaluaciones.puedeEvaluarPendientes(SaldoEvaluaciones(gratisRestantes = 10)))
        assertTrue(ReglasEvaluaciones.puedeEvaluarPendientes(SaldoEvaluaciones(gratisRestantes = 0, planActivo = true)))
    }

    // ---------- Calculadora de E06 ----------

    @Test
    fun precioPorEvaluado_bajaConMasPostulantes() {
        assertEquals(149L, CalculadoraPrecioEmpresa.precioPorEvaluadoCentimos(precioPlan, 100)) // S/ 1,49
        assertEquals(50L, CalculadoraPrecioEmpresa.precioPorEvaluadoCentimos(precioPlan, 300)) // 49,67 → S/ 0,50
        assertEquals(15L, CalculadoraPrecioEmpresa.precioPorEvaluadoCentimos(precioPlan, 1_000)) // 14,9 → S/ 0,15
    }

    @Test
    fun precioPorEvaluado_ceroPostulantesEsElPrecioCompleto() {
        assertEquals(precioPlan, CalculadoraPrecioEmpresa.precioPorEvaluadoCentimos(precioPlan, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun precioPorEvaluado_rechazaNegativos() {
        CalculadoraPrecioEmpresa.precioPorEvaluadoCentimos(precioPlan, -1)
    }

    // ---------- Margen (administración) ----------

    @Test
    fun margen_restaIgvComisionYCostoIa() {
        // S/ 149 con IGV → S/ 126,27 sin IGV. Comisión de ejemplo 5 % de 149 = S/ 7,45. IA: S/ 0,50.
        val margen = CalculadoraPrecioEmpresa.margenMensual(precioPlan, comisionPasarela = 0.05, costoIaCentimos = 50)
        assertEquals(12_627L, margen.ingresoSinIgvCentimos)
        assertEquals(745L, margen.comisionPasarelaCentimos)
        assertEquals(11_832L, margen.margenCentimos)
    }

    @Test
    fun costoIa_segunTokens() {
        // 3 000 de entrada y 800 de salida a US$0,30 / US$2,50 por millón = US$0,0029.
        assertEquals(0.0029, CalculadoraPrecioEmpresa.costoIaUsd(3_000, 800, 0.30, 2.50), 1e-9)
    }
}
