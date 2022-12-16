package no.nav.dagpenger.data.innlop

import mu.KotlinLogging
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.common.errors.InvalidConfigurationException
import org.apache.kafka.common.errors.InvalidTopicException
import org.apache.kafka.common.errors.RecordBatchTooLargeException
import org.apache.kafka.common.errors.RecordTooLargeException
import org.apache.kafka.common.errors.UnknownServerException

internal class DataTopic<T : SpecificRecord>(
    private val producer: KafkaProducer<String, T>,
    private val topic: String
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private fun isFatalError(err: Exception) = when (err) {
            is InvalidConfigurationException,
            is InvalidTopicException,
            is RecordBatchTooLargeException,
            is RecordTooLargeException,
            is UnknownServerException,
            is AuthorizationException -> true

            else -> false
        }
    }

    fun publiser(innlop: T) {
        producer.send(ProducerRecord(topic, innlop)) { _, err ->
            if (err == null || !isFatalError(err)) return@send
            logger.error(err) { "Shutting down rapid due to fatal error: ${err.message}" }
            producer.flush()
            throw err
        }
    }
}
