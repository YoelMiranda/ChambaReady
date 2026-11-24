package pe.edu.esan.chambaready.core.domain.empresa

/** Saldo de evaluaciones de una empresa (tabla `saldo_evaluaciones`). */
data class SaldoEvaluaciones(
    /** Prueba gratis: empieza en 20, cuenta por postulación evaluada y no se renueva. */
    val gratisRestantes: Int,
    /** True mientras el Plan Empresa (precio fijo mensual) está pagado y vigente. */
    val planActivo: Boolean = false,
)

/** De dónde salió una evaluación. */
enum class FuenteEvaluacion { PRUEBA_GRATIS, PLAN }

sealed interface ResultadoConsumo {
    data class Consumida(val fuente: FuenteEvaluacion, val nuevoSaldo: SaldoEvaluaciones) : ResultadoConsumo

    /** Regla 11: sin saldo, el postulante entra sin preguntas filtro y queda "Sin evaluar". */
    data object SinSaldo : ResultadoConsumo
}

/**
 * Reglas 9 a 13: cobro a empresas. Ver `docs/cobro-evaluaciones.md`.
 *
 * - Prueba gratis: 20 evaluaciones por empresa, una sola vez, para todas sus vacantes.
 * - Una evaluación = un postulante que responde las 2 preguntas filtro (opción múltiple) de una vacante.
 * - Plan Empresa: precio FIJO mensual (en la tabla de configuración) con evaluaciones ilimitadas.
 *   Es viable porque con opción múltiple la corrección no usa IA: la IA solo genera las preguntas,
 *   una vez por vacante.
 *
 * El backend aplica [consumir] dentro de una transacción (descuento atómico), para que dos
 * postulaciones simultáneas no usen la misma evaluación gratis.
 */
object ReglasEvaluaciones {

    const val EVALUACIONES_GRATIS = 20

    /** Umbral del aviso ámbar "quedan pocas evaluaciones" en E02. */
    const val UMBRAL_POCAS = 5

    fun saldoInicial(): SaldoEvaluaciones = SaldoEvaluaciones(gratisRestantes = EVALUACIONES_GRATIS)

    /**
     * Con plan activo la evaluación sale del plan (ilimitada) y las gratis se conservan por si la
     * empresa cancela. Sin plan, se descuenta una gratis. Si no queda ninguna, [ResultadoConsumo.SinSaldo].
     */
    fun consumir(saldo: SaldoEvaluaciones): ResultadoConsumo = when {
        saldo.planActivo -> ResultadoConsumo.Consumida(FuenteEvaluacion.PLAN, saldo)
        saldo.gratisRestantes > 0 -> ResultadoConsumo.Consumida(
            FuenteEvaluacion.PRUEBA_GRATIS,
            saldo.copy(gratisRestantes = saldo.gratisRestantes - 1),
        )
        else -> ResultadoConsumo.SinSaldo
    }

    /** Evaluaciones que quedan; null significa ilimitadas (plan activo). */
    fun disponibles(saldo: SaldoEvaluaciones): Int? = if (saldo.planActivo) null else saldo.gratisRestantes

    /** Aviso ámbar de E02: sin plan y con 5 gratis o menos. */
    fun quedanPocas(saldo: SaldoEvaluaciones): Boolean =
        !saldo.planActivo && saldo.gratisRestantes <= UMBRAL_POCAS

    /** Regla 13: solo con el plan activo se envía el test a quienes entraron "Sin evaluar". */
    fun puedeEvaluarPendientes(saldo: SaldoEvaluaciones): Boolean = saldo.planActivo
}
