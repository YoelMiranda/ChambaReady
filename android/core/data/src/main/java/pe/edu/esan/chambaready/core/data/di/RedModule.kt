package pe.edu.esan.chambaready.core.data.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pe.edu.esan.chambaready.core.data.auth.FirebaseSesionRepository
import pe.edu.esan.chambaready.core.data.auth.SesionDemo
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.remoto.ApiChambaReady
import pe.edu.esan.chambaready.core.data.remoto.ConfigApp
import pe.edu.esan.chambaready.core.data.remoto.InterceptorToken
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Red y sesión (F5). [ConfigApp] lo entrega el módulo :app (ver `ConfigModule`).
 * En MODO DEMO nada de esto se crea: Hilt solo arma lo que alguien pide, y los repositorios de
 * prueba no piden la API.
 */
@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    /** JSON tolerante: si el backend agrega un campo nuevo, la app vieja lo ignora en vez de fallar. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideSesion(config: ConfigApp): SesionRepository =
        if (config.usaBackend) FirebaseSesionRepository(FirebaseAuth.getInstance()) else SesionDemo()

    @Provides
    @Singleton
    fun provideOkHttp(config: ConfigApp, sesion: SesionRepository): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(InterceptorToken(sesion))
            // Heroku puede tardar unos segundos en responder el primer pedido tras un deploy.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (config.depuracion) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization") // El token nunca se escribe en Logcat.
                },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApi(config: ConfigApp, cliente: OkHttpClient, json: Json): ApiChambaReady =
        Retrofit.Builder()
            .baseUrl(config.apiBaseUrl)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiChambaReady::class.java)
}
