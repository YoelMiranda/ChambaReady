package pe.edu.esan.chambaready.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pe.edu.esan.chambaready.core.designsystem.R

/**
 * Plus Jakarta Sans empaquetada en `res/font` (decisión de la Fase 0): funciona sin internet y se
 * ve en los `@Preview`, a diferencia de las fuentes descargables.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/** Solo para el código de los mini-tests (P15). */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/**
 * Sin color fijo a propósito: el texto toma el color de su contenedor (LocalContentColor). Así sale
 * oscuro sobre fondos claros (onBackground/onSurface = TextoPrincipal) y blanco sobre botones o el
 * marcador negro de la escala. Con un color fijo aquí, esos textos quedaban oscuros sobre oscuro.
 */
private val Base = TextStyle(fontFamily = PlusJakartaSans)

/**
 * Escala tipográfica. Equivalencias con el sistema de diseño:
 * - Título de pantalla (24 sp ExtraBold) → `headlineSmall`
 * - Subtítulo (16 sp Bold) → `titleMedium`
 * - Cuerpo (15 y 14 sp) → `bodyLarge` y `bodyMedium`
 * - Etiquetas (13 y 12 sp) → `labelMedium` y `labelSmall`
 */
val ChambaTypography = Typography(
    displaySmall = Base.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = Base.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = Base.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = Base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = Base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    titleSmall = Base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Base.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = Base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = Base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = Base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = Base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = Base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
)

/** Estilo para fragmentos de código dentro de los mini-tests. */
val EstiloCodigo = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 19.sp)
