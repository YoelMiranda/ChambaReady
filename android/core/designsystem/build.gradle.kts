// :core:designsystem: tema, tipografía y componentes reutilizables de Compose (con sus @Preview).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "pe.edu.esan.chambaready.core.designsystem"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Los componentes reciben modelos del dominio (NivelHabilidad, TipoSello, VacanteResumen…).
    api(project(":core:domain"))

    // "api" para que :app use Compose y Material 3 sin volver a declararlos.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
}
