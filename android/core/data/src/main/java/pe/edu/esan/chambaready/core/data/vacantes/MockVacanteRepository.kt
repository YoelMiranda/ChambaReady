package pe.edu.esan.chambaready.core.data.vacantes

import kotlinx.coroutines.delay
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Repositorio falso de vacantes (F1–F3). Simula una pequeña demora de red en la lista. */
@Singleton
class MockVacanteRepository @Inject constructor(
    private val reloj: Clock,
) : VacanteRepository {

    override suspend fun listarVacantes(): List<VacanteDetalle> {
        delay(DEMORA_SIMULADA_MS)
        return MockDatos.vacantes(LocalDate.now(reloj))
    }

    override suspend fun obtenerVacante(id: String): VacanteDetalle? =
        MockDatos.vacantes(LocalDate.now(reloj)).firstOrNull { it.resumen.id == id }

    private companion object {
        const val DEMORA_SIMULADA_MS = 300L
    }
}
