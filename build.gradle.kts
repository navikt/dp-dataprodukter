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
    implementation("org.apache.avro:avro:1.12.2")
    implementation(libs.kotlin.logging)
    implementation(libs.konfig)
    implementation("io.confluent:kafka-avro-serializer:8.3.1")

    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("no.nav.dagpenger.dataprodukter.MainKt")
}

// Avro 1.12 nekter å serialisere generert Avro-kode med mindre pakken er eksplisitt tiltrodd.
// Se Dockerfile for tilsvarende oppsett i prod, og AvroSerializationTest for regresjonstest.
tasks.test {
    jvmArgs("-Dorg.apache.avro.SERIALIZABLE_PACKAGES=no.nav.dagpenger.dataprodukt")
}
