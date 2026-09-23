plugins {
    id("common")
    `java-library`
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
}

dependencies {
    api("org.apache.avro:avro:1.12.2")
}
