package pe.edu.esan.chambaready.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda todo el [EstadoUsuario] como un JSON en una sola clave de DataStore. `edit` es atómico:
 * dos cambios seguidos (por ejemplo, marcar dos tareas rápido) no se pisan.
 */
@Singleton
class DataStoreEstadoUsuarioRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : EstadoUsuarioRepository {

    private val clave = stringPreferencesKey("estado_usuario_v1")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val estado: Flow<EstadoUsuario> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferencias -> leer(preferencias[clave]) }

    override suspend fun actualizar(cambio: (EstadoUsuario) -> EstadoUsuario) {
        dataStore.edit { preferencias ->
            val nuevo = cambio(leer(preferencias[clave]))
            preferencias[clave] = json.encodeToString(EstadoGuardado.serializer(), nuevo.aGuardado())
        }
    }

    override suspend fun borrarTodo() {
        dataStore.edit { it.clear() }
    }

    /** Si el JSON está dañado o es de una versión incompatible, se empieza con un estado vacío. */
    private fun leer(texto: String?): EstadoUsuario =
        texto
            ?.let { runCatching { json.decodeFromString(EstadoGuardado.serializer(), it).aDominio() }.getOrNull() }
            ?: EstadoUsuario()
}
