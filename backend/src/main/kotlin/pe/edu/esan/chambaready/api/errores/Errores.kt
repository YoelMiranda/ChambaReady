package pe.edu.esan.chambaready.api.errores

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/** Respuesta de error de la API: un código estable (para el cliente) y un mensaje para mostrar. */
@Serializable
data class ErrorDto(val codigo: String, val mensaje: String)

/**
 * Errores de la API. Lánzalos desde los servicios; [instalarManejoDeErrores] los convierte en
 * la respuesta HTTP correcta.
 */
sealed class ErrorApi(val estado: HttpStatusCode, val codigo: String, mensaje: String) : RuntimeException(mensaje) {
    class SolicitudInvalida(mensaje: String) : ErrorApi(HttpStatusCode.BadRequest, "SOLICITUD_INVALIDA", mensaje)
    class NoAutenticado : ErrorApi(HttpStatusCode.Unauthorized, "NO_AUTENTICADO", "Inicia sesión para continuar.")
    class Prohibido(mensaje: String = "No tienes permiso para esto.") : ErrorApi(HttpStatusCode.Forbidden, "PROHIBIDO", mensaje)
    class NoEncontrado(mensaje: String) : ErrorApi(HttpStatusCode.NotFound, "NO_ENCONTRADO", mensaje)
    class Conflicto(mensaje: String) : ErrorApi(HttpStatusCode.Conflict, "CONFLICTO", mensaje)

    /** Tiene una cuenta de Firebase válida pero aún no llamó a POST /v1/registro. */
    class RegistroPendiente : ErrorApi(HttpStatusCode.Forbidden, "REGISTRO_PENDIENTE", "Completa tu registro para continuar.")

    /** Una regla de negocio no se cumple (por ejemplo, publicar sin verificación). */
    class ReglaDeNegocio(codigo: String, mensaje: String) : ErrorApi(HttpStatusCode.UnprocessableEntity, codigo, mensaje)
}

private val log = LoggerFactory.getLogger("ErroresApi")

fun Application.instalarManejoDeErrores() {
    install(StatusPages) {
        exception<ErrorApi> { call, error ->
            call.respond(error.estado, ErrorDto(error.codigo, error.message ?: ""))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorDto("SOLICITUD_INVALIDA", "El cuerpo de la solicitud no tiene el formato esperado."))
        }
        exception<Throwable> { call, error ->
            // No se devuelve el detalle al cliente: puede contener datos internos.
            log.error("Error no controlado", error)
            call.respond(HttpStatusCode.InternalServerError, ErrorDto("ERROR_INTERNO", "Algo salió mal. Inténtalo de nuevo."))
        }
    }
}
