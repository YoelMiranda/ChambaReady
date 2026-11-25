package pe.edu.esan.chambaready.api.servicios

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import pe.edu.esan.chambaready.api.archivos.ServicioArchivos
import pe.edu.esan.chambaready.api.archivos.TipoArchivo
import pe.edu.esan.chambaready.api.db.AreasInteres
import pe.edu.esan.chambaready.api.db.Empresas
import pe.edu.esan.chambaready.api.db.EvaluacionesHabilidades
import pe.edu.esan.chambaready.api.db.Evidencias
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Habilidades
import pe.edu.esan.chambaready.api.db.HabilidadesUsuario
import pe.edu.esan.chambaready.api.db.HorasLibresTabla
import pe.edu.esan.chambaready.api.db.IntentosTest
import pe.edu.esan.chambaready.api.db.MiniTests
import pe.edu.esan.chambaready.api.db.MovimientosEvaluacion
import pe.edu.esan.chambaready.api.db.PerfilesPracticante
import pe.edu.esan.chambaready.api.db.Postulaciones
import pe.edu.esan.chambaready.api.db.PreguntasFiltro
import pe.edu.esan.chambaready.api.db.RespuestasFiltro
import pe.edu.esan.chambaready.api.db.SaldoEvaluacionesTabla
import pe.edu.esan.chambaready.api.db.SellosPostulacion
import pe.edu.esan.chambaready.api.db.SnapshotsRanking
import pe.edu.esan.chambaready.api.db.SuscripcionesEstudiante
import pe.edu.esan.chambaready.api.db.Usuarios
import pe.edu.esan.chambaready.api.db.Vacantes
import pe.edu.esan.chambaready.api.dto.ActualizarPerfilDto
import pe.edu.esan.chambaready.api.dto.AnalizarCvDto
import pe.edu.esan.chambaready.api.dto.AutoevaluacionDto
import pe.edu.esan.chambaready.api.dto.CrearEvidenciaDto
import pe.edu.esan.chambaready.api.dto.CvAnalizadoDto
import pe.edu.esan.chambaready.api.dto.EstadoEvaluacionDto
import pe.edu.esan.chambaready.api.dto.EvaluacionDto
import pe.edu.esan.chambaready.api.dto.EvidenciaDto
import pe.edu.esan.chambaready.api.dto.IniciarEvaluacionDto
import pe.edu.esan.chambaready.api.dto.MensajeAsistenteDto
import pe.edu.esan.chambaready.api.dto.RespuestaAsistenteDto
import pe.edu.esan.chambaready.api.dto.HabilidadDetectadaDto
import pe.edu.esan.chambaready.api.dto.MiniTestDto
import pe.edu.esan.chambaready.api.dto.PreguntaDto
import pe.edu.esan.chambaready.api.dto.RankingDto
import pe.edu.esan.chambaready.api.dto.ResponderFiltroDto
import pe.edu.esan.chambaready.api.dto.ResponderMiniTestDto
import pe.edu.esan.chambaready.api.dto.ResultadoMiniTestDto
import pe.edu.esan.chambaready.api.dto.BrechaDto
import pe.edu.esan.chambaready.api.dto.CursoDto
import pe.edu.esan.chambaready.api.dto.DiagnosticoDto
import pe.edu.esan.chambaready.api.dto.HorasLibresDto
import pe.edu.esan.chambaready.api.dto.PerfilPracticanteDto
import pe.edu.esan.chambaready.api.dto.PostulacionDto
import pe.edu.esan.chambaready.api.dto.PostularDto
import pe.edu.esan.chambaready.api.dto.PreguntaFiltroDto
import pe.edu.esan.chambaready.api.dto.RespuestaFiltroDto
import pe.edu.esan.chambaready.api.dto.VacanteDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import pe.edu.esan.chambaready.api.ia.PreguntaIa
import pe.edu.esan.chambaready.api.ia.ServicioIa
import pe.edu.esan.chambaready.api.notificaciones.Notificador
import pe.edu.esan.chambaready.api.pagos.Planes
import pe.edu.esan.chambaready.core.domain.empresa.FuenteEvaluacion
import pe.edu.esan.chambaready.core.domain.empresa.ReglasEvaluaciones
import pe.edu.esan.chambaready.core.domain.empresa.ResultadoConsumo
import pe.edu.esan.chambaready.core.domain.empresa.SaldoEvaluaciones
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.reglas.DeclaracionRequisito
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import pe.edu.esan.chambaready.core.domain.reglas.ReglasNivel
import pe.edu.esan.chambaready.core.domain.reglas.ReglasRanking
import pe.edu.esan.chambaready.core.domain.reglas.ReglasCuentaRegresiva
import pe.edu.esan.chambaready.core.domain.reglas.ReglasPostulacion
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Segundos por pregunta filtro (P24). */
const val SEGUNDOS_PREGUNTA_FILTRO = 45

/** Endpoints del practicante (/v1/yo/...). Usa las mismas reglas que la app (`:core:domain`). */
class ServicioPracticante(
    private val bd: BaseDatos,
    private val reloj: Clock,
    private val ia: ServicioIa,
    private val archivos: ServicioArchivos,
    private val notificador: Notificador,
) {

    private fun hoy(): LocalDate = LocalDate.now(reloj)

    private fun ahora(): Instant = Instant.now(reloj)

    // ---------- Perfil ----------

    suspend fun perfil(usuarioId: String): PerfilPracticanteDto = bd.tx { leerPerfil(usuarioId) }

    private fun leerPerfil(usuarioId: String): PerfilPracticanteDto {
        val usuario = Usuarios.selectAll().where { Usuarios.id eq usuarioId }.single()
        val perfil = PerfilesPracticante.selectAll().where { PerfilesPracticante.usuarioId eq usuarioId }.singleOrNull()
        val niveles = Consultas.niveles(usuarioId)
        val horas = HorasLibresTabla.selectAll().where { HorasLibresTabla.usuarioId eq usuarioId }
            .associate { DayOfWeek.of(it[HorasLibresTabla.diaSemana].toInt()).name to it[HorasLibresTabla.horas].toDouble() }
        return PerfilPracticanteDto(
            nombre = usuario[Usuarios.nombre],
            universidad = perfil?.get(PerfilesPracticante.universidad).orEmpty(),
            carrera = perfil?.get(PerfilesPracticante.carrera).orEmpty(),
            ciclo = perfil?.get(PerfilesPracticante.ciclo),
            area = perfil?.get(PerfilesPracticante.areaInteres),
            onboardingCompleto = perfil?.get(PerfilesPracticante.onboardingCompleto) ?: false,
            premium = esPremium(usuarioId),
            nivelesDeclarados = niveles.declarados.mapValues { it.value.name },
            nivelesDemostrados = niveles.demostrados.mapValues { it.value.name },
            horasLibres = horas,
            cvArchivoId = perfil?.get(PerfilesPracticante.cvArchivoId),
        )
    }

    private fun esPremium(usuarioId: String): Boolean =
        SuscripcionesEstudiante.selectAll()
            .where { (SuscripcionesEstudiante.usuarioId eq usuarioId) and (SuscripcionesEstudiante.estado eq "ACTIVA") }
            .any { fila -> fila[SuscripcionesEstudiante.venceEn]?.isAfter(Instant.now(reloj)) ?: true }

    suspend fun actualizarPerfil(usuarioId: String, datos: ActualizarPerfilDto): PerfilPracticanteDto = bd.tx {
        datos.ciclo?.let { if (it !in 1..14) throw ErrorApi.SolicitudInvalida("El ciclo debe estar entre 1 y 14.") }
        datos.area?.let { area ->
            if (AreasInteres.selectAll().where { AreasInteres.codigo eq area }.empty()) throw ErrorApi.SolicitudInvalida("Área de interés desconocida: $area")
        }
        datos.nombre?.let { nombre ->
            if (nombre.isBlank()) throw ErrorApi.SolicitudInvalida("Escribe tu nombre.")
            Usuarios.update({ Usuarios.id eq usuarioId }) { it[Usuarios.nombre] = nombre.trim() }
        }
        val existe = !PerfilesPracticante.selectAll().where { PerfilesPracticante.usuarioId eq usuarioId }.empty()
        if (!existe) {
            PerfilesPracticante.insert {
                it[PerfilesPracticante.usuarioId] = usuarioId
                it[universidad] = ""
                it[carrera] = ""
                it[onboardingCompleto] = false
            }
        }
        // Un UPDATE sin columnas es SQL inválido: solo se ejecuta si llegó algún dato del perfil.
        val hayCambiosDePerfil = listOf(datos.universidad, datos.carrera, datos.ciclo, datos.area, datos.onboardingCompleto).any { it != null }
        if (hayCambiosDePerfil) PerfilesPracticante.update({ PerfilesPracticante.usuarioId eq usuarioId }) { fila ->
            datos.universidad?.let { fila[universidad] = it.trim() }
            datos.carrera?.let { fila[carrera] = it.trim() }
            datos.ciclo?.let { fila[ciclo] = it }
            datos.area?.let { fila[areaInteres] = it }
            datos.onboardingCompleto?.let { fila[onboardingCompleto] = it }
        }
        datos.nivelesDeclarados?.forEach { (habilidadId, textoNivel) ->
            val nivel = NivelHabilidad.entries.firstOrNull { it.name == textoNivel }
                ?: throw ErrorApi.SolicitudInvalida("Nivel desconocido: $textoNivel")
            if (Habilidades.selectAll().where { Habilidades.id eq habilidadId }.empty()) throw ErrorApi.SolicitudInvalida("Habilidad desconocida: $habilidadId")
            val actualizadas = HabilidadesUsuario.update({ (HabilidadesUsuario.usuarioId eq usuarioId) and (HabilidadesUsuario.habilidadId eq habilidadId) }) {
                it[nivelDeclarado] = nivel.name
            }
            if (actualizadas == 0) {
                HabilidadesUsuario.insert {
                    it[HabilidadesUsuario.usuarioId] = usuarioId
                    it[HabilidadesUsuario.habilidadId] = habilidadId
                    it[nivelDeclarado] = nivel.name
                    it[nivelDemostrado] = NivelHabilidad.NO_LO_HE_USADO.name
                }
            }
        }
        leerPerfil(usuarioId)
    }

    /** P04/P22: reemplaza las horas por día. Valida 0–8 h en pasos de 0.5 con la regla del dominio. */
    suspend fun guardarHoras(usuarioId: String, datos: HorasLibresDto): PerfilPracticanteDto = bd.tx {
        val horas = datos.horas.map { (dia, valor) ->
            val diaSemana = DayOfWeek.entries.firstOrNull { it.name == dia } ?: throw ErrorApi.SolicitudInvalida("Día desconocido: $dia")
            if (!HorasLibres.horasValidas(valor)) throw ErrorApi.SolicitudInvalida("Horas inválidas para $dia: deben ser de 0 a 8, en pasos de 0.5.")
            diaSemana to valor
        }
        HorasLibresTabla.deleteWhere { HorasLibresTabla.usuarioId eq usuarioId }
        horas.forEach { (dia, valor) ->
            HorasLibresTabla.insert {
                it[HorasLibresTabla.usuarioId] = usuarioId
                it[diaSemana] = dia.value.toShort()
                it[HorasLibresTabla.horas] = BigDecimal.valueOf(valor)
            }
        }
        leerPerfil(usuarioId)
    }

    private fun horasLibres(usuarioId: String): HorasLibres = HorasLibres(
        HorasLibresTabla.selectAll().where { HorasLibresTabla.usuarioId eq usuarioId }
            .associate { DayOfWeek.of(it[HorasLibresTabla.diaSemana].toInt()) to it[HorasLibresTabla.horas].toDouble() },
    )

    // ---------- Vacantes ----------

    suspend fun vacantes(usuarioId: String): List<VacanteDto> = bd.tx {
        Planes.sincronizarVencimientos(ahora())
        val niveles = Consultas.niveles(usuarioId)
        val conPreguntas = vacantesConPreguntas()
        Consultas.vacantes(Vacantes.fechaCierre greaterEq hoy())
            .map { aDto(it, niveles, conPreguntas) }
            .sortedByDescending { it.probabilidad }
    }

    suspend fun vacante(usuarioId: String, vacanteId: String): VacanteDto = bd.tx {
        Planes.sincronizarVencimientos(ahora())
        val vacante = Consultas.vacante(vacanteId) ?: throw ErrorApi.NoEncontrado("Esta vacante no existe.")
        aDto(vacante, Consultas.niveles(usuarioId), vacantesConPreguntas())
    }

    /** Ids de las vacantes que tienen al menos una pregunta filtro. */
    private fun vacantesConPreguntas(): Set<String> =
        PreguntasFiltro.select(PreguntasFiltro.vacanteId).withDistinct().map { it[PreguntasFiltro.vacanteId] }.toSet()

    private fun aDto(v: VacanteCargada, niveles: NivelesUsuario, conPreguntas: Set<String>) = VacanteDto(
        id = v.id,
        puesto = v.detalle.resumen.puesto,
        empresa = v.empresaNombre,
        area = v.detalle.area.name,
        modalidad = v.detalle.resumen.modalidad.name,
        sueldo = v.detalle.resumen.sueldo,
        fechaCierre = v.detalle.resumen.fechaCierre.toString(),
        diasRestantes = ReglasCuentaRegresiva.diasHastaCierre(hoy(), v.detalle.resumen.fechaCierre),
        descripcion = v.detalle.descripcion,
        queHara = v.detalle.queHaras,
        requisitos = v.requisitosDto(),
        evaluacionActiva = v.detalle.evaluacionActiva,
        probabilidad = MotorDiagnostico.probabilidad(v.detalle, niveles),
        conPreguntasFiltro = v.detalle.evaluacionActiva && v.detalle.empresaConEvaluaciones && v.id in conPreguntas,
    )

    /** P10: el resumen es gratis; el detalle (cursos) solo con Premium (regla 6). */
    suspend fun diagnostico(usuarioId: String, vacanteId: String): DiagnosticoDto = bd.tx {
        val vacante = Consultas.vacante(vacanteId) ?: throw ErrorApi.NoEncontrado("Esta vacante no existe.")
        val d = MotorDiagnostico.diagnosticar(vacante.detalle, Consultas.niveles(usuarioId), Consultas.cursos(), horasLibres(usuarioId), hoy())
        val premium = esPremium(usuarioId)
        DiagnosticoDto(
            vacanteId = vacanteId,
            probabilidadActual = d.probabilidadActual,
            probabilidadMeta = d.probabilidadMeta,
            horasNecesarias = d.horasNecesarias,
            horasDisponibles = d.viabilidad.horasDisponibles,
            viable = d.viabilidad.esViable,
            detalleBloqueado = !premium,
            brechas = d.brechas.map { b ->
                BrechaDto(
                    requisitoId = b.requisito.id,
                    habilidad = b.requisito.nombre,
                    tipo = b.requisito.tipo.name,
                    nivelActual = b.nivelActual.name,
                    nivelExigido = b.requisito.nivelExigido.name,
                    horas = b.horas,
                    curso = if (premium) b.curso?.let { CursoDto(it.titulo, it.proveedor, it.url) } else null,
                )
            },
        )
    }

    // ---------- Postular ----------

    /**
     * P24: preguntas de la vacante SIN la respuesta correcta. Lista vacía si la vacante no evalúa o la
     * empresa ya no tiene evaluaciones (regla 11): entonces se postula sin preguntas.
     */
    suspend fun preguntasFiltro(vacanteId: String): List<PreguntaFiltroDto> = bd.tx {
        Planes.sincronizarVencimientos(ahora())
        val vacante = Consultas.vacante(vacanteId) ?: throw ErrorApi.NoEncontrado("Esta vacante no existe.")
        if (!vacante.detalle.evaluacionActiva || !vacante.detalle.empresaConEvaluaciones) return@tx emptyList()
        PreguntasFiltro.selectAll().where { PreguntasFiltro.vacanteId eq vacanteId }.orderBy(PreguntasFiltro.orden).map {
            PreguntaFiltroDto(it[PreguntasFiltro.id], it[PreguntasFiltro.enunciado], it[PreguntasFiltro.opciones].lines(), SEGUNDOS_PREGUNTA_FILTRO)
        }
    }

    /**
     * P17. Todo en una transacción:
     * - Regla 7: solo se bloquea si declara no cumplir un requisito filtro.
     * - Las respuestas filtro se corrigen AQUÍ (el cliente nunca dice si acertó).
     * - Si respondió, se descuenta una evaluación de la empresa con el saldo bloqueado (FOR UPDATE),
     *   para que dos postulaciones simultáneas no usen la misma. Sin saldo, queda "Sin evaluar" (regla 11).
     */
    suspend fun postular(usuarioId: String, datos: PostularDto): PostulacionDto = bd.tx {
        if (!datos.declaracionJurada) throw ErrorApi.SolicitudInvalida("Debes aceptar la declaración jurada.")
        Planes.sincronizarVencimientos(ahora())
        val vacante = Consultas.vacante(datos.vacanteId) ?: throw ErrorApi.NoEncontrado("Esta vacante no existe.")
        if (vacante.detalle.resumen.fechaCierre.isBefore(hoy())) throw ErrorApi.ReglaDeNegocio("VACANTE_CERRADA", "Esta vacante ya cerró.")
        val yaPostulo = !Postulaciones.selectAll().where { (Postulaciones.vacanteId eq vacante.id) and (Postulaciones.usuarioId eq usuarioId) }.empty()
        if (yaPostulo) throw ErrorApi.Conflicto("Ya postulaste a esta vacante.")

        val requisitos = vacante.detalle.requisitos
        val declaraciones = requisitos.map { DeclaracionRequisito(it.tipo, datos.declaraciones[it.id] ?: true) }
        if (!ReglasPostulacion.puedePostular(declaraciones)) {
            throw ErrorApi.ReglaDeNegocio("FILTRO_NO_CUMPLIDO", "Declaraste no cumplir un requisito filtro: esta empresa no recibe postulaciones sin él.")
        }

        // Corrección de las preguntas filtro y consumo atómico del saldo.
        val preguntas = PreguntasFiltro.selectAll().where { PreguntasFiltro.vacanteId eq vacante.id }.associateBy { it[PreguntasFiltro.id] }
        val respuestas = datos.respuestasFiltro.filter { it.preguntaId in preguntas }
        var fuente: FuenteEvaluacion? = null
        if (vacante.detalle.evaluacionActiva && respuestas.isNotEmpty()) {
            val fila = SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.empresaId eq vacante.empresaId }.forUpdate().single()
            val saldo = SaldoEvaluaciones(fila[SaldoEvaluacionesTabla.gratisRestantes], fila[SaldoEvaluacionesTabla.planActivo])
            val resultado = ReglasEvaluaciones.consumir(saldo)
            if (resultado is ResultadoConsumo.Consumida) {
                SaldoEvaluacionesTabla.update({ SaldoEvaluacionesTabla.empresaId eq vacante.empresaId }) {
                    it[gratisRestantes] = resultado.nuevoSaldo.gratisRestantes
                }
                fuente = resultado.fuente
            }
        }
        val evaluada = fuente != null
        val correctasPorRequisito = if (evaluada) {
            respuestas.filter { r -> preguntas.getValue(r.preguntaId)[PreguntasFiltro.indiceCorrecto] == r.indice }
                .map { preguntas.getValue(it.preguntaId)[PreguntasFiltro.requisitoId] }.toSet()
        } else {
            emptySet()
        }

        val niveles = Consultas.niveles(usuarioId)
        val sellos = requisitos.associate { r ->
            val conEvidencia = r.nivelExigido.valor > 0 && niveles.demostrado(r.habilidadId).valor >= r.nivelExigido.valor
            val declaracion = DeclaracionRequisito(r.tipo, datos.declaraciones[r.id] ?: true, tieneEvidencia = conEvidencia || r.id in correctasPorRequisito)
            r.id to ReglasPostulacion.sello(declaracion)
        }

        val postulacionId = UUID.randomUUID().toString()
        Postulaciones.insert {
            it[id] = postulacionId
            it[vacanteId] = vacante.id
            it[Postulaciones.usuarioId] = usuarioId
            it[estado] = "NUEVA"
            it[Postulaciones.evaluada] = evaluada
            it[preguntasCorrectas] = if (evaluada) correctasPorRequisito.size else null
        }
        sellos.forEach { (requisitoId, sello) ->
            SellosPostulacion.insert {
                it[SellosPostulacion.postulacionId] = postulacionId
                it[SellosPostulacion.requisitoId] = requisitoId
                it[SellosPostulacion.sello] = sello.name
            }
        }
        if (evaluada) {
            respuestas.forEach { r ->
                RespuestasFiltro.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[RespuestasFiltro.postulacionId] = postulacionId
                    it[preguntaId] = r.preguntaId
                    it[indiceElegido] = r.indice
                    it[correcta] = preguntas.getValue(r.preguntaId)[PreguntasFiltro.indiceCorrecto] == r.indice
                }
            }
            MovimientosEvaluacion.insert {
                it[id] = UUID.randomUUID().toString()
                it[empresaId] = vacante.empresaId
                it[MovimientosEvaluacion.postulacionId] = postulacionId
                it[MovimientosEvaluacion.fuente] = fuente.name
            }
        }
        // F9: aviso a la empresa (campanita de la web) y métrica.
        Empresas.selectAll().where { Empresas.id eq vacante.empresaId }.singleOrNull()?.let { empresa ->
            val evaluacion = if (evaluada) "evaluado" else "sin evaluar"
            notificador.notificar(empresa[Empresas.usuarioId], "NUEVO_POSTULANTE", "Nuevo postulante", "Alguien postuló a «${vacante.detalle.resumen.puesto}» ($evaluacion).", vacante.id)
        }
        Metricas.registrar(usuarioId, "POSTULACION", mapOf("vacanteId" to vacante.id, "evaluada" to evaluada.toString()))
        PostulacionDto(
            id = postulacionId,
            vacanteId = vacante.id,
            puesto = vacante.detalle.resumen.puesto,
            empresa = vacante.empresaNombre,
            estado = "NUEVA",
            fecha = hoy().toString(),
            evaluada = evaluada,
            motivoDescarte = null,
            sellos = sellos.mapValues { it.value.name },
        )
    }

    /** P18: postulaciones del practicante, con el motivo de descarte si lo hay. */
    suspend fun postulaciones(usuarioId: String): List<PostulacionDto> = bd.tx {
        val filas = Postulaciones.selectAll().where { Postulaciones.usuarioId eq usuarioId }.orderBy(Postulaciones.creadaEn, SortOrder.DESC).toList()
        if (filas.isEmpty()) return@tx emptyList()
        val vacantes = Consultas.vacantes(Vacantes.id inList filas.map { it[Postulaciones.vacanteId] }).associateBy { it.id }
        val sellos = SellosPostulacion.selectAll().where { SellosPostulacion.postulacionId inList filas.map { it[Postulaciones.id] } }
            .groupBy({ it[SellosPostulacion.postulacionId] }, { it[SellosPostulacion.requisitoId] to it[SellosPostulacion.sello] })
        filas.map { fila ->
            val vacante = vacantes[fila[Postulaciones.vacanteId]]
            PostulacionDto(
                id = fila[Postulaciones.id],
                vacanteId = fila[Postulaciones.vacanteId],
                puesto = vacante?.detalle?.resumen?.puesto.orEmpty(),
                empresa = vacante?.empresaNombre.orEmpty(),
                estado = fila[Postulaciones.estado],
                fecha = fila[Postulaciones.creadaEn].atZone(ZoneId.of("America/Lima")).toLocalDate().toString(),
                evaluada = fila[Postulaciones.evaluada],
                motivoDescarte = fila[Postulaciones.motivoDescarte],
                sellos = sellos[fila[Postulaciones.id]].orEmpty().toMap(),
                preguntasPendientes = fila[Postulaciones.preguntasSolicitadas] && !fila[Postulaciones.evaluada],
            )
        }
    }

    // ---------- F6: CV con IA ----------

    /**
     * P01/P02: el CV ya se subió con POST /v1/archivos?tipo=CV. Aquí se asigna al perfil y la IA
     * detecta habilidades y proyectos. El usuario las confirma en P03 (nada cambia sus niveles aquí).
     */
    suspend fun analizarCv(usuarioId: String, datos: AnalizarCvDto): CvAnalizadoDto {
        val (archivo, catalogo) = bd.tx {
            archivos.propio(usuarioId, datos.archivoId, TipoArchivo.CV) to Habilidades.selectAll().associate { it[Habilidades.id] to it[Habilidades.nombre] }
        }
        val leido = ia.leerCv(usuarioId, archivo.mime, archivo.bytes, catalogo)
        bd.tx {
            asegurarPerfil(usuarioId)
            PerfilesPracticante.update({ PerfilesPracticante.usuarioId eq usuarioId }) { it[cvArchivoId] = archivo.id }
            Metricas.registrar(usuarioId, "CV_ANALIZADO", mapOf("habilidades" to leido.habilidades.size.toString(), "conIa" to ia.conIa.toString()))
        }
        return CvAnalizadoDto(
            archivoId = archivo.id,
            habilidades = leido.habilidades.map { HabilidadDetectadaDto(it.habilidadId, it.nombre, it.nivel) },
            proyectos = leido.proyectos,
            conIa = ia.conIa,
        )
    }

    private fun asegurarPerfil(usuarioId: String) {
        if (PerfilesPracticante.selectAll().where { PerfilesPracticante.usuarioId eq usuarioId }.empty()) {
            PerfilesPracticante.insert {
                it[PerfilesPracticante.usuarioId] = usuarioId
                it[universidad] = ""
                it[carrera] = ""
                it[onboardingCompleto] = false
            }
        }
    }

    // ---------- F6: mini-test (P15/P16), corregido en el servidor ----------

    /**
     * 3 preguntas para comprobar el nivel DECLARADO de la habilidad (o el siguiente, si ya lo demostró). Se generan una vez por habilidad y
     * nivel (y se guardan), así no se paga a Gemini cada vez. Nunca incluyen la respuesta.
     */
    suspend fun miniTest(usuarioId: String, habilidadId: String): MiniTestDto {
        val nivel = bd.tx {
            val niveles = Consultas.niveles(usuarioId)
            // Comprueba el nivel que el estudiante declaró (o el siguiente, si ya lo demostró).
            ReglasNivel.nivelDelMiniTest(niveles.declarado(habilidadId), niveles.demostrado(habilidadId))
        }
        return miniTestDeNivel(usuarioId, habilidadId, nivel)
    }

    /**
     * Mini-test de [nivel] que el usuario todavía no respondió: se reutiliza uno guardado (ahorra
     * llamadas a Gemini) o se genera uno nuevo. Así, quien repite recibe preguntas distintas.
     */
    private suspend fun miniTestDeNivel(usuarioId: String, habilidadId: String, nivel: NivelHabilidad): MiniTestDto {
        val (habilidad, guardado) = bd.tx {
            val fila = Habilidades.selectAll().where { Habilidades.id eq habilidadId }.singleOrNull()
                ?: throw ErrorApi.NoEncontrado("Esta habilidad no existe.")
            fila to miniTestNoRespondido(usuarioId, habilidadId, nivel)
        }
        val nombre = habilidad[Habilidades.nombre]
        val test = guardado ?: run {
            val preguntas = ia.preguntasMiniTest(habilidadId, nombre, nivel.name, usuarioId, habilidad[Habilidades.tipo])
            bd.tx { guardarMiniTest(habilidadId, nivel, preguntas) }
        }
        return MiniTestDto(test.first, habilidadId, nombre, nivel.name, test.second.map { PreguntaDto(it.enunciado, it.opciones) })
    }

    private fun miniTestNoRespondido(usuarioId: String, habilidadId: String, nivel: NivelHabilidad): Pair<String, List<PreguntaIa>>? {
        val respondidos = IntentosTest.selectAll().where { IntentosTest.usuarioId eq usuarioId }.mapNotNull { it[IntentosTest.miniTestId] }.toSet()
        return MiniTests.selectAll().where { (MiniTests.habilidadId eq habilidadId) and (MiniTests.nivel eq nivel.name) }
            .orderBy(MiniTests.creadoEn, SortOrder.DESC)
            .firstOrNull { it[MiniTests.id] !in respondidos }
            ?.let { it[MiniTests.id] to json.decodeFromString(ListSerializer(PreguntaIa.serializer()), it[MiniTests.preguntasJson]) }
    }

    private fun guardarMiniTest(habilidadId: String, nivel: NivelHabilidad, preguntas: List<PreguntaIa>): Pair<String, List<PreguntaIa>> {
        val testId = UUID.randomUUID().toString()
        MiniTests.insert {
            it[id] = testId
            it[MiniTests.habilidadId] = habilidadId
            it[MiniTests.nivel] = nivel.name
            it[preguntasJson] = json.encodeToString(ListSerializer(PreguntaIa.serializer()), preguntas)
            it[creadoEn] = ahora()
        }
        return testId to preguntas
    }

    /** Corrige el mini-test. Regla 4: solo un mini-test aprobado sube el nivel demostrado. */
    suspend fun responderMiniTest(usuarioId: String, miniTestId: String, datos: ResponderMiniTestDto): ResultadoMiniTestDto = bd.tx {
        val fila = MiniTests.selectAll().where { MiniTests.id eq miniTestId }.singleOrNull() ?: throw ErrorApi.NoEncontrado("Este mini-test no existe.")
        val preguntas = json.decodeFromString(ListSerializer(PreguntaIa.serializer()), fila[MiniTests.preguntasJson])
        if (datos.respuestas.size != preguntas.size) throw ErrorApi.SolicitudInvalida("Responde las ${preguntas.size} preguntas (puedes dejar alguna en blanco).")
        val habilidadId = fila[MiniTests.habilidadId]
        val nivelTest = NivelHabilidad.valueOf(fila[MiniTests.nivel])
        val correctas = preguntas.zip(datos.respuestas).count { (p, r) -> p.indiceCorrecto == r }
        val antes = Consultas.niveles(usuarioId).demostrado(habilidadId)
        val despues = ReglasNivel.trasMiniTest(antes, nivelTest, correctas)
        if (despues != antes) cambiarNivelDemostrado(usuarioId, habilidadId, despues, "MINI_TEST")
        val aprobado = ReglasNivel.miniTestAprobado(correctas, preguntas.size)
        IntentosTest.insert {
            it[id] = UUID.randomUUID().toString()
            it[IntentosTest.usuarioId] = usuarioId
            it[IntentosTest.habilidadId] = habilidadId
            it[vacanteId] = datos.vacanteId?.takeIf { v -> !Vacantes.selectAll().where { Vacantes.id eq v }.empty() }
            it[IntentosTest.correctas] = correctas
            it[total] = preguntas.size
            it[IntentosTest.aprobado] = aprobado
            it[nivelAntes] = antes.name
            it[nivelDespues] = despues.name
            it[IntentosTest.miniTestId] = miniTestId
        }
        Metricas.registrar(usuarioId, "MINI_TEST", mapOf("habilidadId" to habilidadId, "aprobado" to aprobado.toString()))
        ResultadoMiniTestDto(correctas, preguntas.size, aprobado, antes.name, despues.name, preguntas.map { it.indiceCorrecto })
    }

    private fun cambiarNivelDemostrado(usuarioId: String, habilidadId: String, nivel: NivelHabilidad, fuente: String) {
        val cambiadas = HabilidadesUsuario.update({ (HabilidadesUsuario.usuarioId eq usuarioId) and (HabilidadesUsuario.habilidadId eq habilidadId) }) {
            it[nivelDemostrado] = nivel.name
            it[fuenteDemostrado] = fuente
        }
        if (cambiadas == 0) {
            HabilidadesUsuario.insert {
                it[HabilidadesUsuario.usuarioId] = usuarioId
                it[HabilidadesUsuario.habilidadId] = habilidadId
                it[nivelDeclarado] = NivelHabilidad.NO_LO_SE.name
                it[nivelDemostrado] = nivel.name
                it[fuenteDemostrado] = fuente
            }
        }
    }

    // ---------- F6: ranking privado (regla 5) ----------

    /**
     * P06: guarda el resultado de la autoevaluación y calcula el "Top X %" dentro del área, contra el
     * último resultado de cada otro estudiante. Solo lo recibe el propio practicante: no existe
     * ningún endpoint de empresa que lo devuelva.
     */
    suspend fun autoevaluacion(usuarioId: String, datos: AutoevaluacionDto): RankingDto = bd.tx {
        if (AreasInteres.selectAll().where { AreasInteres.codigo eq datos.area }.empty()) throw ErrorApi.SolicitudInvalida("Área de interés desconocida: ${datos.area}")
        if (datos.total <= 0 || datos.correctas !in 0..datos.total) throw ErrorApi.SolicitudInvalida("Resultado inválido.")
        val puntaje = ReglasRanking.puntaje(datos.correctas, datos.total)
        val delArea = SnapshotsRanking.selectAll().where { SnapshotsRanking.areaInteres eq datos.area }
            .orderBy(SnapshotsRanking.creadoEn, SortOrder.DESC).toList()
        val ultimoPorUsuario = delArea.distinctBy { it[SnapshotsRanking.usuarioId] }
        val anterior = ultimoPorUsuario.firstOrNull { it[SnapshotsRanking.usuarioId] == usuarioId }?.get(SnapshotsRanking.topPorcentaje)
        val otros = ultimoPorUsuario.filter { it[SnapshotsRanking.usuarioId] != usuarioId }.map { it[SnapshotsRanking.puntaje].toDouble() }
        val top = ReglasRanking.topPorcentaje(puntaje, otros)
        SnapshotsRanking.insert {
            it[id] = UUID.randomUUID().toString()
            it[SnapshotsRanking.usuarioId] = usuarioId
            it[areaInteres] = datos.area
            it[topPorcentaje] = top
            it[SnapshotsRanking.puntaje] = BigDecimal.valueOf(puntaje).setScale(3, java.math.RoundingMode.HALF_UP)
            it[creadoEn] = ahora()
        }
        RankingDto(datos.area, top, anterior, otros.size + 1)
    }

    // ---------- Regla 13: responder las preguntas después de postular ----------

    /**
     * La empresa (con plan) pidió responder las preguntas a quien entró "Sin evaluar". Se corrigen
     * aquí, se gasta la evaluación de forma atómica y los requisitos acertados pasan a "Confirmado".
     */
    suspend fun responderFiltroPendiente(usuarioId: String, postulacionId: String, datos: ResponderFiltroDto): PostulacionDto {
        bd.tx {
            val p = Postulaciones.selectAll().where { (Postulaciones.id eq postulacionId) and (Postulaciones.usuarioId eq usuarioId) }.singleOrNull()
                ?: throw ErrorApi.NoEncontrado("Esta postulación no existe.")
            if (p[Postulaciones.evaluada] || !p[Postulaciones.preguntasSolicitadas]) {
                throw ErrorApi.ReglaDeNegocio("SIN_PREGUNTAS_PENDIENTES", "Esta postulación no tiene preguntas pendientes.")
            }
            val vacante = Consultas.vacante(p[Postulaciones.vacanteId]) ?: throw ErrorApi.NoEncontrado("Esta vacante no existe.")
            Planes.sincronizarVencimientos(ahora())
            val preguntas = PreguntasFiltro.selectAll().where { PreguntasFiltro.vacanteId eq vacante.id }.associateBy { it[PreguntasFiltro.id] }
            val respuestas = datos.respuestas.filter { it.preguntaId in preguntas }
            if (respuestas.isEmpty()) throw ErrorApi.SolicitudInvalida("Responde las preguntas filtro.")
            val fuente = consumirEvaluacion(vacante.empresaId)
                ?: throw ErrorApi.ReglaDeNegocio("SIN_EVALUACIONES", "La empresa ya no tiene evaluaciones disponibles.")
            val acertados = requisitosAcertados(respuestas, preguntas)
            SellosPostulacion.update({ (SellosPostulacion.postulacionId eq postulacionId) and (SellosPostulacion.requisitoId inList acertados.toList()) and (SellosPostulacion.sello eq "DECLARADO") }) {
                it[sello] = "CONFIRMADO"
            }
            Postulaciones.update({ Postulaciones.id eq postulacionId }) {
                it[evaluada] = true
                it[preguntasCorrectas] = acertados.size
                it[preguntasSolicitadas] = false
            }
            guardarRespuestas(postulacionId, respuestas, preguntas)
            MovimientosEvaluacion.insert {
                it[id] = UUID.randomUUID().toString()
                it[empresaId] = vacante.empresaId
                it[MovimientosEvaluacion.postulacionId] = postulacionId
                it[MovimientosEvaluacion.fuente] = fuente.name
            }
            Empresas.selectAll().where { Empresas.id eq vacante.empresaId }.singleOrNull()?.let { empresa ->
                notificador.notificar(empresa[Empresas.usuarioId], "PREGUNTAS_FILTRO", "Preguntas respondidas", "Un postulante a «${vacante.detalle.resumen.puesto}» respondió las preguntas filtro.", vacante.id)
            }
        }
        return postulaciones(usuarioId).first { it.id == postulacionId }
    }

    /** Descuenta una evaluación con el saldo bloqueado (FOR UPDATE). null si no hay saldo. */
    private fun consumirEvaluacion(empresaId: String): FuenteEvaluacion? {
        val fila = SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.empresaId eq empresaId }.forUpdate().single()
        val resultado = ReglasEvaluaciones.consumir(SaldoEvaluaciones(fila[SaldoEvaluacionesTabla.gratisRestantes], fila[SaldoEvaluacionesTabla.planActivo]))
        if (resultado !is ResultadoConsumo.Consumida) return null
        SaldoEvaluacionesTabla.update({ SaldoEvaluacionesTabla.empresaId eq empresaId }) { it[gratisRestantes] = resultado.nuevoSaldo.gratisRestantes }
        return resultado.fuente
    }

    private fun requisitosAcertados(respuestas: List<RespuestaFiltroDto>, preguntas: Map<String, org.jetbrains.exposed.v1.core.ResultRow>): Set<String> =
        respuestas.filter { r -> preguntas.getValue(r.preguntaId)[PreguntasFiltro.indiceCorrecto] == r.indice }
            .map { preguntas.getValue(it.preguntaId)[PreguntasFiltro.requisitoId] }.toSet()

    private fun guardarRespuestas(postulacionId: String, respuestas: List<RespuestaFiltroDto>, preguntas: Map<String, org.jetbrains.exposed.v1.core.ResultRow>) {
        respuestas.forEach { r ->
            RespuestasFiltro.insert {
                it[id] = UUID.randomUUID().toString()
                it[RespuestasFiltro.postulacionId] = postulacionId
                it[preguntaId] = r.preguntaId
                it[indiceElegido] = r.indice
                it[correcta] = preguntas.getValue(r.preguntaId)[PreguntasFiltro.indiceCorrecto] == r.indice
            }
        }
    }

    // ---------- F7: evidencias (P23) ----------

    suspend fun evidencias(usuarioId: String): List<EvidenciaDto> = bd.tx {
        val nombres = Habilidades.selectAll().associate { it[Habilidades.id] to it[Habilidades.nombre] }
        Evidencias.selectAll().where { Evidencias.usuarioId eq usuarioId }.orderBy(Evidencias.creadoEn, SortOrder.DESC).map {
            EvidenciaDto(
                id = it[Evidencias.id],
                habilidadId = it[Evidencias.habilidadId],
                habilidad = nombres[it[Evidencias.habilidadId]].orEmpty(),
                titulo = it[Evidencias.titulo],
                tipo = it[Evidencias.tipo],
                nivel = it[Evidencias.nivel],
                estado = it[Evidencias.estado],
                url = it[Evidencias.url],
                archivoId = it[Evidencias.archivoId],
                fecha = it[Evidencias.creadoEn].atZone(reloj.zone).toLocalDate().toString(),
            )
        }
    }

    /** Queda "En revisión": solo una evidencia validada por el equipo sube el nivel (regla 4). */
    suspend fun crearEvidencia(usuarioId: String, datos: CrearEvidenciaDto): EvidenciaDto {
        val evidenciaId = bd.tx {
            if (Habilidades.selectAll().where { Habilidades.id eq datos.habilidadId }.empty()) throw ErrorApi.SolicitudInvalida("Habilidad desconocida.")
            if (datos.titulo.trim().length < 3) throw ErrorApi.SolicitudInvalida("Escribe un título para la evidencia.")
            if (NivelHabilidad.exigibles.none { it.name == datos.nivel }) throw ErrorApi.SolicitudInvalida("Nivel inválido: ${datos.nivel}")
            when (datos.tipo) {
                "CERTIFICADO" -> archivos.propio(usuarioId, datos.archivoId ?: throw ErrorApi.SolicitudInvalida("Sube el certificado."), TipoArchivo.EVIDENCIA)
                "LINK" -> if (datos.url == null || !Regex("^https?://\\S+\\.\\S+").matches(datos.url.trim())) {
                    throw ErrorApi.SolicitudInvalida("Escribe el link completo, con https://")
                }
                else -> throw ErrorApi.SolicitudInvalida("Tipo de evidencia inválido: ${datos.tipo}")
            }
            val nuevaId = UUID.randomUUID().toString()
            Evidencias.insert {
                it[id] = nuevaId
                it[Evidencias.usuarioId] = usuarioId
                it[habilidadId] = datos.habilidadId
                it[titulo] = datos.titulo.trim()
                it[tipo] = datos.tipo
                it[estado] = EstadoEvidencia.EN_REVISION.name
                it[nivel] = datos.nivel
                it[url] = if (datos.tipo == "LINK") datos.url?.trim() else null
                it[archivoId] = if (datos.tipo == "CERTIFICADO") datos.archivoId else null
                it[creadoEn] = ahora()
            }
            Metricas.registrar(usuarioId, "EVIDENCIA_SUBIDA", mapOf("habilidadId" to datos.habilidadId))
            nuevaId
        }
        return evidencias(usuarioId).first { it.id == evidenciaId }
    }


    // ---------- Evaluación de habilidades (P05 y Perfil) ----------

    /** Con cuenta gratis se evalúa una sola vez; con Premium, sin límite. */
    suspend fun estadoEvaluacion(usuarioId: String): EstadoEvaluacionDto = bd.tx {
        val usadas = EvaluacionesHabilidades.selectAll().where { EvaluacionesHabilidades.usuarioId eq usuarioId }.count().toInt()
        val premium = esPremium(usuarioId)
        EstadoEvaluacionDto(puedeEvaluar = premium || usadas < EVALUACIONES_GRATIS, usadas = usadas, premium = premium)
    }

    /**
     * Arma la evaluación: un mini-test por habilidad elegida (máximo 4), al nivel que el estudiante
     * declaró. Las respuestas van a POST /v1/yo/mini-tests/{id}/respuestas, que sube el nivel
     * demostrado si aprueba: así sus requisitos pasan de "Declarado" a "Confirmado" al postular.
     */
    suspend fun iniciarEvaluacion(usuarioId: String, datos: IniciarEvaluacionDto): EvaluacionDto {
        val elegidas = datos.habilidades.distinct()
        if (elegidas.isEmpty() || elegidas.size > MAX_HABILIDADES_EVALUACION) {
            throw ErrorApi.SolicitudInvalida("Elige de 1 a $MAX_HABILIDADES_EVALUACION habilidades.")
        }
        val (evaluacionId, niveles) = bd.tx {
            val estado = EvaluacionesHabilidades.selectAll().where { EvaluacionesHabilidades.usuarioId eq usuarioId }.count()
            if (!esPremium(usuarioId) && estado >= EVALUACIONES_GRATIS) {
                throw ErrorApi.ReglaDeNegocio("EVALUACION_USADA", "Con la cuenta gratis puedes evaluarte una sola vez. Con Premium te evalúas cuando quieras.")
            }
            val niveles = Consultas.niveles(usuarioId)
            val sinDeclarar = elegidas.filter { niveles.declarado(it).valor == 0 }
            if (sinDeclarar.isNotEmpty()) throw ErrorApi.SolicitudInvalida("Solo puedes evaluar habilidades que declaraste en tu perfil.")
            val nuevaId = UUID.randomUUID().toString()
            EvaluacionesHabilidades.insert {
                it[id] = nuevaId
                it[EvaluacionesHabilidades.usuarioId] = usuarioId
                it[habilidades] = elegidas.joinToString(",")
                it[creadaEn] = ahora()
            }
            Metricas.registrar(usuarioId, "EVALUACION_HABILIDADES", mapOf("habilidades" to elegidas.size.toString()))
            nuevaId to niveles
        }
        // Fuera de la transacción: generar preguntas puede tardar (Gemini).
        val pruebas = elegidas.map { id -> miniTestDeNivel(usuarioId, id, ReglasNivel.nivelDelMiniTest(niveles.declarado(id), niveles.demostrado(id))) }
        return EvaluacionDto(evaluacionId, pruebas)
    }

    // ---------- Asistente de perfil (P02) ----------

    /** El estudiante agrega habilidades que no estaban en su CV o aclara alguna, escribiendo libremente. */
    suspend fun conversarAsistente(usuarioId: String, datos: MensajeAsistenteDto): RespuestaAsistenteDto {
        val mensaje = datos.mensaje.trim()
        if (mensaje.isEmpty()) throw ErrorApi.SolicitudInvalida("Escribe un mensaje.")
        val catalogo = bd.tx { Habilidades.selectAll().associate { it[Habilidades.id] to it[Habilidades.nombre] } }
        val r = ia.conversar(usuarioId, mensaje, datos.habilidades, catalogo)
        return RespuestaAsistenteDto(r.respuesta, r.habilidades.map { HabilidadDetectadaDto(it.habilidadId, it.nombre, it.nivel) }, ia.conIa)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        /** Evaluaciones de habilidades con la cuenta gratis. */
        const val EVALUACIONES_GRATIS = 1
        const val MAX_HABILIDADES_EVALUACION = 4
    }
}
