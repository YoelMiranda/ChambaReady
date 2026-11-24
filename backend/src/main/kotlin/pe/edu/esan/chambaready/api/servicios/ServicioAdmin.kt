package pe.edu.esan.chambaready.api.servicios

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Empresas
import pe.edu.esan.chambaready.api.db.EventosMetricas
import pe.edu.esan.chambaready.api.db.Evidencias
import pe.edu.esan.chambaready.api.db.HabilidadesUsuario
import pe.edu.esan.chambaready.api.db.IntentosTest
import pe.edu.esan.chambaready.api.db.Postulaciones
import pe.edu.esan.chambaready.api.db.SaldoEvaluacionesTabla
import pe.edu.esan.chambaready.api.db.SuscripcionesEmpresa
import pe.edu.esan.chambaready.api.db.UsoIa
import pe.edu.esan.chambaready.api.db.Usuarios
import pe.edu.esan.chambaready.api.db.Vacantes
import pe.edu.esan.chambaready.api.dto.CostoIaDto
import pe.edu.esan.chambaready.api.dto.MetricasDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import pe.edu.esan.chambaready.api.notificaciones.Notificador
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.reglas.ReglasNivel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Operaciones internas del equipo de ChambaReady (protegidas con ADMIN_TOKEN). */
class ServicioAdmin(private val bd: BaseDatos, private val reloj: Clock, private val notificador: Notificador) {

    /** Regla 14: costo real de IA por operación y modelo (nunca se muestra a usuarios). */
    suspend fun costosIa(): List<CostoIaDto> = bd.tx {
        UsoIa.selectAll().toList()
            .groupBy { it[UsoIa.operacion] to it[UsoIa.modelo] }
            .map { (clave, filas) ->
                CostoIaDto(
                    operacion = clave.first,
                    modelo = clave.second,
                    llamadas = filas.size.toLong(),
                    tokensEntrada = filas.sumOf { it[UsoIa.tokensEntrada].toLong() },
                    tokensSalida = filas.sumOf { it[UsoIa.tokensSalida].toLong() },
                    costoUsd = filas.sumOf { it[UsoIa.costoUsd].toDouble() },
                )
            }
            .sortedByDescending { it.costoUsd }
    }

    /** El equipo revisa el RUC y los datos de contacto y marca la empresa como verificada. */
    suspend fun verificarEmpresa(ruc: String) = bd.tx {
        val cambiadas = Empresas.update({ Empresas.ruc eq ruc.trim() }) { it[verificacion] = "VERIFICADA" }
        if (cambiadas == 0) throw ErrorApi.NoEncontrado("No hay una empresa con ese RUC.")
    }

    /**
     * Activa el Plan Empresa SIN vencimiento ni pago (para demostraciones del equipo). Las empresas
     * normales lo activan con el pago simulado de E06, que dura 30 días.
     */
    suspend fun activarPlanDePrueba(ruc: String) = bd.tx {
        val empresa = Empresas.selectAll().where { Empresas.ruc eq ruc.trim() }.singleOrNull() ?: throw ErrorApi.NoEncontrado("No hay una empresa con ese RUC.")
        SuscripcionesEmpresa.insert {
            it[id] = UUID.randomUUID().toString()
            it[empresaId] = empresa[Empresas.id]
            it[estado] = "ACTIVA"
            it[precioCentimos] = Consultas.configuracionLong("precio_plan_empresa_centimos").toInt()
            it[venceEn] = null
        }
        SaldoEvaluacionesTabla.update({ SaldoEvaluacionesTabla.empresaId eq empresa[Empresas.id] }) { it[planActivo] = true }
    }

    /**
     * El equipo revisó la evidencia (P23) y la valida. Regla 4: recién ahí sube el nivel demostrado
     * (nunca baja). El practicante recibe una notificación.
     */
    suspend fun validarEvidencia(evidenciaId: String) = bd.tx {
        val evidencia = Evidencias.selectAll().where { Evidencias.id eq evidenciaId }.singleOrNull() ?: throw ErrorApi.NoEncontrado("Esta evidencia no existe.")
        if (evidencia[Evidencias.estado] == EstadoEvidencia.VALIDADA.name) return@tx
        Evidencias.update({ Evidencias.id eq evidenciaId }) { it[estado] = EstadoEvidencia.VALIDADA.name }
        val usuarioId = evidencia[Evidencias.usuarioId]
        val habilidadId = evidencia[Evidencias.habilidadId]
        val antes = Consultas.niveles(usuarioId).demostrado(habilidadId)
        val despues = ReglasNivel.trasEvidencia(antes, NivelHabilidad.valueOf(evidencia[Evidencias.nivel]), EstadoEvidencia.VALIDADA)
        if (despues != antes) {
            val cambiadas = HabilidadesUsuario.update({ (HabilidadesUsuario.usuarioId eq usuarioId) and (HabilidadesUsuario.habilidadId eq habilidadId) }) {
                it[nivelDemostrado] = despues.name
                it[fuenteDemostrado] = "EVIDENCIA"
            }
            if (cambiadas == 0) {
                HabilidadesUsuario.insert {
                    it[HabilidadesUsuario.usuarioId] = usuarioId
                    it[HabilidadesUsuario.habilidadId] = habilidadId
                    it[nivelDeclarado] = NivelHabilidad.NO_LO_SE.name
                    it[nivelDemostrado] = despues.name
                    it[fuenteDemostrado] = "EVIDENCIA"
                }
            }
        }
        notificador.notificar(usuarioId, "EVIDENCIA", "Evidencia validada", "Validamos «${evidencia[Evidencias.titulo]}». Tu nivel demostrado se actualizó.", null)
    }

    /** F9: métricas del proyecto (sin datos personales). */
    suspend fun metricas(): MetricasDto = bd.tx {
        val desde = Instant.now(reloj).minus(Duration.ofDays(30))
        val eventos = EventosMetricas.selectAll().where { EventosMetricas.creadoEn greaterEq desde }
            .groupingBy { it[EventosMetricas.tipo] }.eachCount()
        val postulaciones = Postulaciones.selectAll().toList()
        val intentos = IntentosTest.selectAll().toList()
        MetricasDto(
            eventos30Dias = eventos,
            practicantes = Usuarios.selectAll().where { Usuarios.rol eq "PRACTICANTE" }.count().toInt(),
            empresas = Empresas.selectAll().count().toInt(),
            vacantesAbiertas = Vacantes.selectAll().where { Vacantes.fechaCierre greaterEq LocalDate.now(reloj) }.count().toInt(),
            postulaciones = postulaciones.size,
            porcentajeEvaluadas = porcentaje(postulaciones.count { it[Postulaciones.evaluada] }, postulaciones.size),
            porcentajeMiniTestsAprobados = porcentaje(intentos.count { it[IntentosTest.aprobado] }, intentos.size),
            costoIaUsd = UsoIa.selectAll().sumOf { it[UsoIa.costoUsd].toDouble() },
        )
    }

    private fun porcentaje(parte: Int, total: Int): Int = if (total == 0) 0 else (parte * 100 + total / 2) / total
}
