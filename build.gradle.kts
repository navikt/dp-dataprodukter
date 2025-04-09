plugins {
    id("common")
    application
    id("io.github.androa.gradle.plugin.avro") version "0.0.11"
}

repositories {
    maven("https://packages.confluent.io/maven")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(project(":soknad"))
    implementation(project(":person"))

    implementation(libs.rapids.and.rivers)
    implementation("org.apache.avro:avro:1.12.0")
    implementation(libs.kotlin.logging)
    implementation(libs.konfig)
    implementation("io.confluent:kafka-avro-serializer:7.9.0")

    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("no.nav.dagpenger.dataprodukter.MainKt")
}
