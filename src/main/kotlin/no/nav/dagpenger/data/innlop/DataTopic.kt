package no.nav.dagpenger.data.innlop

import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType
import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

internal class DataTopic<T : SpecificRecord>(
    private val producer: KafkaProducer<String, T>
) {
    private val kafka_produkt_topic by stringType

    companion object {
        val logger = KotlinLogging.logger {}
    }

    fun publiser(innlop: T) {
        logger.info { "Skal publisere $innlop" }
        producer.send(ProducerRecord(config[kafka_produkt_topic], innlop)) { metadata: RecordMetadata?, exception ->
            if (exception != null) {
                logger.error(exception) { "Kunne ikke produsere ${kafka_produkt_topic.name}. Feilmelding: ${exception.message}" }
                throw exception
            }
            if (metadata != null) {
                logger.info {
                    "produserte topic $metadata"
                }
            }
        }
    }
}
