package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.data.innlop.DataTopic
import no.nav.dagpenger.data.innlop.Utland
import no.nav.dagpenger.data.innlop.erEØS
import no.nav.dagpenger.data.innlop.søknad.SøknadData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class UtlandRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Utland>
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate {
                it.requireAny(
                    "type",
                    listOf(
                        "NySøknad",
                        "Gjenopptak"
                    )
                )
            }
            validate {
                it.interestedIn(
                    "journalpostId",
                    "søknadsData"
                )
            }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknad = SøknadData.lagMapper(packet["søknadsData"])
        Utland.newBuilder().apply {
            journalpostId = packet["journalpostId"].asText()
            bostedsland = søknad.bostedsland
            arbeidsforholdEos = søknad.arbeidsforholdLand.any { it.erEØS() }
            arbeidsforholdLand = søknad.arbeidsforholdLand.joinToString()
        }.build().also { data ->
            logger.info { "[DRY-RUN] Sender ut $data" }
            // dataTopic.publiser(data)
        }
    }
}
