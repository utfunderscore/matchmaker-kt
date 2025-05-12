plugins {
    kotlin("jvm") version "2.1.0"
    application
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
    implementation("io.javalin:javalin:6.6.0")
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:6.6.0")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:6.6.0")
    implementation("io.javalin.community.openapi:javalin-redoc-plugin:6.6.0")

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
     * Kafka messaging library
     */
    implementation("org.apache.kafka:kafka-clients:4.0.0") {
        // exclude logback
    }

    annotationProcessor("io.javalin.community.openapi:openapi-annotation-processor:6.6.0")

    /**
     * Testcontainers - Create docker containers for needed dependencies during test runtime
     */
    testImplementation("org.testcontainers:testcontainers:1.21.0")
    testImplementation("io.javalin:javalin-testtools:6.6.0")

    /**
     * MockK - Mocking library for Kotlin
     */
    testImplementation("io.mockk:mockk:1.13.16")
}

application {
    mainClass.set("org.readutf.matchmaker.ApplicationKt")
}

tasks.test {
    useJUnit()
}
kotlin {
    jvmToolchain(23)
}
