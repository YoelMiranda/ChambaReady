import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :core:domain es Kotlin PURO: modelos y reglas de negocio, sin ninguna dependencia de Android.
// Así lo puede reutilizar el backend (Ktor) y, más adelante, una versión Compose Multiplatform.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
