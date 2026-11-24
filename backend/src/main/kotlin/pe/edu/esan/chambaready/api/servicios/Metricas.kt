package pe.edu.esan.chambaready.api.servicios

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.jdbc.insert
import pe.edu.esan.chambaready.api.db.EventosMetricas
import java.util.UUID

/**
 * Métricas del proyecto (F9): cada evento importante deja una fila en eventos_metricas. Se llama
 * dentro de la transacción de la acción. Nunca guardes datos personales en [datos]: solo ids y tipos.
 * El resumen se ve en GET /v1/admin/metricas.
 */
object Metricas {
    fun registrar(usuarioId: String?, tipo: String, datos: Map<String, String> = emptyMap()) {
        EventosMetricas.insert {
            it[id] = UUID.randomUUID().toString()
            it[EventosMetricas.usuarioId] = usuarioId
            it[EventosMetricas.tipo] = tipo
            it[datosJson] = Json.encodeToString(JsonObject.serializer(), JsonObject(datos.mapValues { (_, v) -> JsonPrimitive(v) }))
        }
    }
}
