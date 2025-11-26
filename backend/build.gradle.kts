import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val h2_version: String by project
val koin_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val hikaricp_version: String by project
val exposed: String by project
val bcrypt_version: String by project
val kotest_version: String by project
val mockk_version: String by project

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.ktor.plugin") version "3.3.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

group = "com.japp"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-status-pages")

    // Authentication
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")

    // Serialization
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    // Dependency Injection
    implementation("io.insert-koin:koin-ktor:${koin_version}")
    implementation("io.insert-koin:koin-logger-slf4j:${koin_version}")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:${exposed}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposed}")

    // Database
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("com.zaxxer:HikariCP:$hikaricp_version")

    // WebSockets
    implementation("io.ktor:ktor-server-websockets")

    // Hashing
    implementation("at.favre.lib:bcrypt:$bcrypt_version")

    // Configuration
    implementation("io.ktor:ktor-server-config-yaml")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Firebase SDK
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.kotest:kotest-runner-junit5:${kotest_version}")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation("io.kotest:kotest-property:$kotest_version")
    testImplementation("io.mockk:mockk:$mockk_version")
}
