package pe.edu.esan.chambaready.api.ia

/**
 * Preguntas escritas a mano para cuando NO hay clave de Gemini (o Gemini falla). Así la app y la
 * web funcionan igual en una computadora sin IA configurada. Cada habilidad del catálogo tiene 3
 * preguntas; una habilidad nueva usa preguntas genéricas.
 */
object ContenidoSimulado {

    private fun p(enunciado: String, correcta: String, vararg incorrectas: String, posicion: Int = 0): PreguntaIa {
        val opciones = incorrectas.toMutableList().apply { add(posicion.coerceIn(0, size), correcta) }
        return PreguntaIa(enunciado, opciones, opciones.indexOf(correcta))
    }

    private val banco: Map<String, List<PreguntaIa>> = mapOf(
        "sql" to listOf(
            p("¿Qué cláusula filtra filas ANTES de agrupar?", "WHERE", "HAVING", "ORDER BY", "LIMIT"),
            p("Para traer todos los clientes, tengan o no pedidos, usas…", "LEFT JOIN desde clientes", "INNER JOIN", "UNION", "CROSS JOIN", posicion = 1),
            p("¿Qué devuelve COUNT(*) en una tabla con 5 filas y 2 valores NULL en una columna?", "5", "3", "2", "NULL", posicion = 2),
        ),
        "excel" to listOf(
            p("¿Qué función busca un valor en la primera columna de un rango y devuelve otra columna?", "BUSCARV", "SUMAR.SI", "CONTAR", "CONCATENAR"),
            p("¿Qué herramienta resume miles de filas por categoría sin fórmulas?", "Tabla dinámica", "Formato condicional", "Validación de datos", "Inmovilizar paneles", posicion = 3),
            p("En =A1*\$B\$1, ¿qué pasa con \$B\$1 al copiar la fórmula hacia abajo?", "No cambia", "Pasa a B2", "Pasa a \$B\$2", "Da error", posicion = 1),
        ),
        "powerbi" to listOf(
            p("¿En qué lenguaje se escriben las medidas de Power BI?", "DAX", "SQL", "Python", "VBA", posicion = 2),
            p("¿Para qué sirve Power Query?", "Limpiar y transformar datos antes del modelo", "Publicar informes", "Dibujar gráficos", "Enviar correos"),
            p("Una relación de uno a varios conecta normalmente…", "Una tabla de dimensión con una de hechos", "Dos tablas de hechos", "Dos gráficos", "Dos páginas del informe", posicion = 1),
        ),
        "python" to listOf(
            p("¿Qué estructura guarda pares clave–valor?", "Diccionario", "Lista", "Tupla", "Conjunto", posicion = 3),
            p("¿Qué imprime print(len([1, 2, 3]))?", "3", "2", "[1, 2, 3]", "Error"),
            p("¿Qué librería se usa más para tablas de datos (DataFrames)?", "pandas", "requests", "flask", "pytest", posicion = 2),
        ),
        "estadistica" to listOf(
            p("Si hay valores muy extremos, ¿qué medida representa mejor el centro?", "La mediana", "La media", "El rango", "La varianza", posicion = 1),
            p("Una correlación de -0.9 indica…", "Relación inversa fuerte", "No hay relación", "Relación directa débil", "Causalidad", posicion = 0),
            p("¿Qué mide la desviación estándar?", "Qué tan dispersos están los datos", "El valor más repetido", "El valor central", "El total de datos", posicion = 2),
        ),
        "kotlin" to listOf(
            p("¿Qué palabra declara una variable que no se puede reasignar?", "val", "var", "const var", "let"),
            p("¿Qué hace el operador ?: (Elvis)?", "Da un valor si lo de la izquierda es null", "Compara dos valores", "Lanza un error", "Convierte a texto", posicion = 1),
            p("Una data class genera automáticamente…", "equals, hashCode, toString y copy", "Solo el constructor", "Una base de datos", "Una interfaz gráfica", posicion = 3),
        ),
        "git" to listOf(
            p("¿Qué comando guarda los cambios preparados en el historial local?", "git commit", "git push", "git clone", "git status"),
            p("¿Qué hace git pull?", "Trae cambios del remoto y los integra", "Sube tus commits", "Borra una rama", "Crea un repositorio", posicion = 2),
            p("Para trabajar una función nueva sin tocar main, creas…", "Una rama", "Un tag", "Un fork del lenguaje", "Un .gitignore", posicion = 1),
        ),
        "marketing" to listOf(
            p("¿Qué mide el CTR?", "Clics ÷ impresiones", "Ventas ÷ visitas", "Seguidores nuevos", "Costo por venta", posicion = 1),
            p("¿Qué es el público objetivo?", "El grupo de personas al que se dirige la campaña", "Todos los usuarios de internet", "La competencia", "El equipo de ventas"),
            p("Una prueba A/B compara…", "Dos versiones para ver cuál funciona mejor", "Dos empresas", "Dos presupuestos anuales", "Dos redes sociales", posicion = 3),
        ),
        "ads" to listOf(
            p("En Google Ads, ¿qué es una palabra clave negativa?", "Una búsqueda en la que NO quieres aparecer", "Una palabra con mala reseña", "Un anuncio pausado", "Una palabra muy cara", posicion = 2),
            p("CPC significa…", "Costo por clic", "Clientes por campaña", "Conversión por compra", "Campaña por categoría"),
            p("El nivel de calidad depende sobre todo de…", "La relevancia del anuncio y la página de destino", "El color del logo", "La hora de creación", "El número de campañas", posicion = 1),
        ),
        "finanzas" to listOf(
            p("Si el VAN de un proyecto es positivo…", "Genera más valor que su costo de capital", "Pierde dinero seguro", "No tiene riesgo", "Su TIR es cero", posicion = 1),
            p("¿Qué estado financiero muestra activos, pasivos y patrimonio?", "Balance general", "Estado de resultados", "Flujo de caja", "Presupuesto de ventas"),
            p("El margen bruto se calcula como…", "(Ventas − costo de ventas) ÷ ventas", "Utilidad neta ÷ activos", "Pasivos ÷ patrimonio", "Ventas ÷ clientes", posicion = 2),
        ),
        "comunicacion" to listOf(
            p("Para explicar un resultado a un gerente con poco tiempo, empiezas por…", "La conclusión y lo que se recomienda", "Todo el proceso paso a paso", "Las fórmulas usadas", "Los problemas del equipo", posicion = 0),
            p("Un correo profesional claro debe tener…", "Asunto específico y una acción esperada", "Mucho texto y adjuntos", "Solo emojis", "Copia a toda la empresa", posicion = 2),
            p("Escuchar activamente incluye…", "Repetir con tus palabras lo que entendiste", "Preparar tu respuesta mientras el otro habla", "Mirar el celular", "Interrumpir para ahorrar tiempo", posicion = 1),
        ),
        "equipo" to listOf(
            p("Si un compañero no entrega su parte a tiempo, lo primero es…", "Conversar con él para entender qué pasa", "Quejarte con el jefe", "Hacer su parte sin decir nada", "Ignorarlo", posicion = 0),
            p("Una buena reunión de equipo termina con…", "Responsables y fechas claras", "Más dudas", "Una nueva reunión sin tema", "Nada escrito", posicion = 3),
            p("Dar retroalimentación útil es…", "Específica, sobre hechos y con propuesta", "General y sobre la persona", "Solo cuando algo sale mal", "Anónima siempre", posicion = 1),
        ),
    )

    /** Preguntas genéricas para una habilidad que no está en el banco (por ejemplo, una nueva de una empresa). */
    private fun genericas(nombre: String): List<PreguntaIa> = listOf(
        p("¿Cuál es la mejor forma de demostrar tu nivel en $nombre?", "Con un proyecto o certificado verificable", "Diciendo que lo dominas", "Con años de estudio sin práctica", "No se puede demostrar"),
        p("Si te piden una tarea de $nombre que no sabes hacer, lo más profesional es…", "Decirlo y proponer cómo aprenderlo rápido", "Fingir que la sabes", "Rechazarla sin explicar", "Pedir que la haga otro siempre", posicion = 1),
        p("¿Qué te ayuda más a mejorar en $nombre?", "Practicar con casos reales y pedir retroalimentación", "Leer una sola vez", "Memorizar definiciones", "Esperar a que te capaciten", posicion = 2),
    )

    /** 3 preguntas para el mini-test (P15). */
    fun miniTest(habilidadId: String, nombre: String): List<PreguntaIa> = banco[habilidadId] ?: genericas(nombre)

    /** Una pregunta filtro por requisito (máximo 2, como pide la regla de las preguntas filtro). */
    fun preguntasFiltro(requisitos: List<Pair<String, String>>): List<PreguntaIa> =
        requisitos.take(2).map { (habilidadId, nombre) -> (banco[habilidadId] ?: genericas(nombre)).first() }

    /** Sin IA y con un PDF no se puede leer el texto: no se detecta nada y el usuario completa P03 a mano. */
    val cvVacio = CvLeido(emptyList(), emptyList())
}
