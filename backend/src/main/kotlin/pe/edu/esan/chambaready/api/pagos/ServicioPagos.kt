package pe.edu.esan.chambaready.api.pagos

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Empresas
import pe.edu.esan.chambaready.api.db.PagosSimulados
import pe.edu.esan.chambaready.api.db.SaldoEvaluacionesTabla
import pe.edu.esan.chambaready.api.db.SuscripcionesEmpresa
import pe.edu.esan.chambaready.api.db.SuscripcionesEstudiante
import pe.edu.esan.chambaready.api.dto.EstadoPlanDto
import pe.edu.esan.chambaready.api.dto.PagoDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import pe.edu.esan.chambaready.api.notificaciones.Notificador
import pe.edu.esan.chambaready.api.servicios.Consultas
import pe.edu.esan.chambaready.api.servicios.Metricas
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Reglas de los pagos simulados. Puras y probadas en ReglasPagosTest. */
object ReglasPagos {
    /**
     * Pagar de nuevo antes de que venza SUMA días al vencimiento actual (no se pierden días);
     * si ya venció (o nunca pagó), cuenta desde ahora.
     */
    fun nuevoVencimiento(venceActual: Instant?, ahora: Instant, dias: Long): Instant {
        val desde = if (venceActual != null && venceActual.isAfter(ahora)) venceActual else ahora
        return desde.plus(Duration.ofDays(dias))
    }

    /** Una suscripción sin fecha de vencimiento (plan de prueba del equipo) no vence. */
    fun vigente(venceEn: Instant?, ahora: Instant): Boolean = venceEn == null || venceEn.isAfter(ahora)
}

/**
 * Estado de los planes de las empresas. El campo saldo_evaluaciones.plan_activo es el que miran las
 * reglas de evaluación; aquí se apaga cuando la suscripción vence. Se llama al empezar las
 * operaciones que dependen del plan (dentro de su transacción).
 */
object Planes {
    fun sincronizarVencimientos(ahora: Instant) {
        SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.planActivo eq true }.forEach { fila ->
            val empresaId = fila[SaldoEvaluacionesTabla.empresaId]
            val vigente = SuscripcionesEmpresa.selectAll()
                .where { (SuscripcionesEmpresa.empresaId eq empresaId) and (SuscripcionesEmpresa.estado eq "ACTIVA") }
                .any { ReglasPagos.vigente(it[SuscripcionesEmpresa.venceEn], ahora) }
            if (!vigente) {
                SaldoEvaluacionesTabla.update({ SaldoEvaluacionesTabla.empresaId eq empresaId }) { it[planActivo] = false }
                SuscripcionesEmpresa.update({ (SuscripcionesEmpresa.empresaId eq empresaId) and (SuscripcionesEmpresa.estado eq "ACTIVA") }) {
                    it[estado] = "VENCIDA"
                }
            }
        }
    }

    /** Fecha hasta la que la empresa tiene plan (la más lejana de sus suscripciones activas), o null. */
    fun venceEmpresa(empresaId: String, ahora: Instant): Instant? =
        SuscripcionesEmpresa.selectAll()
            .where { (SuscripcionesEmpresa.empresaId eq empresaId) and (SuscripcionesEmpresa.estado eq "ACTIVA") }
            .mapNotNull { it[SuscripcionesEmpresa.venceEn] }
            .filter { it.isAfter(ahora) }
            .maxOrNull()
}

/**
 * F8 SIMULADO: nadie paga de verdad (decisión del equipo). "Pagar" registra un pago de prueba con el
 * precio de la tabla de configuración y activa el plan por `dias_por_pago` días.
 */
class ServicioPagos(private val bd: BaseDatos, private val reloj: Clock, private val notificador: Notificador) {

    private fun ahora(): Instant = Instant.now(reloj)

    // ---------- Plan Empresa (E06) ----------

    suspend fun estadoPlanEmpresa(usuarioId: String): EstadoPlanDto = bd.tx { estadoEmpresa(usuarioId) }

    suspend fun pagarPlanEmpresa(usuarioId: String): EstadoPlanDto = bd.tx {
        val empresaId = empresaId(usuarioId)
        Planes.sincronizarVencimientos(ahora())
        val precio = Consultas.configuracionLong("precio_plan_empresa_centimos").toInt()
        val vence = ReglasPagos.nuevoVencimiento(Planes.venceEmpresa(empresaId, ahora()), ahora(), Consultas.configuracionLong("dias_por_pago"))
        SuscripcionesEmpresa.insert {
            it[id] = UUID.randomUUID().toString()
            it[SuscripcionesEmpresa.empresaId] = empresaId
            it[estado] = "ACTIVA"
            it[precioCentimos] = precio
            it[venceEn] = vence
        }
        SaldoEvaluacionesTabla.update({ SaldoEvaluacionesTabla.empresaId eq empresaId }) { it[planActivo] = true }
        registrarPago(usuarioId, "PLAN_EMPRESA", precio, vence)
        notificador.notificar(usuarioId, "PAGO", "Plan Empresa activo", "Pago simulado registrado. Evaluaciones ilimitadas hasta el ${fecha(vence)}.", null)
        Metricas.registrar(usuarioId, "PAGO_SIMULADO", mapOf("concepto" to "PLAN_EMPRESA"))
        estadoEmpresa(usuarioId)
    }

    private fun estadoEmpresa(usuarioId: String): EstadoPlanDto {
        val empresaId = empresaId(usuarioId)
        Planes.sincronizarVencimientos(ahora())
        val activo = SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.empresaId eq empresaId }.single()[SaldoEvaluacionesTabla.planActivo]
        return EstadoPlanDto(
            activo = activo,
            venceEn = if (activo) Planes.venceEmpresa(empresaId, ahora())?.let(::fecha) else null,
            precioCentimos = Consultas.configuracionLong("precio_plan_empresa_centimos"),
            pagos = pagos(usuarioId, "PLAN_EMPRESA"),
        )
    }

    private fun empresaId(usuarioId: String): String =
        Empresas.selectAll().where { Empresas.usuarioId eq usuarioId }.singleOrNull()?.get(Empresas.id)
            ?: throw ErrorApi.NoEncontrado("Primero completa el perfil de tu empresa.")

    // ---------- Premium del practicante (P14) ----------

    suspend fun estadoPremium(usuarioId: String): EstadoPlanDto = bd.tx { estadoPracticante(usuarioId) }

    suspend fun pagarPremium(usuarioId: String): EstadoPlanDto = bd.tx {
        val precio = Consultas.configuracionLong("precio_premium_centimos").toInt()
        val vence = ReglasPagos.nuevoVencimiento(vencePremium(usuarioId), ahora(), Consultas.configuracionLong("dias_por_pago"))
        SuscripcionesEstudiante.insert {
            it[id] = UUID.randomUUID().toString()
            it[SuscripcionesEstudiante.usuarioId] = usuarioId
            it[estado] = "ACTIVA"
            it[precioCentimos] = precio
            it[venceEn] = vence
        }
        registrarPago(usuarioId, "PREMIUM", precio, vence)
        notificador.notificar(usuarioId, "PAGO", "Premium activo", "Pago simulado registrado. Tienes Premium hasta el ${fecha(vence)}.", null)
        Metricas.registrar(usuarioId, "PAGO_SIMULADO", mapOf("concepto" to "PREMIUM"))
        estadoPracticante(usuarioId)
    }

    private fun estadoPracticante(usuarioId: String): EstadoPlanDto {
        val vence = vencePremium(usuarioId)
        return EstadoPlanDto(
            activo = vence != null,
            venceEn = vence?.let(::fecha),
            precioCentimos = Consultas.configuracionLong("precio_premium_centimos"),
            pagos = pagos(usuarioId, "PREMIUM"),
        )
    }

    private fun vencePremium(usuarioId: String): Instant? =
        SuscripcionesEstudiante.selectAll()
            .where { (SuscripcionesEstudiante.usuarioId eq usuarioId) and (SuscripcionesEstudiante.estado eq "ACTIVA") }
            .mapNotNull { it[SuscripcionesEstudiante.venceEn] }
            .filter { it.isAfter(ahora()) }
            .maxOrNull()

    // ---------- Comunes ----------

    private fun registrarPago(usuarioId: String, concepto: String, monto: Int, vence: Instant) {
        PagosSimulados.insert {
            it[id] = UUID.randomUUID().toString()
            it[PagosSimulados.usuarioId] = usuarioId
            it[PagosSimulados.concepto] = concepto
            it[montoCentimos] = monto
            it[venceEn] = vence
            it[creadoEn] = ahora()
        }
    }

    private fun pagos(usuarioId: String, concepto: String): List<PagoDto> =
        PagosSimulados.selectAll()
            .where { (PagosSimulados.usuarioId eq usuarioId) and (PagosSimulados.concepto eq concepto) }
            .orderBy(PagosSimulados.creadoEn, SortOrder.DESC)
            .map {
                PagoDto(it[PagosSimulados.id], it[PagosSimulados.concepto], it[PagosSimulados.montoCentimos], fecha(it[PagosSimulados.creadoEn]), fecha(it[PagosSimulados.venceEn]))
            }

    private fun fecha(instante: Instant): String = instante.atZone(reloj.zone).toLocalDate().toString()
}
