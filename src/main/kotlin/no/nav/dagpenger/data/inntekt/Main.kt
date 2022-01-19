package no.nav.dagpenger.data.inntekt

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.dagpenger.data.inntekt.grunnbeløp.GGrunnbeløp
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

private val aivenKafka: AivenConfig = AivenConfig.default
private val avroProducerConfig = Properties().apply {
    put(
        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY")) { "Expected KAFKA_SCHEMA_REGISTRY" }
    )
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class)
}

fun main() {
    val env = System.getenv()
    val dagpengegrunnlagProducer =
        createProducer<String, Dagpengegrunnlag>(aivenKafka.producerConfig(avroProducerConfig))

    RapidApplication.create(env).apply {
        InntektRiver(this, dagpengegrunnlagProducer, GGrunnbeløp())
    }.start()
}

private fun <K, V> createProducer(producerConfig: Properties = Properties()) =
    KafkaProducer<K, V>(producerConfig).also {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                it.close()
            }
        )
    }
