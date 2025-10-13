plugins {
    id("common")
    application
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
}

repositories {
    maven("https://packages.confluent.io/maven")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(project(":behandling"))
    implementation(project(":person"))
    implementation(project(":soknad"))

    implementation(libs.rapids.and.rivers)
    implementation("org.apache.avro:avro:1.12.0")
    implementation(libs.kotlin.logging)
    implementation(libs.konfig)
    implementation("io.confluent:kafka-avro-serializer:8.0.2")

    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("no.nav.dagpenger.dataprodukter.MainKt")
}
