import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.graphql

plugins {
    id("common")
    `java-library`
    id("com.expediagroup.graphql") version "8.8.1"
    kotlin("plugin.serialization") version "2.3.0"
}

dependencies {
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")

    implementation("com.expediagroup", "graphql-kotlin-ktor-client", "8.2.1")
    implementation("com.nimbusds:oauth2-oidc-sdk:11.29.2")

    testImplementation("no.nav.security:mock-oauth2-server:2.3.0")
    testImplementation(libs.mockk) {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("org.wiremock:wiremock:3.13.1")
    testImplementation(libs.bundles.jackson)
}

graphql {
    client {
        sdlEndpoint = "https://navikt.github.io/pdl/pdl-api-sdl.graphqls"
        packageName = "no.nav.pdl"
        serializer = GraphQLSerializer.KOTLINX
    }
}
