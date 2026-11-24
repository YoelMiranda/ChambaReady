package pe.edu.esan.chambaready.core.data.remoto

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Error al hablar con la API, con un [message] en español listo para mostrar en pantalla.
 * @property codigo el del backend (NO_AUTENTICADO, CONFLICTO…), o null si ni siquiera hubo respuesta.
 * @property estadoHttp 401, 404, 409…; null si falló la conexión.
 */
class ErrorRemoto(
    val codigo: String?,
    mensaje: String,
    val estadoHttp: Int? = null,
    causa: Throwable? = null,
) : Exception(mensaje, causa) {
    val sinConexion: Boolean get() = estadoHttp == null && codigo == null
}

/**
 * Ejecuta una llamada de Retrofit y convierte cualquier falla en [ErrorRemoto].
 * Así los ViewModels solo atrapan un tipo de error y muestran `e.message`.
 */
suspend fun <T> llamarApi(json: Json, bloque: suspend () -> T): T = try {
    bloque()
} catch (e: CancellationException) {
    throw e // Cancelar una corrutina no es un error: hay que dejarlo pasar.
} catch (e: HttpException) {
    throw errorDesdeHttp(json, e)
} catch (e: IOException) {
    throw ErrorRemoto(null, "No pudimos conectarnos con ChambaReady. Revisa tu internet e inténtalo otra vez.", causa = e)
} catch (e: SerializationException) {
    throw ErrorRemoto("RESPUESTA_INVALIDA", "El servidor respondió algo que la app no entiende. Actualiza la app.", causa = e)
}

private fun errorDesdeHttp(json: Json, e: HttpException): ErrorRemoto {
    val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val error = cuerpo?.let { runCatching { json.decodeFromString<ErrorDto>(it) }.getOrNull() }
    val mensaje = error?.mensaje ?: when (e.code()) {
        401 -> "Tu sesión venció. Vuelve a iniciar sesión."
        403 -> "No tienes permiso para esto."
        404 -> "No encontramos lo que buscabas."
        in 500..599 -> "El servidor tuvo un problema. Inténtalo en unos minutos."
        else -> "Algo salió mal (error ${e.code()})."
    }
    return ErrorRemoto(error?.codigo, mensaje, e.code(), e)
}
