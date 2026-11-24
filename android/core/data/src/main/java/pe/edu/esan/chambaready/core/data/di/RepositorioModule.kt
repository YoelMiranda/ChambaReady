package pe.edu.esan.chambaready.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.edu.esan.chambaready.core.data.local.DataStoreEstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.mock.MockContenidoRepository
import pe.edu.esan.chambaready.core.data.repositorio.ContenidoRepository
import pe.edu.esan.chambaready.core.data.remoto.ConfigApp
import pe.edu.esan.chambaready.core.data.remoto.CuentaLocalRepository
import pe.edu.esan.chambaready.core.data.remoto.CuentaRemotaRepository
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.vacantes.MockVacanteRepository
import pe.edu.esan.chambaready.core.data.vacantes.RemotoVacanteRepository
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Enlaza cada interfaz de repositorio con su implementación. Las pantallas solo conocen la
 * interfaz, así que cambiar de datos de prueba a la API no las toca.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositorioModule {

    @Binds
    @Singleton
    abstract fun bindContenidoRepository(impl: MockContenidoRepository): ContenidoRepository

    @Binds
    @Singleton
    abstract fun bindEstadoUsuarioRepository(impl: DataStoreEstadoUsuarioRepository): EstadoUsuarioRepository
}

/**
 * Con backend (hay google-services.json) se usa la API; sin él, los datos de prueba (MODO DEMO).
 * `Provider` crea solo la implementación elegida: en modo demo nunca se arma Retrofit.
 */
@Module
@InstallIn(SingletonComponent::class)
object FuenteDeDatosModule {

    @Provides
    @Singleton
    fun provideVacanteRepository(
        config: ConfigApp,
        remoto: Provider<RemotoVacanteRepository>,
        prueba: Provider<MockVacanteRepository>,
    ): VacanteRepository = if (config.usaBackend) remoto.get() else prueba.get()

    @Provides
    @Singleton
    fun provideCuentaRepository(
        config: ConfigApp,
        remota: Provider<CuentaRemotaRepository>,
        local: Provider<CuentaLocalRepository>,
    ): CuentaRepository = if (config.usaBackend) remota.get() else local.get()
}

/** Un solo DataStore para toda la app (crear dos sobre el mismo archivo da error). */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("chambaready_estado") })
}
