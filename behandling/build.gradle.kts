plugins {
    id("common")
    `java-library`
    id("org.openapi.generator") version "7.13.0"
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

tasks {
    compileKotlin {
        dependsOn("openApiGenerate")
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    remoteInputSpec.set(
        "https://raw.githubusercontent.com/navikt/dp-behandling/refs/heads/main/openapi/src/main/resources/behandling-api.yaml?1",
    )
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.dagpenger.behandling.api")
    globalProperties.set(mapOf("models" to ""))
    modelNameSuffix.set("DTO")
    typeMappings = mapOf("DateTime" to "LocalDateTime")
    importMappings = mapOf("LocalDateTime" to "java.time.LocalDateTime")
    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "dateLibrary" to "custom",
            "enumPropertyNaming" to "original",
        ),
    )
}
