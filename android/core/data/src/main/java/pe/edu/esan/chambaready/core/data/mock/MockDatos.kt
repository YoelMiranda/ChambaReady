package pe.edu.esan.chambaready.core.data.mock

import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.ResultadoAutoevaluacion
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.CursoExterno
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.EstadoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.EstadoPostulacion
import pe.edu.esan.chambaready.core.domain.modelo.Evidencia
import pe.edu.esan.chambaready.core.domain.modelo.Habilidad
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad.AVANZADO
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad.BASICO
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad.INTERMEDIO
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.Notificacion
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.modelo.PreguntaOpcion
import pe.edu.esan.chambaready.core.domain.modelo.RequisitoVacante
import pe.edu.esan.chambaready.core.domain.modelo.TipoEvidencia
import pe.edu.esan.chambaready.core.domain.modelo.TipoHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoNotificacion
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito.DESEABLE
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito.FILTRO
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito.OBLIGATORIO
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Datos de prueba de F1–F3. Todo es ficticio: no uses datos de personas reales.
 * Las fechas se calculan desde "hoy" para que la cuenta regresiva muestre los tres colores.
 */
object MockDatos {

    // ---------- Habilidades ----------

    val habilidades: List<Habilidad> = listOf(
        Habilidad("sql", "SQL", TipoHabilidad.TECNICA),
        Habilidad("excel", "Excel", TipoHabilidad.TECNICA),
        Habilidad("powerbi", "Power BI", TipoHabilidad.TECNICA),
        Habilidad("python", "Python", TipoHabilidad.TECNICA),
        Habilidad("estadistica", "Estadística", TipoHabilidad.TECNICA),
        Habilidad("kotlin", "Kotlin", TipoHabilidad.TECNICA),
        Habilidad("git", "Git", TipoHabilidad.TECNICA),
        Habilidad("marketing", "Marketing digital", TipoHabilidad.TECNICA),
        Habilidad("ads", "Google Ads", TipoHabilidad.TECNICA),
        Habilidad("finanzas", "Finanzas", TipoHabilidad.TECNICA),
        Habilidad("comunicacion", "Comunicación", TipoHabilidad.BLANDA),
        Habilidad("equipo", "Trabajo en equipo", TipoHabilidad.BLANDA),
    )

    fun nombreHabilidad(id: String): String = habilidades.firstOrNull { it.id == id }?.nombre ?: id

    /** Las que la "IA" encuentra en el CV de prueba (P02). */
    val habilidadesDelCv: List<String> = listOf("sql", "excel", "python", "powerbi", "comunicacion", "equipo")

    val proyectosDelCv: List<String> = listOf(
        "Dashboard de ventas en Excel para el curso de Gestión Comercial",
        "Análisis de encuestas con Python (proyecto de Estadística)",
    )

    val universidades: List<String> = listOf(
        "Universidad ESAN", "Pontificia Universidad Católica del Perú", "Universidad de Lima",
        "Universidad del Pacífico", "Universidad Nacional de Ingeniería", "Universidad Nacional Mayor de San Marcos",
        "Universidad Peruana de Ciencias Aplicadas", "Universidad de Ingeniería y Tecnología",
        "Universidad San Ignacio de Loyola", "Universidad Peruana Cayetano Heredia",
    )

    // ---------- Cursos externos (regla 8: siempre externos, con link y horas) ----------

    private fun curso(id: String, titulo: String, proveedor: String, busqueda: String, horas: Double) =
        CursoExterno(id, titulo, proveedor, "https://www.coursera.org/search?query=$busqueda", horas)

    val cursos: Map<String, CursoExterno> = listOf(
        curso("sql", "SQL para análisis de datos", "Coursera", "sql", 6.0),
        curso("excel", "Excel intermedio y avanzado", "Coursera", "excel", 5.0),
        curso("powerbi", "Introducción a Power BI", "Coursera", "power%20bi", 6.0),
        curso("python", "Python para todos", "Coursera", "python", 8.0),
        curso("estadistica", "Estadística básica", "Coursera", "statistics", 6.0),
        curso("kotlin", "Kotlin para Android", "Coursera", "kotlin", 10.0),
        curso("git", "Git y GitHub", "Coursera", "git", 3.0),
        curso("marketing", "Fundamentos de marketing digital", "Coursera", "digital%20marketing", 6.0),
        curso("ads", "Google Ads desde cero", "Coursera", "google%20ads", 4.0),
        curso("finanzas", "Finanzas para no financieros", "Coursera", "finance", 6.0),
        curso("comunicacion", "Comunicación efectiva", "Coursera", "communication", 4.0),
        curso("equipo", "Trabajo en equipo", "Coursera", "teamwork", 3.0),
    ).associateBy { it.habilidadId }

    // ---------- Vacantes ----------

    private fun req(id: String, habilidad: String, nivel: NivelHabilidad, tipo: TipoRequisito) =
        RequisitoVacante(id, habilidad, nombreHabilidad(habilidad), nivel, tipo)

    fun vacantes(hoy: LocalDate): List<VacanteDetalle> = listOf(
        VacanteDetalle(
            resumen = VacanteResumen("v1", "Practicante de Analítica de Datos", "Banco Andino", Modalidad.HIBRIDA, "S/ 1,300", hoy.plusDays(12), 0),
            area = AreaInteres.DATA_ANALYTICS,
            descripcion = "Apoyo al equipo de Inteligencia Comercial en reportes y análisis de clientes.",
            queHaras = listOf(
                "Preparar reportes semanales de ventas con SQL y Excel.",
                "Actualizar tableros de seguimiento comercial.",
                "Presentar hallazgos al equipo cada dos semanas.",
            ),
            requisitos = listOf(
                req("v1r1", "sql", INTERMEDIO, FILTRO),
                req("v1r2", "excel", AVANZADO, OBLIGATORIO),
                req("v1r3", "powerbi", BASICO, DESEABLE),
                req("v1r4", "comunicacion", INTERMEDIO, DESEABLE),
            ),
            evaluacionActiva = true,
            empresaConEvaluaciones = true,
        ),
        VacanteDetalle(
            resumen = VacanteResumen("v2", "Practicante de Desarrollo Android", "Tech Lima Labs", Modalidad.REMOTA, "S/ 1,500", hoy.plusDays(6), 0),
            area = AreaInteres.DESARROLLO_SOFTWARE,
            descripcion = "Desarrollo de nuevas pantallas para la app de clientes.",
            queHaras = listOf(
                "Construir pantallas con Jetpack Compose.",
                "Corregir errores reportados por los usuarios.",
                "Participar en revisiones de código con el equipo.",
            ),
            requisitos = listOf(
                req("v2r1", "kotlin", INTERMEDIO, FILTRO),
                req("v2r2", "git", BASICO, OBLIGATORIO),
                req("v2r3", "equipo", INTERMEDIO, DESEABLE),
            ),
            evaluacionActiva = true,
            // La empresa ya usó sus evaluaciones: no hay preguntas filtro (regla 11).
            empresaConEvaluaciones = false,
        ),
        VacanteDetalle(
            resumen = VacanteResumen("v3", "Practicante de Marketing Digital", "Retail Sur", Modalidad.PRESENCIAL, "S/ 1,025", hoy.plusDays(20), 0),
            area = AreaInteres.MARKETING_DIGITAL,
            descripcion = "Apoyo en campañas digitales para la temporada de verano.",
            queHaras = listOf(
                "Programar publicaciones en redes sociales.",
                "Medir resultados de campañas pagadas.",
            ),
            requisitos = listOf(
                req("v3r1", "marketing", INTERMEDIO, OBLIGATORIO),
                req("v3r2", "ads", BASICO, DESEABLE),
                req("v3r3", "comunicacion", INTERMEDIO, OBLIGATORIO),
            ),
            evaluacionActiva = false,
            empresaConEvaluaciones = true,
        ),
        VacanteDetalle(
            resumen = VacanteResumen("v4", "Practicante de Finanzas Corporativas", "Grupo Pacífico", Modalidad.HIBRIDA, "S/ 1,400", hoy.plusDays(25), 0),
            area = AreaInteres.FINANZAS,
            descripcion = "Soporte al área de Planeamiento Financiero.",
            queHaras = listOf(
                "Armar modelos de presupuesto en Excel.",
                "Comparar resultados reales contra el presupuesto.",
            ),
            requisitos = listOf(
                req("v4r1", "excel", INTERMEDIO, OBLIGATORIO),
                req("v4r2", "finanzas", BASICO, OBLIGATORIO),
                req("v4r3", "estadistica", BASICO, DESEABLE),
            ),
            evaluacionActiva = true,
            empresaConEvaluaciones = true,
        ),
        VacanteDetalle(
            resumen = VacanteResumen("v5", "Practicante de Datos Junior", "Logística Norte", Modalidad.PRESENCIAL, null, hoy.plusDays(2), 0),
            area = AreaInteres.DATA_ANALYTICS,
            descripcion = "Control de indicadores de despacho en el centro de distribución.",
            queHaras = listOf(
                "Consolidar indicadores diarios en Excel.",
                "Hacer consultas simples a la base de datos.",
            ),
            requisitos = listOf(
                req("v5r1", "sql", BASICO, FILTRO),
                req("v5r2", "excel", INTERMEDIO, OBLIGATORIO),
                req("v5r3", "python", BASICO, DESEABLE),
            ),
            evaluacionActiva = true,
            empresaConEvaluaciones = true,
        ),
    )

    // ---------- Preguntas de opción múltiple ----------

    private val preguntasPorHabilidad: Map<String, List<PreguntaOpcion>> = mapOf(
        "sql" to listOf(
            PreguntaOpcion("sql1", "¿Qué cláusula filtra filas antes de agrupar?", listOf("WHERE", "HAVING", "ORDER BY", "No lo sé"), 0, habilidadId = "sql"),
            PreguntaOpcion(
                "sql2", "¿Qué devuelve esta consulta?",
                listOf("El total de ventas por región", "Una fila por venta", "Solo la región con más ventas", "No lo sé"), 0,
                codigo = "SELECT region, SUM(monto)\nFROM ventas\nGROUP BY region;", habilidadId = "sql",
            ),
            PreguntaOpcion("sql3", "Para unir clientes con sus pedidos, aunque no tengan pedidos, usas…", listOf("INNER JOIN", "LEFT JOIN desde clientes", "UNION", "No lo sé"), 1, habilidadId = "sql"),
        ),
        "excel" to listOf(
            PreguntaOpcion("xl1", "¿Qué función busca un valor en una tabla y devuelve otro de la misma fila?", listOf("BUSCARX", "CONTAR", "REDONDEAR", "No lo sé"), 0, habilidadId = "excel"),
            PreguntaOpcion("xl2", "Para resumir ventas por mes y vendedor, lo más rápido es…", listOf("Una tabla dinámica", "Copiar y pegar", "Un gráfico circular", "No lo sé"), 0, habilidadId = "excel"),
            PreguntaOpcion("xl3", "¿Qué hace el signo $ en =A$1?", listOf("Fija la fila al copiar la fórmula", "Convierte a soles", "Fija la columna", "No lo sé"), 0, habilidadId = "excel"),
        ),
        "powerbi" to listOf(
            PreguntaOpcion("bi1", "En Power BI, ¿dónde se limpian y transforman los datos?", listOf("Power Query", "La vista de informe", "Las medidas DAX", "No lo sé"), 0, habilidadId = "powerbi"),
            PreguntaOpcion("bi2", "Una medida en DAX sirve para…", listOf("Calcular valores que cambian según los filtros", "Cambiar colores", "Importar archivos", "No lo sé"), 0, habilidadId = "powerbi"),
            PreguntaOpcion("bi3", "Para relacionar la tabla Ventas con Productos necesitas…", listOf("Una columna en común", "Que tengan el mismo nombre", "Un gráfico", "No lo sé"), 0, habilidadId = "powerbi"),
        ),
        "python" to listOf(
            PreguntaOpcion("py1", "¿Qué imprime este código?", listOf("6", "123", "Error", "No lo sé"), 0, codigo = "print(sum([1, 2, 3]))", habilidadId = "python"),
            PreguntaOpcion("py2", "¿Qué librería se usa para tablas de datos (DataFrames)?", listOf("pandas", "requests", "flask", "No lo sé"), 0, habilidadId = "python"),
            PreguntaOpcion("py3", "Para recorrer una lista usas…", listOf("for elemento in lista:", "while lista:", "if lista:", "No lo sé"), 0, habilidadId = "python"),
        ),
        "kotlin" to listOf(
            PreguntaOpcion("kt1", "¿Qué palabra declara una variable que no cambia?", listOf("val", "var", "const fun", "No lo sé"), 0, habilidadId = "kotlin"),
            PreguntaOpcion("kt2", "¿Qué devuelve nombre?.length si nombre es null?", listOf("null", "0", "Un error", "No lo sé"), 0, habilidadId = "kotlin"),
            PreguntaOpcion("kt3", "En Compose, para recordar un estado entre recomposiciones usas…", listOf("remember { mutableStateOf(...) }", "una variable global", "lateinit", "No lo sé"), 0, habilidadId = "kotlin"),
        ),
        "git" to listOf(
            PreguntaOpcion("git1", "¿Qué comando guarda tus cambios en el historial local?", listOf("git commit", "git push", "git clone", "No lo sé"), 0, habilidadId = "git"),
            PreguntaOpcion("git2", "Para trabajar una función sin tocar main, creas…", listOf("Una rama", "Un tag", "Otro repositorio", "No lo sé"), 0, habilidadId = "git"),
            PreguntaOpcion("git3", "¿Qué hace git pull?", listOf("Trae y une los cambios del remoto", "Borra la rama", "Sube tus cambios", "No lo sé"), 0, habilidadId = "git"),
        ),
        "marketing" to listOf(
            PreguntaOpcion("mk1", "¿Qué mide el CTR de un anuncio?", listOf("Clics ÷ impresiones", "Ventas ÷ clics", "Seguidores nuevos", "No lo sé"), 0, habilidadId = "marketing"),
            PreguntaOpcion("mk2", "Un buyer persona es…", listOf("Un perfil del cliente ideal", "Un comprador real", "Un tipo de anuncio", "No lo sé"), 0, habilidadId = "marketing"),
            PreguntaOpcion("mk3", "Para saber qué campaña trajo una venta usas…", listOf("Parámetros UTM", "Más hashtags", "Un logo nuevo", "No lo sé"), 0, habilidadId = "marketing"),
        ),
        "finanzas" to listOf(
            PreguntaOpcion("fi1", "El flujo de caja muestra…", listOf("Entradas y salidas de dinero", "Solo las ventas", "El valor de la marca", "No lo sé"), 0, habilidadId = "finanzas"),
            PreguntaOpcion("fi2", "Si el VAN de un proyecto es positivo…", listOf("Crea valor a la tasa usada", "Pierde dinero", "No se puede saber nada", "No lo sé"), 0, habilidadId = "finanzas"),
            PreguntaOpcion("fi3", "El margen bruto se calcula como…", listOf("(Ventas − costo de ventas) ÷ ventas", "Ventas ÷ gastos", "Utilidad neta × 2", "No lo sé"), 0, habilidadId = "finanzas"),
        ),
    )

    /** Preguntas genéricas para habilidades sin banco propio (en F6 las genera Gemini). */
    private fun preguntasGenericas(habilidadId: String): List<PreguntaOpcion> {
        val nombre = nombreHabilidad(habilidadId)
        return listOf(
            PreguntaOpcion("${habilidadId}g1", "Al empezar una tarea nueva de $nombre, lo primero es…", listOf("Entender el objetivo y el resultado esperado", "Empezar sin preguntar", "Esperar a que alguien lo haga", "No lo sé"), 0, habilidadId = habilidadId),
            PreguntaOpcion("${habilidadId}g2", "Si te equivocas en un entregable de $nombre…", listOf("Lo avisas, lo corriges y explicas cómo evitarlo", "Lo ocultas", "Culpas a otro", "No lo sé"), 0, habilidadId = habilidadId),
            PreguntaOpcion("${habilidadId}g3", "Para mejorar en $nombre, lo más efectivo es…", listOf("Practicar con casos reales y pedir feedback", "Solo leer teoría", "Nada, ya es suficiente", "No lo sé"), 0, habilidadId = habilidadId),
        )
    }

    fun miniTest(moduloId: String): List<PreguntaOpcion> = preguntasPorHabilidad[moduloId] ?: preguntasGenericas(moduloId)

    private val habilidadesPorArea: Map<AreaInteres, List<String>> = mapOf(
        AreaInteres.DATA_ANALYTICS to listOf("sql", "excel", "python"),
        AreaInteres.DESARROLLO_SOFTWARE to listOf("kotlin", "git", "python"),
        AreaInteres.MARKETING_DIGITAL to listOf("marketing", "ads", "comunicacion"),
        AreaInteres.FINANZAS to listOf("finanzas", "excel", "estadistica"),
        AreaInteres.OPERACIONES_LOGISTICA to listOf("excel", "estadistica", "equipo"),
        AreaInteres.DISENO_UX_UI to listOf("comunicacion", "equipo", "marketing"),
        AreaInteres.RECURSOS_HUMANOS to listOf("comunicacion", "equipo", "excel"),
        AreaInteres.VENTAS_COMERCIAL to listOf("comunicacion", "marketing", "excel"),
    )

    /** P05: una pregunta de cada habilidad principal del área. */
    fun preguntasAutoevaluacion(area: AreaInteres): List<PreguntaOpcion> =
        habilidadesPorArea.getValue(area).map { miniTest(it).first() }

    /** P24: dos preguntas ligadas a los requisitos más importantes de la vacante. */
    fun preguntasFiltro(vacante: VacanteDetalle): List<PreguntaOpcion> =
        vacante.requisitos.sortedBy { it.tipo.ordinal }.take(2).map { requisito ->
            miniTest(requisito.habilidadId)[1].copy(id = "f-${requisito.id}", requisitoId = requisito.id)
        }

    // ---------- Notificaciones ----------

    fun notificaciones(ahora: LocalDateTime): List<Notificacion> = listOf(
        Notificacion("n1", TipoNotificacion.CIERRE_CERCANO, "Cierra pronto", "«Practicante de Datos Junior» cierra en 2 días.", ahora.minusHours(1), "v5"),
        Notificacion("n2", TipoNotificacion.PLAN_SEMANAL, "Tu plan semanal está listo", "Esta semana toca SQL. Revisa tus tareas.", ahora.minusHours(3), "v1"),
        Notificacion("n3", TipoNotificacion.ENTREVISTA, "Entrevista agendada", "Grupo Pacífico te citó para una entrevista por videollamada.", ahora.minusHours(5), "v4"),
        Notificacion("n4", TipoNotificacion.CAMBIO_ESTADO, "Tu postulación cambió de estado", "Retail Sur revisó tu postulación.", ahora.minusDays(2), "v3"),
        Notificacion("n5", TipoNotificacion.NUEVA_VACANTE, "Nueva vacante para ti", "«Practicante de Analítica de Datos» calza con tu perfil.", ahora.minusDays(3), "v1"),
        Notificacion("n6", TipoNotificacion.RANKING, "Subiste en tu ranking", "Ahora estás en el Top 18 % de Data y Analytics.", ahora.minusDays(5), null),
    )

    // ---------- Estado de demostración ----------

    /** Perfil ficticio para explorar la app sin pasar por el registro (botón de prueba en C01). */
    fun estadoDemo(hoy: LocalDate): EstadoUsuario = EstadoUsuario(
        registro = DatosRegistro("Ana Demo", "ana.demo@ejemplo.com", "Universidad ESAN", "Ingeniería de Información", 7),
        onboardingCompleto = true,
        cvNombre = "CV_Ana_Demo.pdf",
        horasLibres = HorasLibres(
            mapOf(
                DayOfWeek.MONDAY to 1.5, DayOfWeek.TUESDAY to 1.5, DayOfWeek.WEDNESDAY to 1.5,
                DayOfWeek.THURSDAY to 1.5, DayOfWeek.FRIDAY to 1.0, DayOfWeek.SATURDAY to 4.0, DayOfWeek.SUNDAY to 2.0,
            ),
        ),
        area = AreaInteres.DATA_ANALYTICS,
        niveles = NivelesUsuario(
            declarados = mapOf(
                "sql" to BASICO, "excel" to AVANZADO, "python" to BASICO, "powerbi" to NivelHabilidad.NO_LO_HE_USADO,
                "comunicacion" to INTERMEDIO, "equipo" to AVANZADO,
            ),
        ),
        evidencias = listOf(
            Evidencia("e1", "Certificado de Excel avanzado", "excel", TipoEvidencia.CERTIFICADO, EstadoEvidencia.VALIDADA, AVANZADO),
            Evidencia("e2", "Repositorio de análisis de encuestas", "python", TipoEvidencia.LINK, EstadoEvidencia.EN_REVISION, BASICO),
        ),
        postulaciones = listOf(
            Postulacion("v4", "Practicante de Finanzas Corporativas", "Grupo Pacífico", EstadoPostulacion.ENTREVISTA, hoy.minusDays(6)),
            Postulacion(
                "v3", "Practicante de Marketing Digital", "Retail Sur", EstadoPostulacion.NO_SELECCIONADO, hoy.minusDays(15),
                motivoDescarte = "Buscábamos nivel intermedio en marketing digital, con experiencia en campañas pagadas.",
            ),
        ),
        autoevaluacion = ResultadoAutoevaluacion(AreaInteres.DATA_ANALYTICS, correctas = 2, total = 3, topPorcentaje = 18, topAnterior = 21),
    )
}
