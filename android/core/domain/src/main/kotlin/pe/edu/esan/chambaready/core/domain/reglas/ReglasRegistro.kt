package pe.edu.esan.chambaready.core.domain.reglas

import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro

/** Campos del formulario de registro (C02). */
enum class CampoRegistro { NOMBRE, CORREO, UNIVERSIDAD, CARRERA, CICLO }

/** Resultado de revisar el CV elegido en P01. */
enum class ResultadoCv(val mensaje: String?) {
    VALIDO(null),
    FORMATO_INVALIDO("Sube tu CV en PDF o Word (.doc o .docx)."),
    MUY_PESADO("El archivo pesa más de 5 MB. Comprímelo o exporta una versión más liviana."),
    VACIO("El archivo está vacío."),
}

/** Validaciones de C02 y P01. Funciones puras, probadas en `ReglasRegistroTest`. */
object ReglasRegistro {

    const val CICLO_MINIMO = 1
    const val CICLO_MAXIMO = 14
    const val CV_BYTES_MAXIMOS = 5L * 1024 * 1024

    private val patronCorreo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    private val tiposCv = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )

    /** Errores por campo, con el mensaje que se muestra debajo de cada uno. Vacío si todo está bien. */
    fun validar(datos: DatosRegistro): Map<CampoRegistro, String> = buildMap {
        if (datos.nombre.trim().length < 2) put(CampoRegistro.NOMBRE, "Escribe tu nombre.")
        if (!patronCorreo.matches(datos.correo.trim())) put(CampoRegistro.CORREO, "Escribe un correo válido.")
        if (datos.universidad.isBlank()) put(CampoRegistro.UNIVERSIDAD, "Elige tu universidad.")
        if (datos.carrera.isBlank()) put(CampoRegistro.CARRERA, "Escribe tu carrera.")
        if (datos.ciclo !in CICLO_MINIMO..CICLO_MAXIMO) put(CampoRegistro.CICLO, "Elige un ciclo entre 1 y 14.")
    }

    fun validarCv(tipoMime: String?, bytes: Long): ResultadoCv = when {
        tipoMime == null || tipoMime !in tiposCv -> ResultadoCv.FORMATO_INVALIDO
        bytes <= 0 -> ResultadoCv.VACIO
        bytes > CV_BYTES_MAXIMOS -> ResultadoCv.MUY_PESADO
        else -> ResultadoCv.VALIDO
    }
}
