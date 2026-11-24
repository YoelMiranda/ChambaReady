package pe.edu.esan.chambaready.core.domain.empresa

import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import kotlin.math.roundToInt

/** Un requisito con el sello que tiene un postulante en él (E04). */
data class RequisitoConSello(
    val tipo: TipoRequisito,
    val sello: TipoSello,
)

/**
 * Reglas de la empresa que valida el backend (F4). La web repite estas mismas reglas en
 * `web-empresa/src/design/reglas/empresa.ts` solo para validar antes de enviar.
 */
object ReglasEmpresa {

    private val prefijosRuc = setOf("10", "15", "16", "17", "20")
    private val pesosRuc = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    /** RUC peruano: 11 dígitos, prefijo válido y dígito verificador (módulo 11 de SUNAT). */
    fun rucValido(ruc: String): Boolean {
        val limpio = ruc.trim()
        if (limpio.length != 11 || !limpio.all { it.isDigit() }) return false
        if (limpio.take(2) !in prefijosRuc) return false
        val suma = pesosRuc.indices.sumOf { i -> pesosRuc[i] * limpio[i].digitToInt() }
        val verificador = when (val resto = 11 - suma % 11) {
            10 -> 0
            11 -> 1
            else -> resto
        }
        return verificador == limpio[10].digitToInt()
    }

    /**
     * % de ajuste de un postulante a UNA vacante (solo ordena los candidatos de esa vacante; no hay
     * ranking global). Pesos: filtro 3, obligatorio 2, deseable 1. Confirmado vale 1, declarado 0.5
     * (nadie lo comprobó) y no cumple 0.
     */
    fun ajuste(requisitos: List<RequisitoConSello>): Int {
        if (requisitos.isEmpty()) return 100
        fun peso(tipo: TipoRequisito) = when (tipo) {
            TipoRequisito.FILTRO -> 3
            TipoRequisito.OBLIGATORIO -> 2
            TipoRequisito.DESEABLE -> 1
        }
        fun puntaje(sello: TipoSello) = when (sello) {
            TipoSello.CONFIRMADO -> 1.0
            TipoSello.DECLARADO -> 0.5
            TipoSello.NO_CUMPLE -> 0.0
        }
        val total = requisitos.sumOf { peso(it.tipo) }
        val obtenido = requisitos.sumOf { peso(it.tipo) * puntaje(it.sello) }
        return (100.0 * obtenido / total).roundToInt()
    }
}
