import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.impldep.com.amazonaws.util.json.Jackson
import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    id("common")
    `java-library`
    id("ch.acanda.gradle.fabrikt") version "1.16.1"
    id("de.undercouch.download") version "5.6.0"
}

dependencies {
    implementation(libs.bundles.jackson)
}

sourceSets {
    main {
        kotlin {
            srcDir("build/generated/src/main/kotlin")
        }
    }
}

val apiSpecFile = layout.buildDirectory.file("tmp/behandling-api.yaml")

val hentOpenAPI by tasks.register<Download>("hentOpenAPI") {
    src("https://raw.githubusercontent.com/navikt/dp-behandling/refs/heads/main/openapi/src/main/resources/behandling-api.yaml")
    dest(apiSpecFile)
    overwrite(true)
    group = "openapi"
    description = "Henter OpenAPI spesifikasjonen fra github og lagrer den lokalt"
}

tasks {
    compileKotlin {
        dependsOn("fabriktGenerate")
    }
    fabriktGenerate {
        dependsOn(hentOpenAPI)
    }
}

fabrikt {
    generate("behandling") {
        apiFile = apiSpecFile
        basePackage = "no.nav.dagpenger.behandling.api"
        skip = false
        quarkusReflectionConfig = disabled
        typeOverrides {
            datetime = LocalDateTime
        }
        model {
            generate = enabled
            validationLibrary = NoValidation
            extensibleEnums = disabled
            sealedInterfacesForOneOf = enabled
            ignoreUnknownProperties = disabled
            nonNullMapValues = enabled
            serializationLibrary = Jackson
            suffix = "DTO"
        }
    }
}
