pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        // El mismo catálogo que la app Android: así Kotlin tiene una sola versión en todo el monorepo.
        create("libs") { from(files("../android/gradle/libs.versions.toml")) }
        // Librerías que solo usa el backend.
        create("api") { from(files("gradle/api.versions.toml")) }
    }
}

rootProject.name = "chambaready-api"

// Reutiliza el módulo de reglas de negocio de la app (Kotlin puro): una sola fuente de verdad.
include(":core-domain")
project(":core-domain").projectDir = file("../android/core/domain")
