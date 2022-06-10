package no.nav.dagpenger.data.innlop

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

internal class InnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        Soknadsinnlop.newBuilder().apply {
            id = id
            opprettetDato = LocalDate.now()
            registrertDato = LocalDate.now()
            journalpostId = journalpostId
            skjemaKode = skjemaKode
            tittel = tittel
            type = type
            fagsakId = fagsakId
        }.build().also { grunnlag ->
            logger.info { "Sender ut $grunnlag" }

            dataTopic.publiser(grunnlag)
        }
    }
}
