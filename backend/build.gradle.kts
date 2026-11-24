import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "pe.edu.esan.chambaready"
version = "0.1.0"

application {
    mainClass.set("pe.edu.esan.chambaready.api.AplicacionKt")
    applicationName = "chambaready-api"
    // Heroku Basic tiene 512 MB: dejamos espacio para la pila y el sistema.
    applicationDefaultJvmArgs = listOf("-Xmx300m", "-XX:+UseSerialGC")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    // Reglas de negocio compartidas con la app (probabilidad, viabilidad, saldo de evaluaciones…).
    implementation(project(":core-domain"))

    implementation(api.ktor.server.core)
    implementation(api.ktor.server.netty)
    implementation(api.ktor.server.content.negotiation)
    implementation(api.ktor.serialization.kotlinx.json)
    implementation(api.ktor.server.status.pages)
    implementation(api.ktor.server.cors)
    implementation(api.ktor.server.call.logging)

    implementation(api.exposed.core)
    implementation(api.exposed.jdbc)
    implementation(api.exposed.java.time)
    implementation(api.flyway.core)
    implementation(api.flyway.postgresql)
    implementation(api.postgresql)
    implementation(api.hikari)
    implementation(api.logback.classic)
    implementation(api.firebase.admin)

    testImplementation(kotlin("test-junit"))
    testImplementation(api.ktor.server.test.host)
    testImplementation(api.ktor.client.content.negotiation)
    testImplementation(api.embedded.postgres)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "failed") }
}
