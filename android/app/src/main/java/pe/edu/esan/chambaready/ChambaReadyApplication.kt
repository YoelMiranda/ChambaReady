package pe.edu.esan.chambaready

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pe.edu.esan.chambaready.core.data.remoto.SincronizadorPerfil
import javax.inject.Inject

@HiltAndroidApp
class ChambaReadyApplication : Application() {

    @Inject lateinit var sincronizador: SincronizadorPerfil

    /** Vive lo mismo que la app. SupervisorJob: si una tarea falla, las demás siguen. */
    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Con login (F5), el perfil se sube solo al backend cada vez que cambia.
        sincronizador.iniciar(alcanceApp)
    }
}
