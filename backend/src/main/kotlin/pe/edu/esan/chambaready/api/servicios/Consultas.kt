package pe.edu.esan.chambaready.api.servicios

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import pe.edu.esan.chambaready.api.db.ConfiguracionTabla
import pe.edu.esan.chambaready.api.db.CursosExternos
import pe.edu.esan.chambaready.api.db.Empresas
import pe.edu.esan.chambaready.api.db.Habilidades
import pe.edu.esan.chambaready.api.db.HabilidadesUsuario
import pe.edu.esan.chambaready.api.db.RequisitosVacante
import pe.edu.esan.chambaready.api.db.SaldoEvaluacionesTabla
import pe.edu.esan.chambaready.api.db.Vacantes
import pe.edu.esan.chambaready.api.dto.RequisitoDto
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.CursoExterno
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.RequisitoVacante
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen

/** Vacante leída de la base, con lo que necesitan los servicios además del modelo del dominio. */
data class VacanteCargada(
    val detalle: VacanteDetalle,
    val empresaId: String,
    val empresaNombre: String,
    /** Fecha y hora de creación (ISO), para ordenar del más nuevo al más antiguo. */
    val creada: String = "",
) {
    val id: String get() = detalle.resumen.id

    fun requisitosDto(): List<RequisitoDto> = detalle.requisitos.map {
        RequisitoDto(it.id, it.habilidadId, it.nombre, it.nivelExigido.name, it.tipo.name)
    }
}

/**
 * Consultas que comparten los servicios. Se llaman DENTRO de una transacción (`bd.tx { ... }`).
 */
object Consultas {

    fun configuracionLong(clave: String): Long =
        ConfiguracionTabla.selectAll().where { ConfiguracionTabla.clave eq clave }.single()[ConfiguracionTabla.valor].toLong()

    fun cursos(): Map<String, CursoExterno> = CursosExternos.selectAll().associate { fila ->
        fila[CursosExternos.habilidadId] to CursoExterno(
            habilidadId = fila[CursosExternos.habilidadId],
            titulo = fila[CursosExternos.titulo],
            proveedor = fila[CursosExternos.proveedor],
            url = fila[CursosExternos.url],
            horasPorNivel = fila[CursosExternos.horasPorNivel].toDouble(),
        )
    }

    fun niveles(usuarioId: String): NivelesUsuario {
        val filas = HabilidadesUsuario.selectAll().where { HabilidadesUsuario.usuarioId eq usuarioId }.toList()
        return NivelesUsuario(
            declarados = filas.associate { it[HabilidadesUsuario.habilidadId] to NivelHabilidad.valueOf(it[HabilidadesUsuario.nivelDeclarado]) },
            demostrados = filas.associate { it[HabilidadesUsuario.habilidadId] to NivelHabilidad.valueOf(it[HabilidadesUsuario.nivelDemostrado]) },
        )
    }

    /** True si la empresa todavía puede evaluar (le quedan gratis o tiene el plan). No descuenta nada. */
    fun empresaPuedeEvaluar(empresaId: String): Boolean {
        val fila = SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.empresaId eq empresaId }.singleOrNull() ?: return false
        return fila[SaldoEvaluacionesTabla.planActivo] || fila[SaldoEvaluacionesTabla.gratisRestantes] > 0
    }

    /** Carga vacantes con sus requisitos. [filtro] limita las filas de `vacantes` (null = todas). */
    fun vacantes(filtro: Op<Boolean>? = null): List<VacanteCargada> {
        val filas = (if (filtro != null) Vacantes.selectAll().where(filtro) else Vacantes.selectAll()).toList()
        if (filas.isEmpty()) return emptyList()

        val ids = filas.map { it[Vacantes.id] }
        val nombresHabilidad = Habilidades.selectAll().associate { it[Habilidades.id] to it[Habilidades.nombre] }
        val requisitos = RequisitosVacante.selectAll().where { RequisitosVacante.vacanteId inList ids }
            .orderBy(RequisitosVacante.orden)
            .groupBy { it[RequisitosVacante.vacanteId] }
        val empresas = Empresas.selectAll().where { Empresas.id inList filas.map { it[Vacantes.empresaId] }.distinct() }
            .associate { it[Empresas.id] to it[Empresas.razonSocial] }

        return filas.map { fila ->
            val empresaId = fila[Vacantes.empresaId]
            VacanteCargada(
                detalle = aDetalle(fila, requisitos[fila[Vacantes.id]].orEmpty(), nombresHabilidad, empresas[empresaId].orEmpty(), empresaPuedeEvaluar(empresaId)),
                empresaId = empresaId,
                empresaNombre = empresas[empresaId].orEmpty(),
                creada = fila[Vacantes.creadaEn].toString(),
            )
        }
    }

    fun vacante(id: String): VacanteCargada? = vacantes(Vacantes.id eq id).singleOrNull()

    private fun aDetalle(
        fila: ResultRow,
        requisitos: List<ResultRow>,
        nombresHabilidad: Map<String, String>,
        empresa: String,
        conEvaluaciones: Boolean,
    ) = VacanteDetalle(
        resumen = VacanteResumen(
            id = fila[Vacantes.id],
            puesto = fila[Vacantes.puesto],
            empresa = empresa,
            modalidad = Modalidad.valueOf(fila[Vacantes.modalidad]),
            sueldo = fila[Vacantes.sueldo],
            fechaCierre = fila[Vacantes.fechaCierre],
            probabilidad = 0,
        ),
        area = AreaInteres.valueOf(fila[Vacantes.areaInteres]),
        descripcion = fila[Vacantes.descripcion],
        queHaras = fila[Vacantes.queHara].lines().filter { it.isNotBlank() },
        requisitos = requisitos.map { r ->
            RequisitoVacante(
                id = r[RequisitosVacante.id],
                habilidadId = r[RequisitosVacante.habilidadId],
                nombre = nombresHabilidad[r[RequisitosVacante.habilidadId]] ?: r[RequisitosVacante.habilidadId],
                nivelExigido = NivelHabilidad.valueOf(r[RequisitosVacante.nivel]),
                tipo = TipoRequisito.valueOf(r[RequisitosVacante.tipo]),
            )
        },
        evaluacionActiva = fila[Vacantes.evaluacionActiva],
        empresaConEvaluaciones = conEvaluaciones,
    )
}
