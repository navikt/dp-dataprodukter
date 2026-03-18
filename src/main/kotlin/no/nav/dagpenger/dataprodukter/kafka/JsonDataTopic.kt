package no.nav.dagpenger.dataprodukter.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.common.errors.InvalidConfigurationException
import org.apache.kafka.common.errors.InvalidTopicException
import org.apache.kafka.common.errors.RecordBatchTooLargeException
import org.apache.kafka.common.errors.RecordTooLargeException
import org.apache.kafka.common.errors.UnknownServerException
import java.util.Properties

internal class JsonDataTopic(
    private val producer: KafkaProducer<String, String>,
    internal val topic: String,
) {
    companion object {
        private val logger = KotlinLogging.logger {}

        fun jsonDataTopic(topic: String) =
            JsonDataTopic(createJsonProducer(AivenConfig.default.jsonProducerConfig()), topic)

        private fun isFatalError(err: Exception) =
            when (err) {
                is InvalidConfigurationException,
                is InvalidTopicException,
                is RecordBatchTooLargeException,
                is RecordTooLargeException,
                is UnknownServerException,
                is AuthorizationException,
                -> true

                else -> false
            }
    }

    fun publiser(
        ident: String,
        json: String,
    ) {
        producer.send(ProducerRecord(topic, ident, json)) { _, err ->
            if (err == null || !isFatalError(err)) return@send
            logger.error(err) { "Shutting down rapid due to fatal error: ${err.message}" }
            producer.flush()
            throw err
        }
    }
}

private fun createJsonProducer(producerConfig: Properties = Properties()) =
    KafkaProducer<String, String>(producerConfig).also { producer ->
        Runtime.getRuntime().addShutdownHook(
            Thread {
                producer.flush()
                producer.close()
            },
        )
    }


