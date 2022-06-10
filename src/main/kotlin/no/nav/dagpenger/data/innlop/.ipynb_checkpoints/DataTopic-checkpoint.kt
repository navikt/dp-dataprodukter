package no.nav.dagpenger.data.innlop

import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class DataTopic(
    private val producer: KafkaProducer<String, Soknadsinnlop>,
) {
    private val kafka_produkt_topic by stringType

    fun publiser(innlop: Soknadsinnlop) {
        producer.send(ProducerRecord(config[kafka_produkt_topic], innlop))
    }
}
