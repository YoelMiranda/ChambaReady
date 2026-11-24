package pe.edu.esan.chambaready.api.archivos

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import pe.edu.esan.chambaready.api.auth.Rol
import pe.edu.esan.chambaready.api.auth.UsuarioActual
import pe.edu.esan.chambaready.api.db.Archivos
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Empresas
import pe.edu.esan.chambaready.api.db.Postulaciones
import pe.edu.esan.chambaready.api.db.Vacantes
import pe.edu.esan.chambaready.api.dto.ArchivoDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import java.util.UUID

/** Un archivo leído de la base, listo para enviarlo. */
class ArchivoGuardado(val id: String, val duenoId: String, val tipo: TipoArchivo, val nombre: String, val mime: String, val bytes: ByteArray)

/**
 * F7 simulado: guarda los archivos en PostgreSQL en vez de Cloudflare R2. La API es la misma que
 * tendría con R2 (subir → recibir un id → referenciarlo), así que cambiar de almacenamiento solo
 * tocaría esta clase.
 */
class ServicioArchivos(private val bd: BaseDatos) {

    /** POST /v1/archivos?tipo=CV|EVIDENCIA|LOGO. El cuerpo es el archivo tal cual. */
    suspend fun subir(usuario: UsuarioActual, tipoTexto: String?, mime: String, nombre: String?, bytes: ByteArray): ArchivoDto {
        val tipo = TipoArchivo.entries.firstOrNull { it.name == tipoTexto }
            ?: throw ErrorApi.SolicitudInvalida("Indica el tipo de archivo: CV, EVIDENCIA o LOGO.")
        ReglasArchivos.validar(tipo, usuario.rol, mime, bytes)?.let { throw ErrorApi.SolicitudInvalida(it) }
        val limpio = mime.substringBefore(";").trim().lowercase()
        val nombreFinal = ReglasArchivos.nombreSeguro(nombre, tipo)
        val archivoId = UUID.randomUUID().toString()
        bd.tx {
            Archivos.insert {
                it[id] = archivoId
                it[usuarioId] = usuario.id
                it[Archivos.tipo] = tipo.name
                it[Archivos.nombre] = nombreFinal
                it[Archivos.mime] = limpio
                it[tamano] = bytes.size
                it[contenido] = bytes
            }
        }
        return ArchivoDto(archivoId, tipo.name, nombreFinal, limpio, bytes.size)
    }

    /** Lee un archivo si [quien] tiene permiso; si no, 404 (no se revela que existe). */
    suspend fun descargar(quien: UsuarioActual, archivoId: String): ArchivoGuardado = bd.tx {
        val archivo = leer(archivoId) ?: throw ErrorApi.NoEncontrado("Este archivo no existe.")
        val postulo = quien.rol == Rol.EMPRESA && postuloAEmpresa(archivo.duenoId, quien.id)
        if (!ReglasArchivos.puedeVer(archivo.tipo, archivo.duenoId, quien.id, quien.rol, postulo)) {
            throw ErrorApi.NoEncontrado("Este archivo no existe.")
        }
        archivo
    }

    /**
     * Archivo propio de un tipo dado, o error. Lo usan el CV, el logo y las evidencias al referenciarlo.
     * Se llama DENTRO de una transacción (bd.tx { ... }).
     */
    fun propio(usuarioId: String, archivoId: String, tipo: TipoArchivo): ArchivoGuardado =
        leer(archivoId)?.takeIf { it.duenoId == usuarioId && it.tipo == tipo }
            ?: throw ErrorApi.SolicitudInvalida("Ese archivo no existe o no es un ${tipo.name.lowercase()} tuyo.")

    private fun leer(archivoId: String): ArchivoGuardado? =
        Archivos.selectAll().where { Archivos.id eq archivoId }.singleOrNull()?.let {
            ArchivoGuardado(
                id = it[Archivos.id],
                duenoId = it[Archivos.usuarioId],
                tipo = TipoArchivo.valueOf(it[Archivos.tipo]),
                nombre = it[Archivos.nombre],
                mime = it[Archivos.mime],
                bytes = it[Archivos.contenido],
            )
        }

    /** ¿El practicante [practicanteId] postuló a alguna vacante de la empresa del usuario [empresaUsuarioId]? */
    private fun postuloAEmpresa(practicanteId: String, empresaUsuarioId: String): Boolean {
        val empresaId = Empresas.select(Empresas.id).where { Empresas.usuarioId eq empresaUsuarioId }.singleOrNull()?.get(Empresas.id)
            ?: return false
        return !Postulaciones.join(Vacantes, JoinType.INNER, Postulaciones.vacanteId, Vacantes.id).select(Postulaciones.id)
            .where { (Postulaciones.usuarioId eq practicanteId) and (Vacantes.empresaId eq empresaId) }
            .empty()
    }
}
