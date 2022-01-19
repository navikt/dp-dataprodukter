import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
}

group = "no.nav.dagpenger.data.inntekt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.github.navikt:rapids-and-rivers:20210617121814-3e67e4d")
    implementation("org.apache.avro:avro:1.11.0")
    implementation("io.confluent:kafka-avro-serializer:5.3.0")
    implementation("io.github.microutils:kotlin-logging:2.1.21")

    testImplementation("io.mockk:mockk:1.12.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}
