plugins {
    id("common")
    application
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
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
    implementation("io.confluent:kafka-avro-serializer:7.8.0")

    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
}

application {
    mainClass.set("no.nav.dagpenger.dataprodukter.MainKt")
}
