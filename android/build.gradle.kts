// Archivo de build a nivel de proyecto: solo declara los plugins, sin aplicarlos (cada módulo aplica los suyos).
//
// Kotlin: AGP 9 trae Kotlin integrado para los módulos Android, así que ellos no aplican
// org.jetbrains.kotlin.android. Declarar aquí kotlin.jvm (lo usa :core:domain) pone en el classpath
// el Kotlin Gradle Plugin 2.4.21, y AGP usa esa versión en vez de su 2.2.10 por defecto. Tiene que
// coincidir con el plugin de Compose, por eso ambos salen de la misma versión del catálogo.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Solo se aplica en :app si existe app/google-services.json (ver app/build.gradle.kts).
    alias(libs.plugins.google.services) apply false
}
