package no.nav.dagpenger.dataprodukter.kafka

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.dagpenger.dataprodukt.soknad.OrkestratorSeksjon
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Regresjonstest for produksjonsfeil etter oppgradering til Avro 1.12.x / kafka-avro-serializer 8.x:
 *
 * Avro sin nye [org.apache.avro.util.ClassSecurityValidator] nekter å serialisere generert
 * Avro-kode med mindre pakken/klassen eksplisitt er tiltrodd, og kastet
 * `java.lang.SecurityException: Forbidden no.nav.dagpenger.dataprodukt.soknad.OrkestratorSeksjon!`
 * i produksjon selv om alle testene (som bruker mocket KafkaProducer) var grønne.
 *
 * Fikset ved å eksplisitt tiltro pakken `no.nav.dagpenger.dataprodukt` via systemegenskapen
 * `org.apache.avro.SERIALIZABLE_PACKAGES` – satt som JVM-argument i Dockerfile (prod) og i
 * `tasks.test` i build.gradle.kts (denne testen). Denne testen bruker en ekte
 * [KafkaAvroSerializer] (mot en [MockSchemaRegistryClient]) i stedet for en mocket
 * KafkaProducer, slik at den faktisk hadde feilet uten fiksen.
 */
internal class AvroSerializationTest {
    @Test
    fun `serialisering av generert avro-record feiler ikke naar pakken er tiltrodd`() {
        val serializer =
            KafkaAvroSerializer(MockSchemaRegistryClient()).apply {
                configure(mapOf("schema.registry.url" to "mock://avro-serialization-test"), false)
            }

        val seksjon =
            OrkestratorSeksjon
                .newBuilder()
                .apply {
                    soknadId = UUID.randomUUID()
                    seksjonId = "personalia"
                    opprettet = Instant.now()
                    oppdatert = Instant.now()
                    versjon = "1"
                    seksjonsvar = emptyMap()
                }.build()

        val bytes = serializer.serialize("data", seksjon)

        assertNotNull(bytes)
    }
}
