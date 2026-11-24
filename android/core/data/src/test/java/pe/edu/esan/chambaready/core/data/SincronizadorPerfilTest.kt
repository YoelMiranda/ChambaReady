package pe.edu.esan.chambaready.core.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.esan.chambaready.core.data.auth.SesionRepository
import pe.edu.esan.chambaready.core.data.remoto.CuentaRepository
import pe.edu.esan.chambaready.core.data.remoto.SincronizadorPerfil
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuario
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.DatosRegistro
import pe.edu.esan.chambaready.core.domain.modelo.Postulacion
import pe.edu.esan.chambaready.core.domain.reglas.DestinoTrasLogin

@OptIn(ExperimentalCoroutinesApi::class)
class SincronizadorPerfilTest {

    private class EstadoFalso(inicial: EstadoUsuario) : EstadoUsuarioRepository {
        val flujo = MutableStateFlow(inicial)
        override val estado: Flow<EstadoUsuario> = flujo
        override suspend fun actualizar(cambio: (EstadoUsuario) -> EstadoUsuario) = flujo.update(cambio)
        override suspend fun borrarTodo() = flujo.update { EstadoUsuario() }
    }

    private class CuentaFalsa : CuentaRepository {
        val subidos = mutableListOf<EstadoUsuario>()
        override suspend fun subirPerfil(estado: EstadoUsuario) {
            subidos += estado
        }
        override suspend fun destinoTrasLogin() = DestinoTrasLogin.ENTRAR
        override suspend fun registrarPracticante(nombre: String) = Unit
        override suspend fun traerPerfil(actual: EstadoUsuario, correo: String) = actual
        override suspend fun postular(local: Postulacion, declaraciones: Map<String, Boolean>, declaracionJurada: Boolean) = local
        override suspend fun postulaciones(): List<Postulacion>? = null
    }

    private class SesionFalsa(override val disponible: Boolean = true, var conSesion: Boolean = true) : SesionRepository {
        override fun haySesion() = conSesion
        override fun correoActual() = "ana@uni.pe"
        override suspend fun token(): String? = "t"
        override suspend fun crearCuenta(correo: String, contrasena: String) = Unit
        override suspend fun iniciarSesion(correo: String, contrasena: String) = Unit
        override suspend fun enviarCorreoDeRecuperacion(correo: String) = Unit
        override fun cerrarSesion() = Unit
    }

    private val registrado = EstadoUsuario(registro = DatosRegistro("Ana", "ana@uni.pe", "ESAN", "Economía", 6))

    @Test
    fun `antes del registro no sube nada`() = runTest {
        val cuenta = CuentaFalsa()
        SincronizadorPerfil(EstadoFalso(EstadoUsuario()), cuenta, SesionFalsa()).iniciar(backgroundScope)
        advanceTimeBy(5_000)
        assertEquals(0, cuenta.subidos.size)
    }

    @Test
    fun `varios cambios seguidos se suben una sola vez`() = runTest {
        val estado = EstadoFalso(registrado)
        val cuenta = CuentaFalsa()
        SincronizadorPerfil(estado, cuenta, SesionFalsa()).iniciar(backgroundScope)
        runCurrent()
        AreaInteres.entries.take(3).forEach { area ->
            estado.actualizar { it.copy(area = area) }
            advanceTimeBy(300)
        }
        advanceTimeBy(1_500)
        assertEquals(1, cuenta.subidos.size)
        assertEquals(AreaInteres.entries[2], cuenta.subidos.single().area)
    }

    @Test
    fun `los cambios que el backend no guarda no generan pedidos`() = runTest {
        val estado = EstadoFalso(registrado)
        val cuenta = CuentaFalsa()
        SincronizadorPerfil(estado, cuenta, SesionFalsa()).iniciar(backgroundScope)
        advanceTimeBy(1_500) // la subida inicial al abrir la app
        estado.actualizar { it.copy(metas = setOf("v1"), notificacionesLeidas = setOf("n1")) }
        advanceTimeBy(1_500)
        assertEquals(1, cuenta.subidos.size)
    }

    @Test
    fun `sin sesion o en modo demo no sube`() = runTest {
        val cuenta = CuentaFalsa()
        SincronizadorPerfil(EstadoFalso(registrado), cuenta, SesionFalsa(conSesion = false)).iniciar(backgroundScope)
        SincronizadorPerfil(EstadoFalso(registrado), cuenta, SesionFalsa(disponible = false)).iniciar(backgroundScope)
        advanceTimeBy(5_000)
        assertEquals(0, cuenta.subidos.size)
    }
}
