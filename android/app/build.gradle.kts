import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Firebase (F5): cada integrante descarga su app/google-services.json desde la Consola de Firebase
// (está en .gitignore). Sin ese archivo la app compila igual y funciona en MODO DEMO, con datos de prueba.
val firebaseConfigurado = file("google-services.json").exists()
if (firebaseConfigurado) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Valores NO secretos que cada integrante define en su local.properties (ver local.properties.example).
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiBaseUrl: String = propiedadesLocales.getProperty("API_BASE_URL", "http://10.0.2.2:8080/v1/")
val webEmpresaUrl: String = propiedadesLocales.getProperty("WEB_EMPRESA_URL", "http://10.0.2.2:5173/")

android {
    namespace = "pe.edu.esan.chambaready"
    compileSdk = 36

    defaultConfig {
        applicationId = "pe.edu.esan.chambaready"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        // C01: "Soy empresa" abre la web de empresas en el navegador.
        buildConfigField("String", "WEB_EMPRESA_URL", "\"$webEmpresaUrl\"")
        // true = login real con Firebase + API; false = modo demo.
        buildConfigField("boolean", "FIREBASE_CONFIGURADO", "$firebaseConfigurado")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Pruebas instrumentadas (emulador): pila de navegación.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.ui.test.manifest)
}
