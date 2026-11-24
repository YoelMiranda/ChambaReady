package pe.edu.esan.chambaready.core.domain.vacantes

import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import kotlin.coroutines.cancellation.CancellationException

/**
 * Vacantes que ve el practicante. En modo demo la implementa `core/data/vacantes/MockVacanteRepository`;
 * con backend (F5), `RemotoVacanteRepository`. La probabilidad del usuario se calcula con
 * `MotorDiagnostico` a partir de sus niveles.
 */
interface VacanteRepository {

    /** Vacantes abiertas. Puede lanzar un error si no hay conexión: la pantalla P08 lo muestra. */
    suspend fun listarVacantes(): List<VacanteDetalle>

    /** null si no existe o no se pudo cargar (la pantalla muestra "no encontrada"). No lanza errores. */
    suspend fun obtenerVacante(id: String): VacanteDetalle?

    /**
     * Como [listarVacantes], pero si falla devuelve una lista vacía. Para pantallas donde la lista es
     * secundaria (Inicio, Pendientes, la vacante parecida de P10) y un error no debe cerrar la app.
     */
    suspend fun listarVacantesOVacio(): List<VacanteDetalle> =
        try {
            listarVacantes()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
}
