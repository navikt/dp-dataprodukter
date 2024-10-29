package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.soknad.SoknadTilstand
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic

internal class SøknadTilstandRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<SoknadTilstand>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
                validate {
                    it.requireKey(
                        "søknad_uuid",
                        "@opprettet",
                        "forrigeTilstand",
                        "gjeldendeTilstand",
                    )
                }
                validate { it.interestedIn("prosessnavn") }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadTilstandRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        val forrigeTilstand = packet["forrigeTilstand"].asText()
        val gjeldendeTilstand = packet["gjeldendeTilstand"].asText()

        withLoggingContext("søknadId" to søknadId.toString()) {
            SoknadTilstand
                .newBuilder()
                .apply {
                    this.soknadId = søknadId
                    this.tidsstempel = opprettet.asTimestamp()
                    this.forrigeTilstand = forrigeTilstand
                    this.gjeldendeTilstand = gjeldendeTilstand
                    packet["prosessnavn"].takeUnless { it.isMissingOrNull() }?.let { this.type = it.asText() }
                }.build()
                .also { data ->
                    logger.info { "Publiserer rad for ${data::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${data::class.java.simpleName}: $data " }
                    dataTopic.publiser(data)
                }
        }
    }
}
