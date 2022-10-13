package no.nav.dagpenger.data.innlop

import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class DataTopic<T : SpecificRecord>(
    private val producer: KafkaProducer<String, T>
) {
    private val kafka_produkt_topic by stringType

    fun publiser(innlop: T) {
        producer.send(ProducerRecord(config[kafka_produkt_topic], innlop))
    }
}
