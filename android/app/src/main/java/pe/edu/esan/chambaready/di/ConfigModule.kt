package pe.edu.esan.chambaready.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.edu.esan.chambaready.BuildConfig
import pe.edu.esan.chambaready.core.data.remoto.ConfigApp
import javax.inject.Singleton

/**
 * Pasa a :core:data los valores de BuildConfig (que solo existen en :app). Ver app/build.gradle.kts:
 * FIREBASE_CONFIGURADO es true cuando el proyecto tiene app/google-services.json.
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideConfigApp(): ConfigApp = ConfigApp(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        usaBackend = BuildConfig.FIREBASE_CONFIGURADO,
        depuracion = BuildConfig.DEBUG,
    )
}
