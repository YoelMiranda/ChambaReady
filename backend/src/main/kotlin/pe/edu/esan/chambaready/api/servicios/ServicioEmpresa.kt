package pe.edu.esan.chambaready.api.servicios

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import pe.edu.esan.chambaready.api.archivos.ServicioArchivos
import pe.edu.esan.chambaready.api.archivos.TipoArchivo
import pe.edu.esan.chambaready.api.db.AreasInteres
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Empresas
import pe.edu.esan.chambaready.api.db.Entrevistas
import pe.edu.esan.chambaready.api.db.Habilidades
import pe.edu.esan.chambaready.api.db.PerfilesPracticante
import pe.edu.esan.chambaready.api.db.Postulaciones
import pe.edu.esan.chambaready.api.db.PreguntasFiltro
import pe.edu.esan.chambaready.api.db.RequisitosVacante
import pe.edu.esan.chambaready.api.db.SaldoEvaluacionesTabla
import pe.edu.esan.chambaready.api.db.SellosPostulacion
import pe.edu.esan.chambaready.api.db.Usuarios
import pe.edu.esan.chambaready.api.db.Vacantes
import pe.edu.esan.chambaready.api.dto.ActividadDto
import pe.edu.esan.chambaready.api.dto.AgendarEntrevistaDto
import pe.edu.esan.chambaready.api.dto.CambiarEstadoDto
import pe.edu.esan.chambaready.api.dto.CrearVacanteDto
import pe.edu.esan.chambaready.api.dto.EmpresaDto
import pe.edu.esan.chambaready.api.dto.EntrevistaDto
import pe.edu.esan.chambaready.api.dto.EntrevistaEmpresaDto
import pe.edu.esan.chambaready.api.dto.LogoDto
import pe.edu.esan.chambaready.api.dto.PreguntaFiltroEmpresaDto
import pe.edu.esan.chambaready.api.dto.PreguntaNuevaDto
import pe.edu.esan.chambaready.api.dto.PreguntasSugeridasDto
import pe.edu.esan.chambaready.api.dto.PostulanteDto
import pe.edu.esan.chambaready.api.dto.SaldoDto
import pe.edu.esan.chambaready.api.dto.SugerirPreguntasDto
import pe.edu.esan.chambaready.api.dto.VacanteEmpresaDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import pe.edu.esan.chambaready.api.ia.PreguntaIa
import pe.edu.esan.chambaready.api.ia.ServicioIa
import pe.edu.esan.chambaready.api.ia.Textos
import pe.edu.esan.chambaready.api.notificaciones.Notificador
import pe.edu.esan.chambaready.api.pagos.Planes
import pe.edu.esan.chambaready.core.domain.empresa.ReglasEmpresa
import pe.edu.esan.chambaready.core.domain.empresa.RequisitoConSello
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import pe.edu.esan.chambaready.core.domain.reglas.ReglasCuentaRegresiva
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** Endpoints de la empresa (/v1/empresa/...). Una empresa SOLO ve sus vacantes y sus postulantes. */
class ServicioEmpresa(
    private val bd: BaseDatos,
    private val reloj: Clock,
    private val ia: ServicioIa,
    private val archivos: ServicioArchivos,
    private val notificador: Notificador,
) {

    private fun hoy(): LocalDate = LocalDate.now(reloj)

    private fun ahora(): Instant = Instant.now(reloj)

    private fun notificar(usuarioId: String, tipo: String, titulo: String, cuerpo: String, vacanteId: String?) =
        notificador.notificar(usuarioId, tipo, titulo, cuerpo, vacanteId)

    /** Empresa del usuario o error 404 si todavía no completó su perfil (E01). */
    private fun empresaDe(usuarioId: String): ResultRow =
        Empresas.selectAll().where { Empresas.usuarioId eq usuarioId }.singleOrNull()
            ?: throw ErrorApi.NoEncontrado("Primero completa el perfil de tu empresa.")

    // ---------- E01 Perfil ----------

    suspend fun perfil(usuarioId: String): EmpresaDto = bd.tx { aDto(empresaDe(usuarioId)) }

    private fun aDto(fila: ResultRow) = EmpresaDto(
        razonSocial = fila[Empresas.razonSocial],
        ruc = fila[Empresas.ruc],
        sector = fila[Empresas.sector],
        web = fila[Empresas.web],
        tamano = fila[Empresas.tamano],
        contactoNombre = fila[Empresas.contactoNombre],
        contactoCorreo = fila[Empresas.contactoCorreo],
        contactoTelefono = fila[Empresas.contactoTelefono],
        descripcion = fila[Empresas.descripcion],
        verificacion = fila[Empresas.verificacion],
        logoArchivoId = fila[Empresas.logoArchivoId],
    )

    /**
     * Crea o actualiza el perfil. Una empresa nueva queda "En revisión" y recibe sus evaluaciones
     * gratis (de la tabla de configuración). Cambiar el RUC obliga a verificar de nuevo.
     */
    suspend fun guardarPerfil(usuarioId: String, datos: EmpresaDto): EmpresaDto = bd.tx {
        if (datos.razonSocial.trim().length < 3) throw ErrorApi.SolicitudInvalida("Escribe la razón social.")
        val ruc = datos.ruc.trim()
        if (!ReglasEmpresa.rucValido(ruc)) throw ErrorApi.SolicitudInvalida("El RUC no es válido.")
        if (datos.contactoCorreo.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(datos.contactoCorreo.trim())) {
            throw ErrorApi.SolicitudInvalida("El correo de contacto no es válido.")
        }
        val rucDeOtra = Empresas.selectAll().where { (Empresas.ruc eq ruc) and (Empresas.usuarioId neq usuarioId) }.empty().not()
        if (rucDeOtra) throw ErrorApi.Conflicto("Ese RUC ya está registrado por otra cuenta.")

        val actual = Empresas.selectAll().where { Empresas.usuarioId eq usuarioId }.singleOrNull()
        if (actual == null) {
            val empresaId = UUID.randomUUID().toString()
            Empresas.insert {
                it[id] = empresaId
                it[Empresas.usuarioId] = usuarioId
                it[verificacion] = "EN_REVISION"
                it[Empresas.ruc] = ruc
                llenar(it, datos)
            }
            SaldoEvaluacionesTabla.insert {
                it[SaldoEvaluacionesTabla.empresaId] = empresaId
                it[gratisRestantes] = Consultas.configuracionLong("evaluaciones_gratis").toInt()
                it[planActivo] = false
            }
        } else {
            val cambioRuc = actual[Empresas.ruc] != ruc
            Empresas.update({ Empresas.usuarioId eq usuarioId }) {
                it[Empresas.ruc] = ruc
                if (cambioRuc) it[verificacion] = "EN_REVISION"
                llenar(it, datos)
            }
        }
        aDto(empresaDe(usuarioId))
    }

    private fun llenar(it: UpdateBuilder<*>, d: EmpresaDto) {
        it[Empresas.razonSocial] = d.razonSocial.trim()
        it[Empresas.sector] = d.sector.trim()
        it[Empresas.web] = d.web.trim()
        it[Empresas.tamano] = d.tamano.trim()
        it[Empresas.contactoNombre] = d.contactoNombre.trim()
        it[Empresas.contactoCorreo] = d.contactoCorreo.trim()
        it[Empresas.contactoTelefono] = d.contactoTelefono.trim()
        it[Empresas.descripcion] = d.descripcion.trim()
    }

    /** F7: el logo se sube con POST /v1/archivos?tipo=LOGO y aquí se asigna a la empresa. */
    suspend fun guardarLogo(usuarioId: String, datos: LogoDto): EmpresaDto = bd.tx {
        val empresa = empresaDe(usuarioId)
        archivos.propio(usuarioId, datos.archivoId, TipoArchivo.LOGO)
        Empresas.update({ Empresas.id eq empresa[Empresas.id] }) { it[logoArchivoId] = datos.archivoId }
        aDto(empresaDe(usuarioId))
    }

    suspend fun saldo(usuarioId: String): SaldoDto = bd.tx {
        val empresaId = empresaDe(usuarioId)[Empresas.id]
        Planes.sincronizarVencimientos(ahora())
        val fila = SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.empresaId eq empresaId }.single()
        SaldoDto(
            gratisRestantes = fila[SaldoEvaluacionesTabla.gratisRestantes],
            gratisTotales = Consultas.configuracionLong("evaluaciones_gratis").toInt(),
            planActivo = fila[SaldoEvaluacionesTabla.planActivo],
            planVenceEn = Planes.venceEmpresa(empresaId, ahora())?.atZone(reloj.zone)?.toLocalDate()?.toString(),
        )
    }

    // ---------- E03 Vacantes ----------

    /**
     * Publicar es gratis, pero la empresa debe estar verificada.
     * Preguntas filtro (F6): si la vacante evalúa y la empresa no mandó preguntas, se generan con IA
     * (o simuladas) para los requisitos más importantes. La IA se llama ANTES de abrir la transacción.
     */
    suspend fun crearVacante(usuarioId: String, datos: CrearVacanteDto): VacanteEmpresaDto {
        val preguntas = when {
            !datos.evaluacionActiva -> emptyList()
            datos.preguntasFiltro.isNotEmpty() -> datos.preguntasFiltro.also { validarPreguntas(it, datos.requisitos.size) }
            else -> {
                val empresaId = bd.tx { empresaDe(usuarioId)[Empresas.id] }
                sugerir(datos.puesto, datos.requisitos, empresaId)
            }
        }
        return guardarVacante(usuarioId, datos, preguntas)
    }

    /** E03: preguntas sugeridas para que la empresa las revise antes de publicar. */
    suspend fun sugerirPreguntas(usuarioId: String, datos: SugerirPreguntasDto): PreguntasSugeridasDto {
        if (datos.requisitos.isEmpty()) throw ErrorApi.SolicitudInvalida("Agrega al menos un requisito.")
        val empresaId = bd.tx { empresaDe(usuarioId)[Empresas.id] }
        return PreguntasSugeridasDto(sugerir(datos.puesto, datos.requisitos, empresaId), ia.conIa)
    }

    private suspend fun sugerir(puesto: String, requisitos: List<pe.edu.esan.chambaready.api.dto.CrearRequisitoDto>, empresaId: String): List<PreguntaNuevaDto> {
        val indices = indicesParaPreguntas(requisitos.map { it.filtro to it.tipo })
        val catalogo = bd.tx { Habilidades.selectAll().associate { Textos.normalizar(it[Habilidades.nombre]) to it[Habilidades.id] } }
        val elegidos = indices.map { i ->
            val r = requisitos[i]
            Triple(catalogo[Textos.normalizar(r.habilidad)] ?: Textos.normalizar(r.habilidad), r.habilidad.trim(), r.nivel)
        }
        return ia.preguntasFiltro(puesto.trim(), elegidos, empresaId).zip(indices) { p: PreguntaIa, i ->
            PreguntaNuevaDto(i, p.enunciado, p.opciones, p.indiceCorrecto)
        }
    }

    private fun validarPreguntas(preguntas: List<PreguntaNuevaDto>, cantidadRequisitos: Int) {
        if (preguntas.size > MAX_PREGUNTAS_FILTRO) throw ErrorApi.SolicitudInvalida("Máximo $MAX_PREGUNTAS_FILTRO preguntas filtro.")
        preguntas.forEach { p ->
            val opciones = p.opciones.map { it.trim() }
            val valida = p.requisitoIndice in 0 until cantidadRequisitos &&
                p.enunciado.trim().length >= 10 &&
                opciones.size == 4 && opciones.all { it.isNotEmpty() } && opciones.map { it.lowercase() }.toSet().size == 4 &&
                p.indiceCorrecto in 0..3
            if (!valida) throw ErrorApi.SolicitudInvalida("Cada pregunta filtro necesita un enunciado, 4 opciones distintas y una correcta.")
        }
    }

    private suspend fun guardarVacante(usuarioId: String, datos: CrearVacanteDto, preguntas: List<PreguntaNuevaDto>): VacanteEmpresaDto = bd.tx {
        val empresa = empresaDe(usuarioId)
        if (empresa[Empresas.verificacion] != "VERIFICADA") {
            throw ErrorApi.ReglaDeNegocio("EMPRESA_NO_VERIFICADA", "Tu empresa aún no está verificada: no puedes publicar vacantes.")
        }
        if (datos.puesto.trim().length < 5) throw ErrorApi.SolicitudInvalida("Escribe el nombre del puesto.")
        if (datos.descripcion.trim().length < 20) throw ErrorApi.SolicitudInvalida("Describe la práctica (20 caracteres como mínimo).")
        if (AreasInteres.selectAll().where { AreasInteres.codigo eq datos.area }.empty()) throw ErrorApi.SolicitudInvalida("Área desconocida: ${datos.area}")
        if (Modalidad.entries.none { it.name == datos.modalidad }) throw ErrorApi.SolicitudInvalida("Modalidad desconocida: ${datos.modalidad}")
        val cierre = runCatching { LocalDate.parse(datos.fechaCierre) }.getOrNull() ?: throw ErrorApi.SolicitudInvalida("Fecha de cierre inválida.")
        if (!cierre.isAfter(hoy())) throw ErrorApi.SolicitudInvalida("La fecha de cierre debe ser después de hoy.")
        val tareas = datos.queHara.map { it.trim() }.filter { it.isNotEmpty() }
        if (tareas.isEmpty()) throw ErrorApi.SolicitudInvalida("Agrega al menos una tarea.")
        if (datos.requisitos.isEmpty()) throw ErrorApi.SolicitudInvalida("Agrega al menos un requisito.")

        val vacanteId = UUID.randomUUID().toString()
        Vacantes.insert {
            it[id] = vacanteId
            it[empresaId] = empresa[Empresas.id]
            it[puesto] = datos.puesto.trim()
            it[areaInteres] = datos.area
            it[modalidad] = datos.modalidad
            it[sueldo] = datos.sueldo?.trim()?.takeIf { s -> s.isNotEmpty() }
            it[fechaCierre] = cierre
            it[descripcion] = datos.descripcion.trim()
            it[queHara] = tareas.joinToString("\n")
            it[evaluacionActiva] = datos.evaluacionActiva
        }
        val idsRequisitos = datos.requisitos.mapIndexed { i, r ->
            if (NivelHabilidad.exigibles.none { it.name == r.nivel }) throw ErrorApi.SolicitudInvalida("Nivel inválido: ${r.nivel}")
            if (r.tipo !in setOf("OBLIGATORIO", "DESEABLE")) throw ErrorApi.SolicitudInvalida("Tipo inválido: ${r.tipo}")
            val requisitoId = UUID.randomUUID().toString()
            RequisitosVacante.insert {
                it[id] = requisitoId
                it[RequisitosVacante.vacanteId] = vacanteId
                it[habilidadId] = habilidadPorNombre(r.habilidad)
                it[nivel] = r.nivel
                // Un requisito filtro es siempre obligatorio y pesa más (regla 3).
                it[tipo] = if (r.filtro) "FILTRO" else r.tipo
                it[orden] = i
            }
            requisitoId
        }
        preguntas.forEachIndexed { orden, p ->
            PreguntasFiltro.insert {
                it[id] = UUID.randomUUID().toString()
                it[PreguntasFiltro.vacanteId] = vacanteId
                it[requisitoId] = idsRequisitos[p.requisitoIndice]
                it[enunciado] = p.enunciado.trim()
                it[opciones] = p.opciones.joinToString("\n") { o -> o.trim().replace("\n", " ") }
                it[indiceCorrecto] = p.indiceCorrecto
                it[PreguntasFiltro.orden] = orden
            }
        }
        avisarNuevaVacante(vacanteId, datos.area, datos.puesto.trim())
        Metricas.registrar(usuarioId, "VACANTE_PUBLICADA", mapOf("vacanteId" to vacanteId))
        vacantesDe(empresa[Empresas.id]).first { it.id == vacanteId }
    }

    /** F9: avisa a los practicantes con esa área de interés (dentro de la app). */
    private fun avisarNuevaVacante(vacanteId: String, area: String, puesto: String) {
        PerfilesPracticante.selectAll()
            .where { (PerfilesPracticante.areaInteres eq area) and (PerfilesPracticante.onboardingCompleto eq true) }
            .forEach { notificar(it[PerfilesPracticante.usuarioId], "NUEVA_VACANTE", "Nueva vacante en tu área", "Se publicó «$puesto». Mira tu probabilidad.", vacanteId) }
    }

    /** E03: preguntas filtro de una vacante propia, con la respuesta correcta. */
    suspend fun preguntasDeVacante(usuarioId: String, vacanteId: String): List<PreguntaFiltroEmpresaDto> = bd.tx {
        val vacante = vacantePropia(empresaDe(usuarioId)[Empresas.id], vacanteId)
        PreguntasFiltro.selectAll().where { PreguntasFiltro.vacanteId eq vacante.id }.orderBy(PreguntasFiltro.orden).map {
            PreguntaFiltroEmpresaDto(it[PreguntasFiltro.id], it[PreguntasFiltro.requisitoId], it[PreguntasFiltro.enunciado], it[PreguntasFiltro.opciones].lines(), it[PreguntasFiltro.indiceCorrecto])
        }
    }

    /** Busca la habilidad por nombre (sin tildes ni mayúsculas); si no existe, la agrega al catálogo. */
    private fun habilidadPorNombre(nombre: String): String {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) throw ErrorApi.SolicitudInvalida("Escribe la habilidad de cada requisito.")
        val clave = Textos.normalizar(limpio)
        Habilidades.selectAll().firstOrNull { Textos.normalizar(it[Habilidades.nombre]) == clave || it[Habilidades.id] == clave }
            ?.let { return it[Habilidades.id] }
        Habilidades.insert {
            it[id] = clave
            it[Habilidades.nombre] = limpio
            it[tipo] = "TECNICA"
        }
        return clave
    }

    suspend fun vacantes(usuarioId: String): List<VacanteEmpresaDto> = bd.tx { vacantesDe(empresaDe(usuarioId)[Empresas.id]) }

    private fun vacantesDe(empresaId: String): List<VacanteEmpresaDto> {
        val vacantes = Consultas.vacantes(Vacantes.empresaId eq empresaId)
        val postulaciones = Postulaciones.selectAll().where { Postulaciones.vacanteId inList vacantes.map { it.id } }.toList()
        val sellos = SellosPostulacion.selectAll().where { SellosPostulacion.postulacionId inList postulaciones.map { it[Postulaciones.id] } }
            .groupBy({ it[SellosPostulacion.postulacionId] }, { it[SellosPostulacion.sello] })
        return vacantes.map { v ->
            val dePostulacion = postulaciones.filter { it[Postulaciones.vacanteId] == v.id }
            val dias = ReglasCuentaRegresiva.diasHastaCierre(hoy(), v.detalle.resumen.fechaCierre)
            VacanteEmpresaDto(
                id = v.id,
                puesto = v.detalle.resumen.puesto,
                area = v.detalle.area.name,
                modalidad = v.detalle.resumen.modalidad.name,
                sueldo = v.detalle.resumen.sueldo,
                descripcion = v.detalle.descripcion,
                queHara = v.detalle.queHaras,
                creada = v.creada,
                fechaCierre = v.detalle.resumen.fechaCierre.toString(),
                diasParaCierre = dias,
                estado = if (dias < 0) "CERRADA" else "ACTIVA",
                evaluacionActiva = v.detalle.evaluacionActiva,
                requisitos = v.requisitosDto(),
                postulantes = dePostulacion.size,
                evaluados = dePostulacion.count { it[Postulaciones.evaluada] },
                confirmados = dePostulacion.count { p -> sellos[p[Postulaciones.id]].orEmpty().let { it.isNotEmpty() && it.all { s -> s == "CONFIRMADO" } } },
            )
        }.sortedByDescending { it.creada }
    }

    // ---------- E04 Candidatos ----------

    /** Vacante de ESTA empresa o 404 (no se revela si existe una vacante de otra empresa). */
    private fun vacantePropia(empresaId: String, vacanteId: String): VacanteCargada =
        Consultas.vacante(vacanteId)?.takeIf { it.empresaId == empresaId } ?: throw ErrorApi.NoEncontrado("Esta vacante no existe.")

    suspend fun postulantes(usuarioId: String, vacanteId: String): List<PostulanteDto> = bd.tx {
        val vacante = vacantePropia(empresaDe(usuarioId)[Empresas.id], vacanteId)
        val filas = Postulaciones.selectAll().where { Postulaciones.vacanteId eq vacante.id }.toList()
        val usuarios = Usuarios.selectAll().where { Usuarios.id inList filas.map { it[Postulaciones.usuarioId] } }.associateBy { it[Usuarios.id] }
        val perfiles = PerfilesPracticante.selectAll().where { PerfilesPracticante.usuarioId inList filas.map { it[Postulaciones.usuarioId] } }
            .associateBy { it[PerfilesPracticante.usuarioId] }
        val sellos = SellosPostulacion.selectAll().where { SellosPostulacion.postulacionId inList filas.map { it[Postulaciones.id] } }
            .groupBy({ it[SellosPostulacion.postulacionId] }, { it[SellosPostulacion.requisitoId] to it[SellosPostulacion.sello] })
        filas.map { p ->
            val sellosP = sellos[p[Postulaciones.id]].orEmpty().toMap()
            val perfil = perfiles[p[Postulaciones.usuarioId]]
            PostulanteDto(
                id = p[Postulaciones.id],
                vacanteId = vacante.id,
                nombre = usuarios[p[Postulaciones.usuarioId]]?.get(Usuarios.nombre).orEmpty(),
                carrera = perfil?.get(PerfilesPracticante.carrera).orEmpty(),
                universidad = perfil?.get(PerfilesPracticante.universidad).orEmpty(),
                ciclo = perfil?.get(PerfilesPracticante.ciclo),
                fecha = p[Postulaciones.creadaEn].atZone(ZoneId.of("America/Lima")).toLocalDate().toString(),
                evaluado = p[Postulaciones.evaluada],
                preguntasCorrectas = p[Postulaciones.preguntasCorrectas],
                sellos = sellosP,
                estado = p[Postulaciones.estado],
                motivoDescarte = p[Postulaciones.motivoDescarte],
                ajuste = ReglasEmpresa.ajuste(
                    vacante.detalle.requisitos.map { r -> RequisitoConSello(r.tipo, TipoSello.valueOf(sellosP[r.id] ?: "DECLARADO")) },
                ),
                cvArchivoId = perfil?.get(PerfilesPracticante.cvArchivoId),
                preguntasSolicitadas = p[Postulaciones.preguntasSolicitadas] && !p[Postulaciones.evaluada],
            )
        }.sortedByDescending { it.ajuste }
    }

    /** Postulación a una vacante de ESTA empresa o 404. */
    private fun postulacionPropia(empresaId: String, postulacionId: String): ResultRow {
        val p = Postulaciones.selectAll().where { Postulaciones.id eq postulacionId }.singleOrNull()
            ?: throw ErrorApi.NoEncontrado("Esta postulación no existe.")
        vacantePropia(empresaId, p[Postulaciones.vacanteId])
        return p
    }

    /** Cambia el estado. Descartar exige motivo (el practicante lo ve con "Mejorar esto"). */
    suspend fun cambiarEstado(usuarioId: String, postulacionId: String, datos: CambiarEstadoDto): PostulanteDto {
        val vacanteId = bd.tx {
            val p = postulacionPropia(empresaDe(usuarioId)[Empresas.id], postulacionId)
            if (datos.estado !in setOf("NUEVA", "REVISION", "ENTREVISTA", "ELEGIDA", "DESCARTADA")) throw ErrorApi.SolicitudInvalida("Estado desconocido: ${datos.estado}")
            val motivo = datos.motivo?.trim().orEmpty()
            if (datos.estado == "DESCARTADA" && motivo.length < 10) {
                throw ErrorApi.ReglaDeNegocio("MOTIVO_OBLIGATORIO", "Escribe el motivo del descarte (10 caracteres como mínimo): el practicante lo verá.")
            }
            Postulaciones.update({ Postulaciones.id eq postulacionId }) {
                it[estado] = datos.estado
                it[motivoDescarte] = if (datos.estado == "DESCARTADA") motivo else null
            }
            notificar(p[Postulaciones.usuarioId], "CAMBIO_ESTADO", "Tu postulación cambió de estado", "Revisa tu postulación en Mis postulaciones.", p[Postulaciones.vacanteId])
            p[Postulaciones.vacanteId]
        }
        return postulantes(usuarioId, vacanteId).first { it.id == postulacionId }
    }

    // ---------- E07 Entrevistas ----------

    /**
     * Agenda una entrevista: no en días pasados ni domingos, de 9:00 a 18:00 sin cruzar el almuerzo,
     * y sin chocar con otra entrevista de la empresa. Pasa la postulación a Entrevista y avisa al practicante.
     */
    suspend fun agendar(usuarioId: String, datos: AgendarEntrevistaDto): EntrevistaDto = bd.tx {
        val empresaId = empresaDe(usuarioId)[Empresas.id]
        val p = postulacionPropia(empresaId, datos.postulacionId)
        if (p[Postulaciones.estado] == "DESCARTADA") throw ErrorApi.ReglaDeNegocio("POSTULACION_DESCARTADA", "No puedes entrevistar a un postulante descartado.")
        val fecha = runCatching { LocalDate.parse(datos.fecha) }.getOrNull() ?: throw ErrorApi.SolicitudInvalida("Fecha inválida.")
        val hora = runCatching { LocalTime.parse(datos.hora) }.getOrNull() ?: throw ErrorApi.SolicitudInvalida("Hora inválida.")
        if (datos.duracionMin !in setOf(30, 45, 60)) throw ErrorApi.SolicitudInvalida("La duración debe ser de 30, 45 o 60 minutos.")
        if (datos.modalidad !in setOf("VIDEOLLAMADA", "PRESENCIAL")) throw ErrorApi.SolicitudInvalida("Modalidad inválida.")
        if (fecha.isBefore(hoy()) || fecha.dayOfWeek == DayOfWeek.SUNDAY) throw ErrorApi.ReglaDeNegocio("FECHA_NO_DISPONIBLE", "Elige un día desde hoy, de lunes a sábado.")
        val inicio = hora.toSecondOfDay() / 60
        val fin = inicio + datos.duracionMin
        val dentroDeHorario = inicio >= 9 * 60 && fin <= 18 * 60 && (fin <= 13 * 60 || inicio >= 14 * 60)
        if (!dentroDeHorario) throw ErrorApi.ReglaDeNegocio("HORA_NO_DISPONIBLE", "Elige un horario entre 9:00 y 18:00, fuera de la hora de almuerzo.")
        if (fecha == hoy() && hora.isBefore(LocalTime.now(reloj))) throw ErrorApi.ReglaDeNegocio("HORA_NO_DISPONIBLE", "Ese horario ya pasó.")
        val cruza = Entrevistas.selectAll().where { (Entrevistas.empresaId eq empresaId) and (Entrevistas.fecha eq fecha) }.any { e ->
            val a = e[Entrevistas.hora].toSecondOfDay() / 60
            inicio < a + e[Entrevistas.duracionMin] && a < fin
        }
        if (cruza) throw ErrorApi.Conflicto("Ese horario se cruza con otra entrevista.")

        val entrevistaId = UUID.randomUUID().toString()
        Entrevistas.insert {
            it[id] = entrevistaId
            it[postulacionId] = datos.postulacionId
            it[Entrevistas.empresaId] = empresaId
            it[Entrevistas.fecha] = fecha
            it[Entrevistas.hora] = hora
            it[duracionMin] = datos.duracionMin
            it[modalidad] = datos.modalidad
            it[mensaje] = datos.mensaje.trim()
        }
        Postulaciones.update({ Postulaciones.id eq datos.postulacionId }) { it[estado] = "ENTREVISTA" }
        // En F9 esta notificación también se envía por FCM.
        notificar(p[Postulaciones.usuarioId], "ENTREVISTA", "Entrevista agendada", "Tienes una entrevista el $fecha a las ${datos.hora}.", p[Postulaciones.vacanteId])
        EntrevistaDto(entrevistaId, datos.postulacionId, fecha.toString(), datos.hora, datos.duracionMin, datos.modalidad, datos.mensaje.trim())
    }

    // ---------- Vistas para la web (F5) ----------

    /** Una postulación de ESTA empresa (para E05/E07). */
    suspend fun postulante(usuarioId: String, postulacionId: String): PostulanteDto {
        val vacanteId = bd.tx { postulacionPropia(empresaDe(usuarioId)[Empresas.id], postulacionId)[Postulaciones.vacanteId] }
        return postulantes(usuarioId, vacanteId).first { it.id == postulacionId }
    }

    /** Entrevistas de la empresa, con el nombre del postulante y el puesto. */
    suspend fun entrevistas(usuarioId: String): List<EntrevistaEmpresaDto> = bd.tx {
        val empresaId = empresaDe(usuarioId)[Empresas.id]
        val filas = Entrevistas.selectAll().where { Entrevistas.empresaId eq empresaId }.toList()
        val postulaciones = Postulaciones.selectAll().where { Postulaciones.id inList filas.map { it[Entrevistas.postulacionId] } }
            .associateBy { it[Postulaciones.id] }
        val nombres = Usuarios.selectAll().where { Usuarios.id inList postulaciones.values.map { it[Postulaciones.usuarioId] } }
            .associate { it[Usuarios.id] to it[Usuarios.nombre] }
        val puestos = Vacantes.selectAll().where { Vacantes.empresaId eq empresaId }.associate { it[Vacantes.id] to it[Vacantes.puesto] }
        filas.mapNotNull { e ->
            val p = postulaciones[e[Entrevistas.postulacionId]] ?: return@mapNotNull null
            EntrevistaEmpresaDto(
                id = e[Entrevistas.id],
                postulacionId = p[Postulaciones.id],
                vacanteId = p[Postulaciones.vacanteId],
                nombrePostulante = nombres[p[Postulaciones.usuarioId]].orEmpty(),
                puesto = puestos[p[Postulaciones.vacanteId]].orEmpty(),
                fecha = e[Entrevistas.fecha].toString(),
                hora = e[Entrevistas.hora].toString().take(5),
                duracionMin = e[Entrevistas.duracionMin],
                modalidad = e[Entrevistas.modalidad],
                mensaje = e[Entrevistas.mensaje],
            )
        }.sortedWith(compareBy({ it.fecha }, { it.hora }))
    }

    /**
     * Regla 13: con el Plan Empresa se puede enviar las preguntas filtro a quien entró "Sin evaluar".
     * Se le avisa al practicante (en F9 por FCM) para que las responda.
     */
    suspend fun enviarPreguntas(usuarioId: String, postulacionId: String) = bd.tx {
        val empresaId = empresaDe(usuarioId)[Empresas.id]
        val p = postulacionPropia(empresaId, postulacionId)
        Planes.sincronizarVencimientos(ahora())
        val planActivo = SaldoEvaluacionesTabla.selectAll().where { SaldoEvaluacionesTabla.empresaId eq empresaId }.single()[SaldoEvaluacionesTabla.planActivo]
        if (!planActivo) throw ErrorApi.ReglaDeNegocio("PLAN_REQUERIDO", "Necesitas el Plan Empresa para evaluar a este postulante.")
        if (p[Postulaciones.evaluada]) throw ErrorApi.Conflicto("Este postulante ya fue evaluado.")
        val sinPreguntas = PreguntasFiltro.selectAll().where { PreguntasFiltro.vacanteId eq p[Postulaciones.vacanteId] }.empty()
        if (sinPreguntas) throw ErrorApi.ReglaDeNegocio("SIN_PREGUNTAS", "Esta vacante no tiene preguntas filtro.")
        Postulaciones.update({ Postulaciones.id eq postulacionId }) { it[preguntasSolicitadas] = true }
        notificar(p[Postulaciones.usuarioId], "PREGUNTAS_FILTRO", "Responde las preguntas filtro", "La empresa te pide responder las preguntas para confirmar tus requisitos.", p[Postulaciones.vacanteId])
    }

    /** Actividad reciente del panel (E02): postulaciones y entrevistas más nuevas. */
    suspend fun actividad(usuarioId: String): List<ActividadDto> = bd.tx {
        val empresaId = empresaDe(usuarioId)[Empresas.id]
        val vacantes = Vacantes.selectAll().where { Vacantes.empresaId eq empresaId }.associate { it[Vacantes.id] to it[Vacantes.puesto] }
        val postulaciones = Postulaciones.selectAll().where { Postulaciones.vacanteId inList vacantes.keys.toList() }.toList()
        val nombres = Usuarios.selectAll().where { Usuarios.id inList postulaciones.map { it[Postulaciones.usuarioId] } }
            .associate { it[Usuarios.id] to it[Usuarios.nombre] }
        postulaciones.map { p ->
            val evaluacion = if (p[Postulaciones.evaluada]) "evaluado" else "sin evaluar"
            ActividadDto(
                id = p[Postulaciones.id],
                texto = "${nombres[p[Postulaciones.usuarioId]].orEmpty()} postuló a «${vacantes[p[Postulaciones.vacanteId]]}» ($evaluacion)",
                fecha = p[Postulaciones.creadaEn].toString(),
            )
        }.sortedByDescending { it.fecha }.take(10)
    }

    companion object {
        const val MAX_PREGUNTAS_FILTRO = 2

        /**
         * Requisitos que llevan pregunta filtro: primero los filtro, luego los obligatorios, luego los
         * deseables (en el orden en que la empresa los escribió); máximo [MAX_PREGUNTAS_FILTRO].
         * @param requisitos (es filtro, tipo) de cada requisito.
         */
        fun indicesParaPreguntas(requisitos: List<Pair<Boolean, String>>): List<Int> =
            requisitos.indices.sortedBy { i ->
                val (filtro, tipo) = requisitos[i]
                when {
                    filtro -> 0
                    tipo == "OBLIGATORIO" -> 1
                    else -> 2
                }
            }.take(MAX_PREGUNTAS_FILTRO)
    }
}
