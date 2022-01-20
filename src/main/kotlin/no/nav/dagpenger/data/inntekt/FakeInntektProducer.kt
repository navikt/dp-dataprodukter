package no.nav.dagpenger.data.inntekt

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDate
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger { }

internal class FakeInntektProducer(
    rapidsConnection: RapidsConnection,
    private val dagpengegrunnlagProducer: KafkaProducer<String, Dagpengegrunnlag>,
) : RapidsConnection.StatusListener {
    private val fake: Timer = fixedRateTimer(name = "fake-inntekt-producer", period = 5000) {
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

    init {
        rapidsConnection.register(this)
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        fake.cancel()
    }
}
