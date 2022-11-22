package no.nav.dagpenger.data.innlop

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.dagpenger.data.innlop.tjenester.SoknadsinnlopRiver
import no.nav.dagpenger.data.innlop.tjenester.UtlandRiver
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

private val aivenKafka: AivenConfig = AivenConfig.default
private val avroProducerConfig = Properties().apply {
    val schemaRegistryUser =
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY_USER")) { "Expected KAFKA_SCHEMA_REGISTRY_USER" }
    val schemaRegistryPassword =
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD")) { "Expected KAFKA_SCHEMA_REGISTRY_PASSWORD" }

    put(
        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY")) { "Expected KAFKA_SCHEMA_REGISTRY" }
    )
    put(KafkaAvroSerializerConfig.USER_INFO_CONFIG, "$schemaRegistryUser:$schemaRegistryPassword")
    put(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
}

fun main() {
    val env = System.getenv()
    val soknadsinnlopDataTopic by lazy {
        DataTopic(
            createProducer<String, Soknadsinnlop>(aivenKafka.producerConfig(avroProducerConfig)),
            config[kafka_produkt_topic]
        )
    }
    val identDataTopic by lazy {
        DataTopic(
            createProducer<String, Ident>(aivenKafka.producerConfig(avroProducerConfig)),
            config[kafka_produkt_ident_topic]
        )
    }
    val utlandDataTopic by lazy {
        DataTopic(
            createProducer<String, Utland>(aivenKafka.producerConfig(avroProducerConfig)),
            config[kafka_produkt_utland_topic]
        )
    }

    RapidApplication.create(env) { _, rapidsConnection ->
        SoknadsinnlopRiver(rapidsConnection, soknadsinnlopDataTopic, identDataTopic)
        UtlandRiver(rapidsConnection, utlandDataTopic)
    }.start()
}

private fun <K, V> createProducer(producerConfig: Properties = Properties()) =
    KafkaProducer<K, V>(producerConfig).also { producer ->
        Runtime.getRuntime().addShutdownHook(
            Thread {
                producer.close()
            }
        )
    }
