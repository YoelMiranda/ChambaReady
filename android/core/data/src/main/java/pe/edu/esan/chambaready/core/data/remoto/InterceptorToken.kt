package pe.edu.esan.chambaready.core.data.remoto

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Entrega el ID token de Firebase del usuario actual, o null si no hay sesión. */
fun interface ProveedorToken {
    suspend fun token(): String?
}

/**
 * Agrega `Authorization: Bearer <token>` a cada pedido a la API. El backend verifica ese token con
 * Firebase Admin y nunca confía en un rol que mande el cliente.
 *
 * OkHttp llama a los interceptores en un hilo de red (nunca en el principal), así que aquí sí se
 * puede esperar con runBlocking. Firebase renueva solo el token cuando está por vencer.
 */
class InterceptorToken(private val proveedor: ProveedorToken) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { proveedor.token() }
        val pedido = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(pedido)
    }
}
