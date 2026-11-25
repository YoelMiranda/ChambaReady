package pe.edu.esan.chambaready.core.data.remoto

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de la API /v1 que usa la app (ver docs/api.md). Retrofit genera la implementación.
 * Todas las rutas son relativas a API_BASE_URL (que termina en "/v1/"), por eso no empiezan con "/".
 * El token de Firebase lo agrega [InterceptorToken]; aquí no se repite.
 */
interface ApiChambaReady {

    /** ¿Esta cuenta de Firebase ya se registró en ChambaReady y con qué rol? */
    @GET("sesion")
    suspend fun sesion(): SesionDto

    @POST("registro")
    suspend fun registrar(@Body datos: RegistroDto): SesionDto

    @GET("yo/perfil")
    suspend fun perfil(): PerfilPracticanteDto

    @PUT("yo/perfil")
    suspend fun actualizarPerfil(@Body datos: ActualizarPerfilDto): PerfilPracticanteDto

    @PUT("yo/horas-libres")
    suspend fun guardarHorasLibres(@Body datos: HorasLibresDto): PerfilPracticanteDto

    @GET("vacantes")
    suspend fun vacantes(): List<VacanteDto>

    @GET("vacantes/{id}")
    suspend fun vacante(@Path("id") id: String): VacanteDto

    @GET("yo/postulaciones")
    suspend fun postulaciones(): List<PostulacionDto>

    @POST("yo/postulaciones")
    suspend fun postular(@Body datos: PostularDto): PostulacionDto

    // ---------- F6: IA ----------

    /** P24: preguntas SIN la respuesta (lista vacía si la vacante no evalúa o la empresa no tiene saldo). */
    @GET("vacantes/{id}/preguntas-filtro")
    suspend fun preguntasFiltro(@Path("id") vacanteId: String): List<PreguntaFiltroDto>

    /** Regla 13: responder las preguntas que pidió la empresa después de postular. */
    @POST("yo/postulaciones/{id}/respuestas-filtro")
    suspend fun responderFiltro(@Path("id") postulacionId: String, @Body datos: ResponderFiltroDto): PostulacionDto

    @GET("yo/mini-tests/{habilidadId}")
    suspend fun miniTest(@Path("habilidadId") habilidadId: String): MiniTestDto

    @POST("yo/mini-tests/{id}/respuestas")
    suspend fun responderMiniTest(@Path("id") miniTestId: String, @Body datos: ResponderMiniTestDto): ResultadoMiniTestDto

    @POST("yo/autoevaluacion")
    suspend fun autoevaluacion(@Body datos: AutoevaluacionDto): RankingDto

    @POST("yo/cv")
    suspend fun analizarCv(@Body datos: AnalizarCvDto): CvAnalizadoDto

    /** ¿Puede evaluarse? Con cuenta gratis, una sola vez. */
    @GET("yo/evaluacion")
    suspend fun estadoEvaluacion(): EstadoEvaluacionDto

    /** Un mini-test por habilidad elegida (máximo 4), al nivel declarado. */
    @POST("yo/evaluacion")
    suspend fun iniciarEvaluacion(@Body datos: IniciarEvaluacionDto): EvaluacionDto

    /** P02: el estudiante agrega o aclara habilidades escribiendo libremente. */
    @POST("yo/asistente")
    suspend fun conversarAsistente(@Body datos: MensajeAsistenteDto): RespuestaAsistenteDto

    // ---------- F7: archivos ----------

    /** Sube el archivo tal cual (el Content-Type va en el RequestBody). El nombre, codificado como URL. */
    @POST("archivos")
    suspend fun subirArchivo(@Query("tipo") tipo: String, @Header("X-Nombre-Archivo") nombre: String, @Body archivo: RequestBody): ArchivoDto

    // ---------- F8: pago simulado ----------

    @POST("yo/premium/pago-simulado")
    suspend fun pagarPremium(): EstadoPlanDto

    // ---------- F9: notificaciones ----------

    @GET("notificaciones")
    suspend fun notificaciones(): List<NotificacionDto>

    @POST("notificaciones/{id}/leida")
    suspend fun marcarLeida(@Path("id") id: String): retrofit2.Response<Unit>

    @POST("notificaciones/leer-todas")
    suspend fun marcarTodas(): retrofit2.Response<Unit>
}
