package no.nav.dagpenger.data.inntekt

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.netty.util.internal.ThreadExecutorMap.apply
import mu.KotlinLogging
import no.nav.dagpenger.data.inntekt.grunnbeløp.GGrunnbeløp
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.time.LocalDate
import java.util.Properties
import kotlin.concurrent.fixedRateTimer

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
private val logger = KotlinLogging.logger { }

fun main() {
    val env = System.getenv()
    val dagpengegrunnlagProducer by lazy {
        createProducer<String, Dagpengegrunnlag>(aivenKafka.producerConfig(avroProducerConfig))
    }
    val fake = fixedRateTimer(name = "foo", period = 5000) {
        Dagpengegrunnlag.newBuilder().apply {
            beregningsdato = LocalDate.now()
            gjeldendeGrunnbeloep = 123.0
            grunnlag = listOf(
                Grunnlag(Grunnlagsperiode.Siste12mnd, 345000.0),
                Grunnlag(Grunnlagsperiode.Siste36mnd, 656000.0)
            )
            kontekst = Kontekst.Automatisering
        }.build().also { grunnlag ->
            logger.info { "Sender ut $grunnlag" }

            dagpengegrunnlagProducer.send(ProducerRecord("teamdagpenger.data-inntekt-v1", grunnlag))
        }
    }

    RapidApplication.create(env) { _, rapidsConnection ->
        rapidsConnection.seekToBeginning()
        rapidsConnection.register(object : RapidsConnection.StatusListener {
            override fun onShutdown(rapidsConnection: RapidsConnection) {
                fake.cancel()
            }
        })
        InntektRiver(rapidsConnection, dagpengegrunnlagProducer, GGrunnbeløp(timeToLive = Duration.ofHours(4)))
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
