package pe.edu.esan.chambaready.core.data.remoto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Respuestas de P24 que todavía no se enviaron (F6). Viven solo en memoria entre P24 y "Enviar
 * postulación" en P17: con backend, quien corrige es el servidor, así que el teléfono guarda la
 * opción elegida (no si acertó). Si se cierra la app, se vuelven a responder.
 */
@Singleton
class RespuestasFiltroSesion @Inject constructor() {
    private val porVacante = mutableMapOf<String, Map<String, Int?>>()

    @Synchronized fun guardar(vacanteId: String, respuestas: Map<String, Int?>) {
        porVacante[vacanteId] = respuestas
    }

    @Synchronized fun obtener(vacanteId: String): Map<String, Int?>? = porVacante[vacanteId]

    @Synchronized fun borrar(vacanteId: String) {
        porVacante.remove(vacanteId)
    }
}
