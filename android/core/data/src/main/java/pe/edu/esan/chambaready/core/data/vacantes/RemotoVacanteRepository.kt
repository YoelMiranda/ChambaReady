package pe.edu.esan.chambaready.core.data.vacantes

import kotlinx.serialization.json.Json
import pe.edu.esan.chambaready.core.data.remoto.ApiChambaReady
import pe.edu.esan.chambaready.core.data.remoto.ErrorRemoto
import pe.edu.esan.chambaready.core.data.remoto.aDominio
import pe.edu.esan.chambaready.core.data.remoto.llamarApi
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vacantes desde la API (F5). Las pantallas no notan la diferencia con [MockVacanteRepository]:
 * ambas cumplen [VacanteRepository]. Qué implementación se usa lo decide `RepositorioModule`.
 *
 * Guarda la última lista en memoria: así abrir el detalle justo después de la lista no vuelve a
 * pedirlo a la red. La caché se renueva cada vez que se llama a [listarVacantes].
 */
@Singleton
class RemotoVacanteRepository @Inject constructor(
    private val api: ApiChambaReady,
    private val json: Json,
) : VacanteRepository {

    @Volatile
    private var cache: Map<String, VacanteDetalle> = emptyMap()

    /** Si falla la red pero ya había una lista, devuelve esa; si no, lanza [ErrorRemoto]. */
    override suspend fun listarVacantes(): List<VacanteDetalle> = try {
        val vacantes = llamarApi(json) { api.vacantes() }.mapNotNull { it.aDominio() }
        cache = vacantes.associateBy { it.resumen.id }
        vacantes
    } catch (e: ErrorRemoto) {
        if (e.sinConexion && cache.isNotEmpty()) cache.values.toList() else throw e
    }

    /** null si la vacante no existe (404), ya cerró o no hay conexión. */
    override suspend fun obtenerVacante(id: String): VacanteDetalle? {
        cache[id]?.let { return it }
        return try {
            llamarApi(json) { api.vacante(id) }.aDominio()?.also { cache = cache + (id to it) }
        } catch (_: ErrorRemoto) {
            null
        }
    }
}
