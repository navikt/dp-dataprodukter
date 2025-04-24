import com.expediagroup.graphql.plugin.gradle.graphql

plugins {
    id("common")
    `java-library`
    id("com.expediagroup.graphql") version "8.6.2"
}

dependencies {
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.client)

    implementation("com.expediagroup", "graphql-kotlin-spring-client", "8.2.1")
    implementation("com.nimbusds:oauth2-oidc-sdk:11.20.1")

    testImplementation("no.nav.security:mock-oauth2-server:2.1.10")
    testImplementation(libs.mockk) {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("org.wiremock:wiremock:3.9.2")
    testImplementation(libs.bundles.jackson)
}

graphql {
    client {
        sdlEndpoint = "https://navikt.github.io/pdl/pdl-api-sdl.graphqls"
        packageName = "no.nav.pdl"
    }
}
