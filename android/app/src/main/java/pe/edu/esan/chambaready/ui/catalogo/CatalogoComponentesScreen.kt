package pe.edu.esan.chambaready.ui.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.esan.chambaready.core.designsystem.componentes.BottomNavBar
import pe.edu.esan.chambaready.core.designsystem.componentes.ChipSelector
import pe.edu.esan.chambaready.core.designsystem.componentes.CountdownChip
import pe.edu.esan.chambaready.core.designsystem.componentes.ItemsBarraInferior
import pe.edu.esan.chambaready.core.designsystem.componentes.PrimaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.ProbabilityScale
import pe.edu.esan.chambaready.core.designsystem.componentes.PuntoEscala
import pe.edu.esan.chambaready.core.designsystem.componentes.SealBadge
import pe.edu.esan.chambaready.core.designsystem.componentes.SecondaryButton
import pe.edu.esan.chambaready.core.designsystem.componentes.SkillLevelBar
import pe.edu.esan.chambaready.core.designsystem.componentes.StepProgress
import pe.edu.esan.chambaready.core.designsystem.componentes.VacancyCard
import pe.edu.esan.chambaready.core.designsystem.componentes.WeekHoursEditor
import pe.edu.esan.chambaready.core.designsystem.theme.ChambaReadyTheme
import pe.edu.esan.chambaready.core.designsystem.theme.Dimensiones
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.TipoSello
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import java.time.DayOfWeek
import java.time.LocalDate

/** Catálogo del sistema de diseño (solo para el equipo): los 12 componentes funcionando en el emulador. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoComponentesScreen(onBack: () -> Unit) {
    var nivel by rememberSaveable { mutableStateOf(NivelHabilidad.INTERMEDIO) }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var horas by remember { mutableStateOf(HorasLibres(mapOf(DayOfWeek.MONDAY to 2.0, DayOfWeek.SATURDAY to 4.0))) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Componentes", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            BottomNavBar(items = ItemsBarraInferior.practicante, indiceSeleccionado = pestana, onSeleccion = { pestana = it })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensiones.paddingLateral, vertical = Dimensiones.espacioGrande),
            verticalArrangement = Arrangement.spacedBy(Dimensiones.espacioGrande),
        ) {
            Seccion("PrimaryButton / SecondaryButton")
            PrimaryButton("Mejorar mis probabilidades", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Postular sin preparación", onClick = {}, modifier = Modifier.fillMaxWidth())

            Seccion("StepProgress")
            StepProgress(pasoActual = 2, totalPasos = 6)

            Seccion("DayHoursStepper (WeekHoursEditor)")
            WeekHoursEditor(horasLibres = horas, onCambio = { horas = it })

            Seccion("ChipSelector")
            ChipSelector(
                opciones = NivelHabilidad.entries,
                seleccionadas = setOf(nivel),
                onClick = { nivel = it },
                etiqueta = { it.etiqueta },
            )

            Seccion("SkillLevelBar")
            SkillLevelBar("SQL", nivel, nivelRequerido = NivelHabilidad.AVANZADO)

            Seccion("CountdownChip")
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico)) {
                CountdownChip(2)
                CountdownChip(5)
                CountdownChip(14)
            }

            Seccion("SealBadge")
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensiones.espacioChico)) {
                TipoSello.entries.forEach { SealBadge(it) }
            }

            Seccion("ProbabilityScale")
            ProbabilityScale(porcentaje = 58)
            Spacer(Modifier.height(Dimensiones.espacioChico))
            ProbabilityScale(
                porcentaje = 52,
                etiqueta = "Ahora",
                puntosExtra = listOf(PuntoEscala(31, "Antes"), PuntoEscala(78, "Meta")),
            )

            Seccion("VacancyCard")
            VacancyCard(
                vacante = VacanteResumen(
                    id = "demo",
                    puesto = "Practicante de Analítica de Datos",
                    empresa = "Banco Andino",
                    modalidad = Modalidad.HIBRIDA,
                    sueldo = "S/ 1,300",
                    fechaCierre = LocalDate.now().plusDays(3),
                    probabilidad = 74,
                ),
                diasRestantes = 3,
                onClick = {},
            )
            Seccion("BottomNavBar (abajo)")
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(Dimensiones.espacioMedio))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1800)
@Composable
private fun CatalogoComponentesPreview() {
    ChambaReadyTheme {
        CatalogoComponentesScreen(onBack = {})
    }
}
