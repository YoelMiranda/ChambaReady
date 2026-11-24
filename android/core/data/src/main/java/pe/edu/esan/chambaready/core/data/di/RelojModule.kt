package pe.edu.esan.chambaready.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Reloj inyectable. Las cuentas regresivas lo usan en vez de `LocalDate.now()` para que las
 * pruebas puedan fijar la fecha.
 */
@Module
@InstallIn(SingletonComponent::class)
object RelojModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
