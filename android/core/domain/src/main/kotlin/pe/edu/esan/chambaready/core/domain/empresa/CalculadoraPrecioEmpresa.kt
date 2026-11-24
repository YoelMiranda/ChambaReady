package pe.edu.esan.chambaready.core.domain.empresa

import kotlin.math.roundToLong

/** Margen de una empresa en un mes, para el endpoint de administración. Montos en céntimos de sol. */
data class MargenMensual(
    val ingresoSinIgvCentimos: Long,
    val comisionPasarelaCentimos: Long,
    val costoIaCentimos: Long,
) {
    val margenCentimos: Long get() = ingresoSinIgvCentimos - comisionPasarelaCentimos - costoIaCentimos
}

/**
 * Cálculos de precio de la empresa. El precio del Plan Empresa es FIJO y viene de la tabla de
 * configuración (montos en céntimos, IGV incluido); aquí no hay precios escritos.
 */
object CalculadoraPrecioEmpresa {

    const val IGV = 0.18

    /**
     * Calculadora de E06: "¿Cuántos postulantes recibes al mes?" → precio por postulante evaluado.
     * Con 0 postulantes devuelve el precio completo. Se redondea al céntimo más cercano.
     */
    fun precioPorEvaluadoCentimos(precioMensualCentimos: Long, postulantesMes: Int): Long {
        require(postulantesMes >= 0) { "Postulantes negativos: $postulantesMes" }
        val divisor = postulantesMes.coerceAtLeast(1)
        return (precioMensualCentimos + divisor / 2) / divisor
    }

    /**
     * Regla 14 (solo administración): cuánto queda de lo que paga una empresa después del IGV, la
     * comisión de la pasarela (sobre el monto cobrado) y el costo de IA registrado en `uso_ia`.
     */
    fun margenMensual(precioConIgvCentimos: Long, comisionPasarela: Double, costoIaCentimos: Long): MargenMensual {
        require(comisionPasarela in 0.0..1.0) { "Comisión fuera de rango: $comisionPasarela" }
        return MargenMensual(
            ingresoSinIgvCentimos = (precioConIgvCentimos / (1 + IGV)).roundToLong(),
            comisionPasarelaCentimos = (precioConIgvCentimos * comisionPasarela).roundToLong(),
            costoIaCentimos = costoIaCentimos,
        )
    }

    /** Costo de una llamada a Gemini según sus tokens (precios por millón de tokens, configurables). */
    fun costoIaUsd(tokensEntrada: Long, tokensSalida: Long, usdPorMillonEntrada: Double, usdPorMillonSalida: Double): Double =
        (tokensEntrada * usdPorMillonEntrada + tokensSalida * usdPorMillonSalida) / 1_000_000.0
}
