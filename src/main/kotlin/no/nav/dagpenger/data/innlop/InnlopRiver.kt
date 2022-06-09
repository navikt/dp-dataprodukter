package no.nav.dagpenger.data.innlop

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

internal class InnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "faktum_svar") }
            validate { it.requireAll("@behov", listOf("InntektSiste12Mnd", "InntektSiste3År")) }
            validate { it.requireKey("@løsning", "Virkningstidspunkt") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val grunnlag12mnd = packet["@løsning"]["InntektSiste12Mnd"].asDouble()
        val grunnlag36mnd = packet["@løsning"]["InntektSiste3År"].asDouble()
        val virkingstidspunkt = packet["Virkningstidspunkt"].asLocalDate()
        val beregningsId = UUID.randomUUID().toString()

        mapOf(
            Grunnlagsperiode.Siste12mnd to grunnlag12mnd,
            Grunnlagsperiode.Siste36mnd to grunnlag36mnd
        ).forEach { (grunnlagsperiode, beregnetGrunnlag) ->
            Dagpengegrunnlag.newBuilder().apply {
                beregningsdato = virkingstidspunkt
                id = beregningsId
                type = grunnlagsperiode
                verdi = beregnetGrunnlag
                kontekst = Kontekst.Automatisering
            }.build().also { grunnlag ->
                logger.info { "Sender ut $grunnlag" }

                dataTopic.publiser(grunnlag)
            }
        }
    }
}
