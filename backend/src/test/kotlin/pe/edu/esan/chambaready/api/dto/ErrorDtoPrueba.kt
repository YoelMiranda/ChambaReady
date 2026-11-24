package pe.edu.esan.chambaready.api.dto

import kotlinx.serialization.Serializable

/** Lee las respuestas de error en las pruebas (mismo formato que `ErrorDto`). */
@Serializable
data class ErrorDtoPrueba(val codigo: String, val mensaje: String)
