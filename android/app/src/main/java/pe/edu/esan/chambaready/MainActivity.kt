package pe.edu.esan.chambaready

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import pe.edu.esan.chambaready.navigation.ChambaReadyApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El tema se aplica dentro de ChambaReadyApp porque depende del rol activo.
            ChambaReadyApp()
        }
    }
}
