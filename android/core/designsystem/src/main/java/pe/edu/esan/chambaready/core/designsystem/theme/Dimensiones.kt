package pe.edu.esan.chambaready.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Medidas del sistema de diseño. Úsalas en vez de escribir números sueltos. */
object Dimensiones {
    /** Padding lateral de las pantallas (el diseño permite 20–24 dp). */
    val paddingLateral = 20.dp
    val alturaBoton = 52.dp
    val radioBoton = 14.dp
    val radioTarjeta = 16.dp
    val radioTarjetaGrande = 20.dp
    val bordeTarjeta = 1.dp
    /** Objetivo táctil mínimo de accesibilidad. */
    val tactilMinimo = 48.dp
    val espacioChico = 8.dp
    val espacioMedio = 12.dp
    val espacioGrande = 16.dp
    val espacioSeccion = 24.dp
}

/** Formas de Material 3: los botones usan `medium` (14 dp) y las tarjetas `large` (16 dp). */
val ChambaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(Dimensiones.radioBoton),
    large = RoundedCornerShape(Dimensiones.radioTarjeta),
    extraLarge = RoundedCornerShape(Dimensiones.radioTarjetaGrande),
)
