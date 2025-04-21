plugins {
    kotlin("jvm") version "2.1.0"
}

group = "io.github.utfunderscore"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    /**
     * Kotlin test library
     */
    testImplementation(kotlin("test"))

    /**
     * Javalin - Simple web framework for Java and Kotlin
     */
    implementation("io.javalin:javalin:6.4.0")

    /**
     * tinylog 2 - Tiny logging library
     */
    implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")

    /**
     * Kotlin logging - Logging library for Kotlin
     */
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    /**
     * Jackson - JSON library
     */
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    /**
     * Jackson Kotlin module - Jackson module for Kotlin
     */
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    /**
     * Kotlin Result - Result type for Kotlin
     */
    implementation("com.michael-bull.kotlin-result:kotlin-result:2.0.1")
    testImplementation("com.michael-bull.kotlin-result:kotlin-result:2.0.1")

    /**
     * HikariCP - High performance JDBC connection pool
     */
    implementation("com.zaxxer:HikariCP:5.1.0")

    /**
     * PostgreSQL JDBC driver
     */
    implementation("org.postgresql:postgresql:42.7.5")

    /**
     * Kafka messaging library
     */
    implementation("org.apache.kafka:kafka-clients:4.0.0")

    /**
     * Testcontainers - Create docker containers for needed dependencies during test runtime
     */
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.0")

    testImplementation("io.javalin:javalin-bundle:6.4.0")

    /**
     * MockK - Mocking library for Kotlin
     */
    testImplementation("io.mockk:mockk:1.13.16")
}

tasks.test {
    useTestNG()
}

tasks.test {
    useTestNG()
}
kotlin {
    jvmToolchain(23)
}
