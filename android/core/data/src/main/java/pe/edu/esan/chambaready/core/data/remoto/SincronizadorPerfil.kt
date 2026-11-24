package pe.edu.esan.chambaready.core.data.remoto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sube el perfil al backend cada vez que cambia algo que el backend guarda (datos del registro,
 * área, niveles declarados, horas libres, registro terminado). Así ninguna pantalla tiene que
 * acordarse de sincronizar: todas siguen escribiendo en [EstadoUsuarioRepository] como antes.
 *
 * - Espera 1 s sin cambios antes de subir (mover el selector de horas no manda 10 pedidos).
 * - Si falla (sin internet), no insiste: se vuelve a intentar con el siguiente cambio o al abrir la
 *   app, porque al empezar siempre sube una vez lo que haya.
 * - En MODO DEMO no hace nada.
 */
@Singleton
class SincronizadorPerfil @Inject constructor(
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val cuenta: CuentaRepository,
    private val sesion: SesionRepository,
) {

    /** Solo los campos que viajan en PUT /v1/yo/perfil y PUT /v1/yo/horas-libres. */
    private fun EstadoUsuario.datosDelBackend() =
        listOf(registro, area, niveles.declarados, horasLibres, onboardingCompleto)

    fun iniciar(alcance: CoroutineScope) {
        if (!sesion.disponible) return
        alcance.launch {
            estadoRepositorio.estado
                // Antes de C02 la cuenta aún no existe en el backend (respondería 403).
                .filter { it.registro != null }
                .distinctUntilChangedBy { it.datosDelBackend() }
                .collectLatest { estado ->
                    delay(ESPERA_MS) // Si llega otro cambio antes, collectLatest cancela este.
                    if (!sesion.haySesion()) return@collectLatest
                    try {
                        cuenta.subirPerfil(estado)
                    } catch (_: ErrorRemoto) {
                        // Se reintenta con el próximo cambio o al abrir la app.
                    }
                }
        }
    }

    private companion object {
        const val ESPERA_MS = 1_000L
    }
}
