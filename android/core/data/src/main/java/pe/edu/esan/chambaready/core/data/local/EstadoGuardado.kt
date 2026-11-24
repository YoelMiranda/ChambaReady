package pe.edu.esan.chambaready.core.data.local

import kotlinx.serialization.Serializable
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.ResultadoAutoevaluacion
import pe.edu.esan.chambaready.core.data.repositorio.ResultadoMiniTest
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.EstadoPostulacion
import pe.edu.esan.chambaready.core.domain.modelo.Evidencia
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * Formato en que se guarda el estado en DataStore (JSON). Solo usa tipos simples (texto, números,
 * listas y mapas) para que el dominio no dependa de kotlinx.serialization. Si agregas un campo,
 * dale un valor por defecto: así los teléfonos con datos viejos siguen funcionando.
 */

@Serializable
internal data class RegistroGuardado(
    val nombre: String,
    val correo: String,
    val universidad: String,
    val carrera: String,
    val ciclo: Int,
)

@Serializable
internal data class EvidenciaGuardada(
    val id: String,
    val titulo: String,
    val habilidadId: String,
    val tipo: String,
    val estado: String,
    val nivel: String,
)

@Serializable
internal data class PostulacionGuardada(
    val vacanteId: String,
    val puesto: String,
    val empresa: String,
    val estado: String,
    val fechaEnvio: String,
    val motivoDescarte: String? = null,
    val sellos: Map<String, String> = emptyMap(),
    // F6–F9 (con valor por defecto: los datos guardados por versiones anteriores siguen leyéndose).
    val id: String? = null,
    val preguntasPendientes: Boolean = false,
)

@Serializable
internal data class MiniTestGuardado(
    val vacanteId: String,
    val moduloId: String,
    val nombreHabilidad: String,
    val correctas: Int,
    val total: Int,
    val nivelAntes: String,
    val nivelDespues: String,
)

@Serializable
internal data class AutoevaluacionGuardada(
    val area: String,
    val correctas: Int,
    val total: Int,
    val topPorcentaje: Int,
    val topAnterior: Int? = null,
)

@Serializable
internal data class EstadoGuardado(
    val registro: RegistroGuardado? = null,
    val onboardingCompleto: Boolean = false,
    val cvNombre: String? = null,
    val horasPorDia: Map<String, Double> = emptyMap(),
    val area: String? = null,
    val premium: Boolean = false,
    val nivelesDeclarados: Map<String, String> = emptyMap(),
    val nivelesDemostrados: Map<String, String> = emptyMap(),
    val evidencias: List<EvidenciaGuardada> = emptyList(),
    val tareasCompletadas: Set<String> = emptySet(),
    val planesActivos: Set<String> = emptySet(),
    val metas: Set<String> = emptySet(),
    val notificacionesLeidas: Set<String> = emptySet(),
    val postulaciones: List<PostulacionGuardada> = emptyList(),
    val respuestasFiltro: Map<String, List<Boolean>> = emptyMap(),
    val probabilidadInicial: Map<String, Int> = emptyMap(),
    val ultimoMiniTest: MiniTestGuardado? = null,
    val autoevaluacion: AutoevaluacionGuardada? = null,
    val avisosActivos: Map<String, Boolean> = emptyMap(),
)

// ---------- Conversión ----------

/** Lee un enum por nombre; devuelve null si el valor guardado ya no existe en el código. */
private inline fun <reified T : Enum<T>> enumONulo(nombre: String?): T? =
    enumValues<T>().firstOrNull { it.name == nombre }

private fun nivel(nombre: String?): NivelHabilidad = enumONulo<NivelHabilidad>(nombre) ?: NivelHabilidad.NO_LO_HE_USADO

private fun Map<String, String>.aNiveles(): Map<String, NivelHabilidad> = mapValues { nivel(it.value) }

internal fun EstadoGuardado.aDominio(): EstadoUsuario = EstadoUsuario(
    registro = registro?.let { DatosRegistro(it.nombre, it.correo, it.universidad, it.carrera, it.ciclo) },
    onboardingCompleto = onboardingCompleto,
    cvNombre = cvNombre,
    horasLibres = HorasLibres(
        horasPorDia.mapNotNull { (dia, horas) ->
            val d = enumONulo<DayOfWeek>(dia)
            if (d != null && HorasLibres.horasValidas(horas)) d to horas else null
        }.toMap(),
    ),
    area = enumONulo<AreaInteres>(area),
    premium = premium,
    niveles = NivelesUsuario(nivelesDeclarados.aNiveles(), nivelesDemostrados.aNiveles()),
    evidencias = evidencias.map {
        Evidencia(
            id = it.id,
            titulo = it.titulo,
            habilidadId = it.habilidadId,
            tipo = enumONulo<TipoEvidencia>(it.tipo) ?: TipoEvidencia.LINK,
            estado = enumONulo<EstadoEvidencia>(it.estado) ?: EstadoEvidencia.EN_REVISION,
            nivel = nivel(it.nivel),
        )
    },
    tareasCompletadas = tareasCompletadas,
    planesActivos = planesActivos,
    metas = metas,
    notificacionesLeidas = notificacionesLeidas,
    postulaciones = postulaciones.map {
        Postulacion(
            vacanteId = it.vacanteId,
            puesto = it.puesto,
            empresa = it.empresa,
            estado = enumONulo<EstadoPostulacion>(it.estado) ?: EstadoPostulacion.ENVIADA,
            fechaEnvio = runCatching { LocalDate.parse(it.fechaEnvio) }.getOrDefault(LocalDate.now()),
            motivoDescarte = it.motivoDescarte,
            sellos = it.sellos.mapNotNull { (requisito, sello) ->
                enumONulo<TipoSello>(sello)?.let { s -> requisito to s }
            }.toMap(),
            id = it.id,
            preguntasPendientes = it.preguntasPendientes,
        )
    },
    respuestasFiltro = respuestasFiltro,
    probabilidadInicial = probabilidadInicial,
    ultimoMiniTest = ultimoMiniTest?.let {
        ResultadoMiniTest(it.vacanteId, it.moduloId, it.nombreHabilidad, it.correctas, it.total, nivel(it.nivelAntes), nivel(it.nivelDespues))
    },
    autoevaluacion = autoevaluacion?.let { a ->
        enumONulo<AreaInteres>(a.area)?.let { ResultadoAutoevaluacion(it, a.correctas, a.total, a.topPorcentaje, a.topAnterior) }
    },
    avisosActivos = TipoNotificacion.entries.associateWith { avisosActivos[it.name] ?: true },
)

internal fun EstadoUsuario.aGuardado(): EstadoGuardado = EstadoGuardado(
    registro = registro?.let { RegistroGuardado(it.nombre, it.correo, it.universidad, it.carrera, it.ciclo) },
    onboardingCompleto = onboardingCompleto,
    cvNombre = cvNombre,
    horasPorDia = horasLibres.porDia.mapKeys { it.key.name },
    area = area?.name,
    premium = premium,
    nivelesDeclarados = niveles.declarados.mapValues { it.value.name },
    nivelesDemostrados = niveles.demostrados.mapValues { it.value.name },
    evidencias = evidencias.map { EvidenciaGuardada(it.id, it.titulo, it.habilidadId, it.tipo.name, it.estado.name, it.nivel.name) },
    tareasCompletadas = tareasCompletadas,
    planesActivos = planesActivos,
    metas = metas,
    notificacionesLeidas = notificacionesLeidas,
    postulaciones = postulaciones.map {
        PostulacionGuardada(
            it.vacanteId, it.puesto, it.empresa, it.estado.name, it.fechaEnvio.toString(), it.motivoDescarte,
            it.sellos.mapValues { s -> s.value.name }, it.id, it.preguntasPendientes,
        )
    },
    respuestasFiltro = respuestasFiltro,
    probabilidadInicial = probabilidadInicial,
    ultimoMiniTest = ultimoMiniTest?.let {
        MiniTestGuardado(it.vacanteId, it.moduloId, it.nombreHabilidad, it.correctas, it.total, it.nivelAntes.name, it.nivelDespues.name)
    },
    autoevaluacion = autoevaluacion?.let { AutoevaluacionGuardada(it.area.name, it.correctas, it.total, it.topPorcentaje, it.topAnterior) },
    avisosActivos = avisosActivos.mapKeys { it.key.name },
)
