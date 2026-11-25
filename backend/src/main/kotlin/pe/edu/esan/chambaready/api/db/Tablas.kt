package pe.edu.esan.chambaready.api.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.time
import org.jetbrains.exposed.v1.javatime.timestamp

/*
 * Tablas para Exposed. Deben coincidir con las migraciones de `resources/db/migration`: Flyway crea
 * las tablas y Exposed solo las consulta (nunca uses SchemaUtils.create). Solo están las tablas que
 * usa algún endpoint.
 */

object Usuarios : Table("usuarios") {
    val id = text("id")
    val firebaseUid = text("firebase_uid")
    val rol = text("rol")
    val nombre = text("nombre")
    val correo = text("correo")
    override val primaryKey = PrimaryKey(id)
}

object AreasInteres : Table("areas_interes") {
    val codigo = text("codigo")
    val nombre = text("nombre")
    override val primaryKey = PrimaryKey(codigo)
}

object Habilidades : Table("habilidades") {
    val id = text("id")
    val nombre = text("nombre")
    val tipo = text("tipo")
    override val primaryKey = PrimaryKey(id)
}

object ConfiguracionTabla : Table("configuracion") {
    val clave = text("clave")
    val valor = text("valor")
    override val primaryKey = PrimaryKey(clave)
}

object PerfilesPracticante : Table("perfiles_practicante") {
    val usuarioId = text("usuario_id")
    val universidad = text("universidad")
    val carrera = text("carrera")
    val ciclo = integer("ciclo").nullable()
    val areaInteres = text("area_interes").nullable()
    val onboardingCompleto = bool("onboarding_completo")
    val cvArchivoId = text("cv_archivo_id").nullable()
    override val primaryKey = PrimaryKey(usuarioId)
}

object HorasLibresTabla : Table("horas_libres") {
    val usuarioId = text("usuario_id")
    val diaSemana = short("dia_semana")
    val horas = decimal("horas", 3, 1)
    override val primaryKey = PrimaryKey(usuarioId, diaSemana)
}

object HabilidadesUsuario : Table("habilidades_usuario") {
    val usuarioId = text("usuario_id")
    val habilidadId = text("habilidad_id")
    val nivelDeclarado = text("nivel_declarado")
    val nivelDemostrado = text("nivel_demostrado")
    val fuenteDemostrado = text("fuente_demostrado").nullable()
    override val primaryKey = PrimaryKey(usuarioId, habilidadId)
}

object Empresas : Table("empresas") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val razonSocial = text("razon_social")
    val ruc = text("ruc")
    val sector = text("sector")
    val web = text("web")
    val tamano = text("tamano")
    val contactoNombre = text("contacto_nombre")
    val contactoCorreo = text("contacto_correo")
    val contactoTelefono = text("contacto_telefono")
    val descripcion = text("descripcion")
    val verificacion = text("verificacion")
    val logoArchivoId = text("logo_archivo_id").nullable()
    override val primaryKey = PrimaryKey(id)
}

object SaldoEvaluacionesTabla : Table("saldo_evaluaciones") {
    val empresaId = text("empresa_id")
    val gratisRestantes = integer("gratis_restantes")
    val planActivo = bool("plan_activo")
    override val primaryKey = PrimaryKey(empresaId)
}

object Vacantes : Table("vacantes") {
    val id = text("id")
    val empresaId = text("empresa_id")
    val puesto = text("puesto")
    val areaInteres = text("area_interes")
    val modalidad = text("modalidad")
    val sueldo = text("sueldo").nullable()
    val fechaCierre = date("fecha_cierre")
    val descripcion = text("descripcion")
    val queHara = text("que_hara")
    val evaluacionActiva = bool("evaluacion_activa")
    val creadaEn = timestamp("creada_en")
    override val primaryKey = PrimaryKey(id)
}

object RequisitosVacante : Table("requisitos_vacante") {
    val id = text("id")
    val vacanteId = text("vacante_id")
    val habilidadId = text("habilidad_id")
    val nivel = text("nivel")
    val tipo = text("tipo")
    val orden = integer("orden")
    override val primaryKey = PrimaryKey(id)
}

object PreguntasFiltro : Table("preguntas_filtro") {
    val id = text("id")
    val vacanteId = text("vacante_id")
    val requisitoId = text("requisito_id")
    val enunciado = text("enunciado")
    val opciones = text("opciones")
    val indiceCorrecto = integer("indice_correcto")
    val orden = integer("orden")
    override val primaryKey = PrimaryKey(id)
}

object CursosExternos : Table("cursos_externos") {
    val habilidadId = text("habilidad_id")
    val titulo = text("titulo")
    val proveedor = text("proveedor")
    val url = text("url")
    val horasPorNivel = decimal("horas_por_nivel", 5, 1)
    override val primaryKey = PrimaryKey(habilidadId)
}

object Postulaciones : Table("postulaciones") {
    val id = text("id")
    val vacanteId = text("vacante_id")
    val usuarioId = text("usuario_id")
    val estado = text("estado")
    val motivoDescarte = text("motivo_descarte").nullable()
    val evaluada = bool("evaluada")
    val preguntasCorrectas = integer("preguntas_correctas").nullable()
    val preguntasSolicitadas = bool("preguntas_solicitadas")
    val creadaEn = timestamp("creada_en")
    override val primaryKey = PrimaryKey(id)
}

object SellosPostulacion : Table("sellos_postulacion") {
    val postulacionId = text("postulacion_id")
    val requisitoId = text("requisito_id")
    val sello = text("sello")
    override val primaryKey = PrimaryKey(postulacionId, requisitoId)
}

object RespuestasFiltro : Table("respuestas_filtro") {
    val id = text("id")
    val postulacionId = text("postulacion_id")
    val preguntaId = text("pregunta_id")
    val indiceElegido = integer("indice_elegido").nullable()
    val correcta = bool("correcta")
    override val primaryKey = PrimaryKey(id)
}

object MovimientosEvaluacion : Table("movimientos_evaluacion") {
    val id = text("id")
    val empresaId = text("empresa_id")
    val postulacionId = text("postulacion_id")
    val fuente = text("fuente")
    override val primaryKey = PrimaryKey(id)
}

object Entrevistas : Table("entrevistas") {
    val id = text("id")
    val postulacionId = text("postulacion_id")
    val empresaId = text("empresa_id")
    val fecha = date("fecha")
    val hora = time("hora")
    val duracionMin = integer("duracion_min")
    val modalidad = text("modalidad")
    val mensaje = text("mensaje")
    override val primaryKey = PrimaryKey(id)
}

object Notificaciones : Table("notificaciones") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val tipo = text("tipo")
    val titulo = text("titulo")
    val cuerpo = text("cuerpo")
    val vacanteId = text("vacante_id").nullable()
    val leida = bool("leida")
    val creadaEn = timestamp("creada_en")
    override val primaryKey = PrimaryKey(id)
}

object UsoIa : Table("uso_ia") {
    val id = text("id")
    val usuarioId = text("usuario_id").nullable()
    val empresaId = text("empresa_id").nullable()
    val operacion = text("operacion")
    val modelo = text("modelo")
    val tokensEntrada = integer("tokens_entrada")
    val tokensSalida = integer("tokens_salida")
    val costoUsd = decimal("costo_usd", 12, 6)
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object SuscripcionesEstudiante : Table("suscripciones_estudiante") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val estado = text("estado")
    val precioCentimos = integer("precio_centimos")
    val venceEn = timestamp("vence_en").nullable()
    override val primaryKey = PrimaryKey(id)
}

object SuscripcionesEmpresa : Table("suscripciones_empresa") {
    val id = text("id")
    val empresaId = text("empresa_id")
    val estado = text("estado")
    val precioCentimos = integer("precio_centimos")
    val venceEn = timestamp("vence_en").nullable()
    override val primaryKey = PrimaryKey(id)
}

// ---------- F6 a F9 ----------

object Archivos : Table("archivos") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val tipo = text("tipo")
    val nombre = text("nombre")
    val mime = text("mime")
    val tamano = integer("tamano")
    val contenido = binary("contenido")
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object Evidencias : Table("evidencias") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val habilidadId = text("habilidad_id")
    val titulo = text("titulo")
    val tipo = text("tipo")
    val estado = text("estado")
    val nivel = text("nivel")
    val url = text("url").nullable()
    val archivoId = text("archivo_id").nullable()
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object MiniTests : Table("mini_tests") {
    val id = text("id")
    val habilidadId = text("habilidad_id")
    val nivel = text("nivel")
    val preguntasJson = text("preguntas_json")
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object IntentosTest : Table("intentos_test") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val habilidadId = text("habilidad_id")
    val vacanteId = text("vacante_id").nullable()
    val correctas = integer("correctas")
    val total = integer("total")
    val aprobado = bool("aprobado")
    val nivelAntes = text("nivel_antes")
    val nivelDespues = text("nivel_despues")
    val miniTestId = text("mini_test_id").nullable()
    override val primaryKey = PrimaryKey(id)
}

object EvaluacionesHabilidades : Table("evaluaciones_habilidades") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val habilidades = text("habilidades")
    val creadaEn = timestamp("creada_en")
    override val primaryKey = PrimaryKey(id)
}

object SnapshotsRanking : Table("snapshots_ranking") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val areaInteres = text("area_interes")
    val topPorcentaje = integer("top_porcentaje")
    val puntaje = decimal("puntaje", 4, 3)
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object PagosSimulados : Table("pagos_simulados") {
    val id = text("id")
    val usuarioId = text("usuario_id")
    val concepto = text("concepto")
    val montoCentimos = integer("monto_centimos")
    val venceEn = timestamp("vence_en")
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}

object EventosMetricas : Table("eventos_metricas") {
    val id = text("id")
    val usuarioId = text("usuario_id").nullable()
    val tipo = text("tipo")
    val datosJson = text("datos_json")
    val creadoEn = timestamp("creado_en")
    override val primaryKey = PrimaryKey(id)
}
