package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.data.innlop.SoknadTilstand
import no.nav.dagpenger.data.innlop.asUUID
import no.nav.dagpenger.data.innlop.avro.asTimestamp
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class SøknadTilstandRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<SoknadTilstand>
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
            validate {
                it.requireKey(
                    "søknad_uuid",
                    "@opprettet",
                    "forrigeTilstand",
                    "gjeldendeTilstand"
                )
            }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        val forrigeTilstand = packet["forrigeTilstand"].asText()
        val gjeldendeTilstand = packet["gjeldendeTilstand"].asText()

        withLoggingContext("søknadId" to søknadId.toString()) {
            SoknadTilstand.newBuilder().apply {
                this.soknadId = søknadId
                this.tidsstempel = opprettet.asTimestamp()
                this.forrigeTilstand = forrigeTilstand
                this.gjeldendeTilstand = gjeldendeTilstand
            }.build().also { data ->
                logger.info { "Sender ut $data" }
                dataTopic.publiser(data)
            }
        }
    }
}
