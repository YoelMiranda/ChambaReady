package pe.edu.esan.chambaready.api.notificaciones

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import pe.edu.esan.chambaready.api.db.BaseDatos
import pe.edu.esan.chambaready.api.db.Notificaciones
import pe.edu.esan.chambaready.api.dto.NotificacionDto
import pe.edu.esan.chambaready.api.dto.ResumenNotificacionesDto
import pe.edu.esan.chambaready.api.errores.ErrorApi
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Crea notificaciones. Se llama DENTRO de la transacción de la acción que la origina (postular,
 * agendar…): si la acción falla, la notificación tampoco queda.
 *
 * F9 reducido: las notificaciones se ven dentro de la app y de la web (campanita). Sin FCM no hay
 * avisos push en el celular.
 */
class Notificador(private val reloj: Clock) {
    fun notificar(usuarioId: String, tipo: String, titulo: String, cuerpo: String, vacanteId: String?) {
        Notificaciones.insert {
            it[id] = UUID.randomUUID().toString()
            it[Notificaciones.usuarioId] = usuarioId
            it[Notificaciones.tipo] = tipo
            it[Notificaciones.titulo] = titulo
            it[Notificaciones.cuerpo] = cuerpo
            it[Notificaciones.vacanteId] = vacanteId
            it[leida] = false
            it[creadaEn] = Instant.now(reloj)
        }
    }
}

/** GET /v1/notificaciones y marcar como leídas. Cada usuario ve SOLO las suyas. */
class ServicioNotificaciones(private val bd: BaseDatos) {

    suspend fun listar(usuarioId: String): List<NotificacionDto> = bd.tx {
        Notificaciones.selectAll().where { Notificaciones.usuarioId eq usuarioId }
            .orderBy(Notificaciones.creadaEn, SortOrder.DESC)
            .limit(MAXIMO)
            .map {
                NotificacionDto(
                    id = it[Notificaciones.id],
                    tipo = it[Notificaciones.tipo],
                    titulo = it[Notificaciones.titulo],
                    cuerpo = it[Notificaciones.cuerpo],
                    vacanteId = it[Notificaciones.vacanteId],
                    leida = it[Notificaciones.leida],
                    fecha = it[Notificaciones.creadaEn].toString(),
                )
            }
    }

    suspend fun resumen(usuarioId: String): ResumenNotificacionesDto = bd.tx {
        ResumenNotificacionesDto(
            Notificaciones.selectAll().where { (Notificaciones.usuarioId eq usuarioId) and (Notificaciones.leida eq false) }.count().toInt(),
        )
    }

    /** Marca una como leída. Si no es del usuario, 404 (no se revela que existe). */
    suspend fun marcarLeida(usuarioId: String, notificacionId: String) = bd.tx {
        val cambiadas = Notificaciones.update({ (Notificaciones.id eq notificacionId) and (Notificaciones.usuarioId eq usuarioId) }) { it[leida] = true }
        if (cambiadas == 0) throw ErrorApi.NoEncontrado("Esta notificación no existe.")
    }

    suspend fun marcarTodas(usuarioId: String) = bd.tx {
        Notificaciones.update({ (Notificaciones.usuarioId eq usuarioId) and (Notificaciones.leida eq false) }) { it[leida] = true }
    }

    private companion object {
        /** Las más nuevas; las viejas no se borran, solo no se listan. */
        const val MAXIMO = 50
    }
}
