package no.nav.dagpenger.data.innlop

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class DataTopic<T : SpecificRecord>(
    private val producer: KafkaProducer<String, T>,
    private val topic: String
) {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    fun publiser(innlop: T) {
        producer.send(ProducerRecord(topic, innlop))
    }
}
