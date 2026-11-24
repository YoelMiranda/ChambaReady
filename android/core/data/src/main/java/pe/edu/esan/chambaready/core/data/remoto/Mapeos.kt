package pe.edu.esan.chambaready.core.data.remoto

import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.EstadoPostulacion
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.domain.modelo.RequisitoVacante
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/*
 * Conversiones entre el JSON de la API y los modelos del dominio. Las pantallas solo conocen el
 * dominio; si el backend cambia, se arregla aquí. Probadas en MapeosTest.
 */

/** Busca un valor de enum por su nombre exacto; null si el backend manda uno que no conocemos. */
private inline fun <reified E : Enum<E>> enumONull(nombre: String?): E? =
    enumValues<E>().firstOrNull { it.name == nombre }

private fun fechaONull(texto: String): LocalDate? =
    try {
        LocalDate.parse(texto)
    } catch (_: DateTimeParseException) {
        null
    }

/**
 * Vacante de la API → dominio. Devuelve null si trae un área, modalidad o fecha desconocida
 * (por ejemplo, si el backend agregó un área nueva y la app aún no se actualizó): mejor no mostrarla
 * que mostrarla mal. Un requisito con nivel o tipo desconocido se omite.
 */
fun VacanteDto.aDominio(): VacanteDetalle? {
    val areaDominio = enumONull<AreaInteres>(area) ?: return null
    val modalidadDominio = enumONull<Modalidad>(modalidad) ?: return null
    val cierre = fechaONull(fechaCierre) ?: return null
    return VacanteDetalle(
        resumen = VacanteResumen(
            id = id,
            puesto = puesto,
            empresa = empresa,
            modalidad = modalidadDominio,
            sueldo = sueldo,
            fechaCierre = cierre,
            probabilidad = probabilidad.coerceIn(0, 100),
        ),
        area = areaDominio,
        descripcion = descripcion,
        queHaras = queHara,
        requisitos = requisitos.mapNotNull { it.aDominio() },
        evaluacionActiva = evaluacionActiva,
        // El backend ya calculó si hay preguntas que responder (evalúa, tiene preguntas y la empresa
        // tiene saldo). Si no, se postula sin preguntas y queda "Sin evaluar" (regla 11).
        empresaConEvaluaciones = conPreguntasFiltro,
    )
}

fun RequisitoDto.aDominio(): RequisitoVacante? {
    val nivelDominio = enumONull<NivelHabilidad>(nivel) ?: return null
    val tipoDominio = enumONull<TipoRequisito>(tipo) ?: return null
    return RequisitoVacante(id = id, habilidadId = habilidadId, nombre = habilidad, nivelExigido = nivelDominio, tipo = tipoDominio)
}

/**
 * La empresa maneja su propio flujo (E04); el practicante ve uno más simple (P18).
 * Estados desconocidos se muestran como "Enviada".
 */
fun estadoParaPracticante(estadoEmpresa: String): EstadoPostulacion = when (estadoEmpresa) {
    "REVISION" -> EstadoPostulacion.EN_REVISION
    "ENTREVISTA" -> EstadoPostulacion.ENTREVISTA
    "ELEGIDA" -> EstadoPostulacion.SELECCIONADO
    "DESCARTADA" -> EstadoPostulacion.NO_SELECCIONADO
    else -> EstadoPostulacion.ENVIADA
}

fun PostulacionDto.aDominio(): Postulacion = Postulacion(
    vacanteId = vacanteId,
    puesto = puesto,
    empresa = empresa,
    estado = estadoParaPracticante(estado),
    fechaEnvio = fechaONull(fecha) ?: LocalDate.MIN,
    motivoDescarte = motivoDescarte,
    sellos = sellos.mapNotNull { (requisitoId, sello) -> enumONull<TipoSello>(sello)?.let { requisitoId to it } }.toMap(),
    id = id,
    preguntasPendientes = preguntasPendientes,
)

/** Una pregunta del backend nunca trae la respuesta: [PreguntaOpcion.indiceCorrecto] queda en [SIN_RESPUESTA]. */
const val SIN_RESPUESTA = -1

fun PreguntaFiltroDto.aDominio(): PreguntaOpcion = PreguntaOpcion(id = id, enunciado = enunciado, opciones = opciones, indiceCorrecto = SIN_RESPUESTA)

/** Notificación del backend → dominio, con la hora de Lima. Un tipo que la app no conoce se omite. */
fun NotificacionDto.aDominio(zona: ZoneId = ZoneId.of("America/Lima")): Notificacion? {
    val tipoDominio = enumONull<TipoNotificacion>(tipo) ?: return null
    val instante = runCatching { Instant.parse(fecha) }.getOrNull() ?: return null
    return Notificacion(id, tipoDominio, titulo, cuerpo, LocalDateTime.ofInstant(instante, zona), vacanteId)
}

/** Datos del perfil para PUT /v1/yo/perfil al terminar el registro. */
fun actualizacionDePerfil(
    registro: DatosRegistro?,
    area: AreaInteres?,
    niveles: NivelesUsuario,
    onboardingCompleto: Boolean,
): ActualizarPerfilDto = ActualizarPerfilDto(
    nombre = registro?.nombre?.takeIf { it.isNotBlank() },
    universidad = registro?.universidad?.takeIf { it.isNotBlank() },
    carrera = registro?.carrera?.takeIf { it.isNotBlank() },
    ciclo = registro?.ciclo?.takeIf { it > 0 },
    area = area?.name,
    onboardingCompleto = onboardingCompleto,
    // "No lo sé" no es un nivel que se pueda guardar: se omite.
    nivelesDeclarados = niveles.declarados.filterValues { it != NivelHabilidad.NO_LO_SE }.mapValues { it.value.name },
)

fun HorasLibres.aDto(): HorasLibresDto = HorasLibresDto(DayOfWeek.entries.associate { it.name to horas(it) })

/** Perfil de la API → niveles del dominio (los textos desconocidos se ignoran). */
fun PerfilPracticanteDto.niveles(): NivelesUsuario = NivelesUsuario(
    declarados = nivelesDeclarados.mapNotNull { (id, nivel) -> enumONull<NivelHabilidad>(nivel)?.let { id to it } }.toMap(),
    demostrados = nivelesDemostrados.mapNotNull { (id, nivel) -> enumONull<NivelHabilidad>(nivel)?.let { id to it } }.toMap(),
)

/** Horas del perfil de la API; si alguna no es válida (0 a 8 en pasos de 0.5), se descarta. */
fun PerfilPracticanteDto.horas(): HorasLibres = HorasLibres(
    horasLibres.mapNotNull { (dia, horas) ->
        val diaSemana = enumONull<DayOfWeek>(dia) ?: return@mapNotNull null
        if (HorasLibres.horasValidas(horas)) diaSemana to horas else null
    }.toMap(),
)
