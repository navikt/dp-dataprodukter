package no.nav.dagpenger.dataprodukter.produkter.innlop

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.innlop.Soknadsinnlop
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic

internal class SoknadsinnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Soknadsinnlop>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "innsending_ferdigstilt") }
                validate {
                    it.requireAny(
                        "type",
                        listOf(
                            "NySøknad",
                            "Gjenopptak",
                            "Utdanning",
                            "Etablering",
                            "Klage",
                            "Anke",
                        ),
                    )
                }
                validate {
                    it.interestedIn(
                        "@id",
                        "@opprettet",
                        "datoRegistrert",
                        "fødselsnummer",
                        "journalpostId",
                        "skjemaKode",
                        "tittel",
                        "fagsakId",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SoknadsinnlopRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val ident = packet["fødselsnummer"].asText()

        if (ident.isNullOrEmpty()) {
            logger.error { "Mottok søknad uten ident. Se sikkerlogg for detaljer." }
            sikkerlogg.error { "Mottok søknad uten ident. ${packet.toJson()}" }
            return
        }

        withLoggingContext(
            "journalpostId" to journalpostId,
            "dataprodukt" to dataTopic.topic,
        ) {
            Soknadsinnlop
                .newBuilder()
                .apply {
                    id = packet["@id"].asUUID()
                    opprettetDato = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    registrertDato = packet["datoRegistrert"].asLocalDateTime().asTimestamp()
                    this.journalpostId = journalpostId
                    skjemaKode = packet["skjemaKode"].asText()
                    tittel = packet["tittel"].asText()
                    type = packet["type"].asText()
                    fagsakId = packet["fagsakId"].asText()
                }.build()
                .also { innlop ->
                    logger.info { "Publiserer rad for ${innlop::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${innlop::class.java.simpleName}: $innlop " }

                    dataTopic.publiser(ident, innlop)
                }
        }
    }
}
