plugins {
    id("common")
    application
}

repositories {
    maven("https://packages.confluent.io/maven")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(project(":behandling"))
    implementation(project(":person"))
    implementation(project(":soknad"))
    implementation(project(":avro-schemas"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.kotlin.logging)
    implementation(libs.konfig)
    implementation("io.confluent:kafka-avro-serializer:8.3.2")

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
