package pe.edu.esan.chambaready.core.data.remoto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado del análisis del CV (P01) para usarlo en P02. En memoria: P02 viene justo después.
 * En modo demo queda vacío y P02 usa las habilidades de prueba.
 */
@Singleton
class CvSesion @Inject constructor() {
    @Volatile var analisis: CvAnalizado? = null
}
