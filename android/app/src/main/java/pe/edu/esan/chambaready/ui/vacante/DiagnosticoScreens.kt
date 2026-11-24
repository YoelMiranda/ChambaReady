package pe.edu.esan.chambaready.ui.vacante

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.edu.esan.chambaready.core.data.mock.MockDatos
import pe.edu.esan.chambaready.core.data.repositorio.EstadoUsuarioRepository
import pe.edu.esan.chambaready.core.data.servicio.ServicioPreparacion
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.ProbabilityScale
import pe.edu.esan.chambaready.core.designsystem.componentes.PuntoEscala
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.formatoHoras
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaTema
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.reglas.Diagnostico
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import pe.edu.esan.chambaready.core.domain.vacantes.VacanteRepository
import pe.edu.esan.chambaready.core.viewmodel.BaseViewModel
import pe.edu.esan.chambaready.ui.comun.BarraAcciones
import pe.edu.esan.chambaready.ui.comun.BloqueoPremium
import pe.edu.esan.chambaready.ui.comun.CargandoPantalla
import pe.edu.esan.chambaready.ui.comun.ContenidoDesplazable
import pe.edu.esan.chambaready.ui.comun.Etiqueta
import pe.edu.esan.chambaready.ui.comun.PantallaBase
import pe.edu.esan.chambaready.ui.comun.TarjetaSeccion
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class DiagnosticoUiState(
    val vacante: VacanteDetalle? = null,
    val diagnostico: Diagnostico? = null,
    val premium: Boolean = false,
    /** P13: vacante parecida más alcanzable (mismo área, viable y con más probabilidad). */
    val parecida: VacanteDetalle? = null,
    val esMeta: Boolean = false,
)

/**
 * P10 Diagnóstico y P13 No viable. El resumen es gratis; el detalle y los cursos son Premium (regla 6).
 * La vacante llega por la ruta: las dos rutas tienen el argumento `vacanteId`.
 */
@HiltViewModel
class DiagnosticoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vacantes: VacanteRepository,
    private val estadoRepositorio: EstadoUsuarioRepository,
    private val servicio: ServicioPreparacion,
) : BaseViewModel<DiagnosticoUiState, Nothing>(DiagnosticoUiState()) {

    private val vacanteId: String = checkNotNull(savedStateHandle["vacanteId"])

    init {
        viewModelScope.launch {
            val vacante = vacantes.obtenerVacante(vacanteId) ?: return@launch
            val todas = vacantes.listarVacantesOVacio()
            // El "antes" de P16: la probabilidad la primera vez que vio el diagnóstico.
            estadoRepositorio.actualizar { e ->
                if (vacanteId in e.probabilidadInicial) e
                else e.copy(probabilidadInicial = e.probabilidadInicial + (vacanteId to servicio.probabilidad(vacante, e)))
            }
            estadoRepositorio.estado.collect { e ->
                setState {
                    copy(
                        vacante = vacante,
                        diagnostico = servicio.diagnostico(vacante, e),
                        premium = e.premium,
                        esMeta = vacanteId in e.metas,
                        parecida = todas
                            .filter { it.resumen.id != vacanteId && it.area == vacante.area && servicio.diasRestantes(it) >= 0 }
                            .filter { servicio.diagnostico(it, e).viabilidad.esViable }
                            .maxByOrNull { servicio.probabilidad(it, e) },
                    )
                }
            }
        }
    }

    /** Abre el plan: queda como plan activo (Mis pendientes y el aviso de P22 lo usan). */
    fun activarPlan() {
        viewModelScope.launch { estadoRepositorio.actualizar { it.copy(planesActivos = it.planesActivos + vacanteId) } }
    }

    fun guardarComoMeta() {
        viewModelScope.launch { estadoRepositorio.actualizar { it.copy(metas = it.metas + vacanteId) } }
    }
}

// ---------- P10 Diagnóstico ----------

@Composable
fun DiagnosticoScreen(
    onBack: () -> Unit,
    onPremium: (String) -> Unit,
    onPlan: (String) -> Unit,
    onNoViable: (String) -> Unit,
    viewModel: DiagnosticoViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val abridor = LocalUriHandler.current
    DiagnosticoContenido(
        estado = estado,
        onBack = onBack,
        onPremium = onPremium,
        onPlan = { id -> viewModel.activarPlan(); onPlan(id) },
        onNoViable = onNoViable,
        onAbrirCurso = { abridor.openUri(it) },
    )
}

@Composable
fun DiagnosticoContenido(
    estado: DiagnosticoUiState,
    onBack: () -> Unit,
    onPremium: (String) -> Unit,
    onPlan: (String) -> Unit,
    onNoViable: (String) -> Unit,
    onAbrirCurso: (String) -> Unit,
) {
    val vacante = estado.vacante
    val d = estado.diagnostico
    PantallaBase(
        titulo = "Diagnóstico",
        onBack = onBack,
        barraInferior = {
            if (vacante != null && d != null) {
                val id = vacante.resumen.id
                BarraAcciones {
                    when {
                        d.brechas.isEmpty() -> Text("No tienes brechas para esta vacante. ¡Puedes postular!", style = MaterialTheme.typography.bodyMedium)
                        !estado.premium -> PrimaryButton("Desbloquear con Premium", onClick = { onPremium(id) }, modifier = Modifier.fillMaxWidth())
                        d.viabilidad.esViable -> PrimaryButton("Ver mi plan", onClick = { onPlan(id) }, modifier = Modifier.fillMaxWidth())
                        else -> PrimaryButton("Ver alternativas", onClick = { onNoViable(id) }, modifier = Modifier.fillMaxWidth())
                    }
                    if (d.brechas.isNotEmpty() && !d.viabilidad.esViable && !estado.premium) {
                        SecondaryButton("Ver alternativas", onClick = { onNoViable(id) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { padding ->
        if (vacante == null || d == null) {
            CargandoPantalla()
            return@PantallaBase
        }
        ContenidoDesplazable(padding) {
            Text(vacante.resumen.puesto, style = MaterialTheme.typography.titleMedium)
            ResumenDiagnostico(d)
            if (estado.premium) {
                TarjetaSeccion(titulo = "Detalle y cursos recomendados") {
                    d.brechas.forEachIndexed { i, b ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text("${b.requisito.nombre}: de ${b.nivelActual.etiqueta} a ${b.requisito.nivelExigido.etiqueta}", style = MaterialTheme.typography.bodyLarge)
                        b.curso?.let { curso ->
                            Text("${curso.titulo} · ${curso.proveedor} · ${formatoHoras(b.horas)}", style = MaterialTheme.typography.bodyMedium, color = ChambaTema.colores.textoSecundario)
                            SecondaryButton("Abrir curso externo", onClick = { onAbrirCurso(curso.url) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Text(
                        "Los cursos son externos. Terminar un curso no sube tu nivel: lo actualizamos con un mini-test o una evidencia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChambaTema.colores.textoSecundario,
                    )
                }
            } else if (d.brechas.isNotEmpty()) {
                BloqueoPremium(
                    titulo = "Detalle y cursos recomendados",
                    descripcion = "Con Premium ves qué curso tomar para cada brecha y te armamos un plan semanal que cabe en tus horas libres.",
                    onDesbloquear = { onPremium(vacante.resumen.id) },
                )
            }
        }
    }
}

/** Resumen gratis: n.º de brechas, horas, si llega a tiempo y probabilidad actual → meta. */
@Composable
private fun ResumenDiagnostico(d: Diagnostico) {
    val colores = ChambaTema.colores
    TarjetaSeccion(titulo = "Resumen") {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico), verticalAlignment = Alignment.CenterVertically) {
            if (d.viabilidad.esViable) {
                Etiqueta("LLEGAS A TIEMPO", colores.exitoFondo, colores.exitoTexto)
            } else {
                Etiqueta("NO LLEGAS A TIEMPO", colores.avisoRojoFondo, colores.avisoRojoTexto)
            }
        }
        Text(
            "${d.brechas.size} ${if (d.brechas.size == 1) "brecha" else "brechas"} · ${formatoHoras(d.horasNecesarias)} necesarias · ${formatoHoras(d.viabilidad.horasDisponibles)} disponibles antes del cierre",
            style = MaterialTheme.typography.bodyLarge,
        )
        ProbabilityScale(
            porcentaje = d.probabilidadActual,
            etiqueta = "Hoy",
            puntosExtra = listOf(PuntoEscala(d.probabilidadMeta, "Si cierras tus brechas")),
        )
        d.brechas.forEach { b ->
            Row(Modifier.fillMaxWidth()) {
                Text("${b.requisito.nombre} (${b.requisito.tipo.etiqueta.lowercase()})", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(formatoHoras(b.horas), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ---------- P13 No viable ----------

@Composable
fun NoViableScreen(
    onBack: () -> Unit,
    onVacante: (String) -> Unit,
    onSubirHoras: () -> Unit,
    onPostular: (String) -> Unit,
    viewModel: DiagnosticoViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    NoViableContenido(
        estado = estado,
        snackbar = snackbar,
        onBack = onBack,
        onVacante = onVacante,
        onSubirHoras = onSubirHoras,
        onGuardarMeta = {
            viewModel.guardarComoMeta()
            alcance.launch { snackbar.showSnackbar("Guardada como meta sin fecha. La verás en Mis pendientes.") }
        },
        onPostular = onPostular,
    )
}

@Composable
fun NoViableContenido(
    estado: DiagnosticoUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onVacante: (String) -> Unit,
    onSubirHoras: () -> Unit,
    onGuardarMeta: () -> Unit,
    onPostular: (String) -> Unit,
) {
    val vacante = estado.vacante
    val d = estado.diagnostico
    PantallaBase(titulo = "No llegas a tiempo", onBack = onBack, snackbarHostState = snackbar) { padding ->
        if (vacante == null || d == null) {
            CargandoPantalla()
            return@PantallaBase
        }
        ContenidoDesplazable(padding) {
            Text(vacante.resumen.puesto, style = MaterialTheme.typography.titleMedium)
            TarjetaSeccion(titulo = "Horas necesarias vs. disponibles") {
                val maximo = maxOf(d.horasNecesarias, d.viabilidad.horasDisponibles, 1.0)
                BarraHoras("Necesarias", d.horasNecesarias, maximo, ChambaTema.colores.avisoRojoTexto)
                BarraHoras("Disponibles", d.viabilidad.horasDisponibles, maximo, MaterialTheme.colorScheme.primary)
                Text("Te faltan ${formatoHoras(d.viabilidad.horasFaltantes)} antes del cierre.", style = MaterialTheme.typography.bodyMedium)
            }
            Text("¿Qué quieres hacer?", style = MaterialTheme.typography.titleMedium)
            estado.parecida?.let { parecida ->
                TarjetaSeccion(titulo = "Una vacante parecida más alcanzable") {
                    Text("${parecida.resumen.puesto} · ${parecida.resumen.empresa}", style = MaterialTheme.typography.bodyLarge)
                    PrimaryButton("Ver vacante", onClick = { onVacante(parecida.resumen.id) }, modifier = Modifier.fillMaxWidth())
                }
            }
            SecondaryButton("Subir mis horas libres", onClick = onSubirHoras, modifier = Modifier.fillMaxWidth())
            SecondaryButton(
                if (estado.esMeta) "Ya está guardada como meta" else "Guardarla como meta sin fecha",
                onClick = onGuardarMeta,
                enabled = !estado.esMeta,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton("Postular igual", onClick = { onPostular(vacante.resumen.id) }, modifier = Modifier.fillMaxWidth())
            Text("Postular nunca se bloquea por falta de horas.", style = MaterialTheme.typography.bodySmall, color = ChambaTema.colores.textoSecundario)
        }
    }
}

@Composable
private fun BarraHoras(etiqueta: String, horas: Double, maximo: Double, color: Color) {
    Column(Modifier.clearAndSetSemantics { contentDescription = "$etiqueta: ${formatoHoras(horas)}" }) {
        Row(Modifier.fillMaxWidth()) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(formatoHoras(horas), style = MaterialTheme.typography.labelMedium)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
            Box(Modifier.fillMaxWidth((horas / maximo).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color, CircleShape))
        }
    }
}

// ---------- Previews ----------

private fun diagnosticoDePrueba(): Pair<VacanteDetalle, Diagnostico> {
    val hoy = LocalDate.now()
    val vacante = MockDatos.vacantes(hoy).first()
    val niveles = NivelesUsuario(mapOf("sql" to pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad.BASICO))
    val horas = HorasLibres(mapOf(DayOfWeek.SATURDAY to 4.0))
    return vacante to MotorDiagnostico.diagnosticar(vacante, niveles, MockDatos.cursos, horas, hoy)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun DiagnosticoPreview() {
    val (vacante, d) = diagnosticoDePrueba()
    ChambaReadyTheme {
        DiagnosticoContenido(DiagnosticoUiState(vacante, d, premium = false), onBack = {}, onPremium = {}, onPlan = {}, onNoViable = {}, onAbrirCurso = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun NoViablePreview() {
    val (vacante, d) = diagnosticoDePrueba()
    ChambaReadyTheme {
        NoViableContenido(DiagnosticoUiState(vacante, d), SnackbarHostState(), onBack = {}, onVacante = {}, onSubirHoras = {}, onGuardarMeta = {}, onPostular = {})
    }
}
