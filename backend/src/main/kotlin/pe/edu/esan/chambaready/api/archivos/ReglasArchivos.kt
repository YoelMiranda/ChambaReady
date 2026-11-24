package pe.edu.esan.chambaready.api.archivos

import pe.edu.esan.chambaready.api.auth.Rol

/** Para qué es un archivo. Cada tipo tiene sus formatos, su tamaño máximo y quién lo puede subir. */
enum class TipoArchivo(val formatos: Set<String>, val bytesMaximos: Int, val rolQueSube: Rol, val mensajeFormato: String) {
    CV(setOf(Mime.PDF, Mime.DOCX), 5 * 1024 * 1024, Rol.PRACTICANTE, "Sube tu CV en PDF o Word (.docx)."),
    EVIDENCIA(setOf(Mime.PDF, Mime.PNG, Mime.JPEG), 5 * 1024 * 1024, Rol.PRACTICANTE, "La evidencia debe ser un PDF o una imagen (PNG o JPG)."),
    LOGO(setOf(Mime.PNG, Mime.JPEG), 1024 * 1024, Rol.EMPRESA, "El logo debe ser una imagen PNG o JPG."),
}

object Mime {
    const val PDF = "application/pdf"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
}

/**
 * Reglas de los archivos (F7). Puras y probadas en ReglasArchivosTest.
 * Seguridad: no se confía en el tipo que dice el cliente; se revisan los primeros bytes del archivo
 * ("firma"). Así nadie sube un ejecutable renombrado como .pdf.
 */
object ReglasArchivos {

    /** Mensaje de error, o null si el archivo se puede guardar. */
    fun validar(tipo: TipoArchivo, rol: Rol, mimeDeclarado: String, bytes: ByteArray): String? {
        if (rol != tipo.rolQueSube) return "Tu cuenta no puede subir este tipo de archivo."
        if (bytes.isEmpty()) return "El archivo está vacío."
        if (bytes.size > tipo.bytesMaximos) return "El archivo pesa más de ${tipo.bytesMaximos / (1024 * 1024)} MB."
        val mime = mimeDeclarado.substringBefore(";").trim().lowercase()
        if (mime !in tipo.formatos) return tipo.mensajeFormato
        if (!firmaCoincide(mime, bytes)) return "El contenido del archivo no corresponde a su formato."
        return null
    }

    /** Revisa la "firma" (primeros bytes) de cada formato. */
    fun firmaCoincide(mime: String, bytes: ByteArray): Boolean {
        fun empiezaCon(vararg firma: Int) = bytes.size >= firma.size && firma.indices.all { bytes[it].toInt() and 0xFF == firma[it] }
        return when (mime) {
            Mime.PDF -> empiezaCon(0x25, 0x50, 0x44, 0x46) // %PDF
            Mime.PNG -> empiezaCon(0x89, 0x50, 0x4E, 0x47)
            Mime.JPEG -> empiezaCon(0xFF, 0xD8, 0xFF)
            Mime.DOCX -> empiezaCon(0x50, 0x4B, 0x03, 0x04) // ZIP ("PK")
            else -> false
        }
    }

    /** Nombre seguro para mostrar: sin carpetas, sin caracteres de control y de 100 caracteres como máximo. */
    fun nombreSeguro(nombre: String?, tipo: TipoArchivo): String {
        val base = nombre.orEmpty().substringAfterLast('/').substringAfterLast('\\')
            .filter { !it.isISOControl() }.trim().take(100)
        return base.ifEmpty { tipo.name.lowercase() }
    }

    /**
     * ¿Puede [quien] ver el archivo?
     * - Su dueño, siempre.
     * - Un logo lo ve cualquier usuario registrado (aparece en las vacantes).
     * - El CV y las evidencias de un practicante los ve una empresa solo si él postuló a una de sus
     *   vacantes ([postuloAEstaEmpresa]). Una empresa nunca ve archivos de quien no le postuló.
     */
    fun puedeVer(tipo: TipoArchivo, duenoId: String, quienId: String, quienRol: Rol, postuloAEstaEmpresa: Boolean): Boolean = when {
        duenoId == quienId -> true
        tipo == TipoArchivo.LOGO -> true
        quienRol == Rol.EMPRESA -> postuloAEstaEmpresa
        else -> false
    }
}
