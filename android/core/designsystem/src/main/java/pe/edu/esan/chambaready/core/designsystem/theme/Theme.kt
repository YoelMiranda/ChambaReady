package pe.edu.esan.chambaready.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Esquema del practicante (verde). La app Android es solo para estudiantes; las empresas usan la
 * web. `secondaryContainer` es el color de la píldora del ítem activo de la barra inferior.
 */
private val EsquemaPracticante = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = Superficie,
    primaryContainer = VerdeTenue,
    onPrimaryContainer = VerdeTextoSobreClaro,
    secondary = TextoSecundario,
    onSecondary = Superficie,
    secondaryContainer = VerdeTenue,
    onSecondaryContainer = VerdeTextoSobreClaro,
    tertiary = AzulMarino,
    onTertiary = Superficie,
    background = FondoPracticante,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = FondoPracticante,
    onSurfaceVariant = TextoSecundario,
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,
    surfaceContainerHigh = Superficie,
    surfaceContainerHighest = FondoPracticante,
    outline = Borde,
    outlineVariant = Borde,
    error = RojoTexto,
    onError = Superficie,
    errorContainer = RojoFondo,
    onErrorContainer = RojoTexto,
)

/**
 * Colores que Material 3 no tiene: avisos, escala de probabilidad y sellos. Se leen con
 * `ChambaTema.colores`.
 */
@Immutable
data class ColoresChamba(
    val avisoAmbarTexto: Color = AmbarTexto,
    val avisoAmbarFondo: Color = AmbarFondo,
    val avisoRojoTexto: Color = RojoTexto,
    val avisoRojoFondo: Color = RojoFondo,
    val exitoTexto: Color = VerdeTextoSobreClaro,
    val exitoFondo: Color = VerdeTenue,
    val zonaBaja: Color = ZonaBaja,
    val zonaMedia: Color = ZonaMedia,
    val zonaAlta: Color = ZonaAlta,
    val marcadorEscala: Color = MarcadorEscala,
    val selloConfirmado: Color = SelloConfirmado,
    val selloDeclarado: Color = SelloDeclarado,
    val selloNoCumple: Color = SelloNoCumple,
    val textoSecundario: Color = TextoSecundario,
    val borde: Color = Borde,
)

private val LocalColoresChamba = staticCompositionLocalOf { ColoresChamba() }

/** Acceso a los tokens propios de ChambaReady: `ChambaTema.colores.zonaAlta`. */
object ChambaTema {
    val colores: ColoresChamba
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresChamba.current
}

/** Tema de la app. Por ahora solo hay modo claro, porque los mockups no definen un modo oscuro. */
@Composable
fun ChambaReadyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColoresChamba provides ColoresChamba()) {
        MaterialTheme(
            colorScheme = EsquemaPracticante,
            typography = ChambaTypography,
            shapes = ChambaShapes,
            content = content,
        )
    }
}
