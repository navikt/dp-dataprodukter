import org.gradle.internal.impldep.com.amazonaws.util.json.Jackson
import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled
import java.net.URI

plugins {
    id("common")
    `java-library`
    id("ch.acanda.gradle.fabrikt") version "1.15.4"
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

val downloadApiSpec by tasks.registering {
    group = "openapi"
    description = "Henter OpenAPI spesifikasjonen fra github og lagrer den lokalt"
    outputs.file(apiSpecFile)
    doLast {
        val url =
            URI(
                "https://raw.githubusercontent.com/navikt/dp-behandling/refs/heads/main/openapi/src/main/resources/behandling-api.yaml",
            ).toURL()
        val file = apiSpecFile.get().asFile
        file.parentFile.mkdirs()
        url.openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

tasks {
    compileKotlin {
        dependsOn("fabriktGenerate")
    }
    fabriktGenerate {
        dependsOn(downloadApiSpec)
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
