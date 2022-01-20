package no.nav.dagpenger.data.inntekt

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

private val logger = KotlinLogging.logger { }

internal class InntektRiver(
    rapidsConnection: RapidsConnection,
    private val dagpengegrunnlagProducer: KafkaProducer<String, Dagpengegrunnlag>,
    private val grunnbeløp: Grunnbeløp
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "faktum_svar") }
            validate { it.requireAll("@behov", listOf("InntektSiste12Mnd", "InntektSiste3År")) }
            validate { it.requireKey("@løsning", "Virkningstidspunkt") }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logger.info { "Problems: $problems" }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        logger.info { "FOLE GALE: $error" }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val grunnlag12mnd = packet["@løsning"]["InntektSiste12Mnd"].asDouble()
        val grunnlag36mnd = packet["@løsning"]["InntektSiste3År"].asDouble()
        val virkingstidspunkt = packet["Virkningstidspunkt"].asLocalDate()

        Dagpengegrunnlag.newBuilder().apply {
            beregningsdato = virkingstidspunkt
            gjeldendeGrunnbeloep = grunnbeløp.gjeldendeGrunnbeløp(virkingstidspunkt)
            grunnlag = listOf(
                Grunnlag(Grunnlagsperiode.Siste12mnd, grunnlag12mnd),
                Grunnlag(Grunnlagsperiode.Siste36mnd, grunnlag36mnd)
            )
            kontekst = Kontekst.Automatisering
        }.build().also { grunnlag ->
            logger.info { "Sender ut $grunnlag" }

            dagpengegrunnlagProducer.send(ProducerRecord("teamdagpenger.dp-data-inntekt", grunnlag))
        }
    }
}
