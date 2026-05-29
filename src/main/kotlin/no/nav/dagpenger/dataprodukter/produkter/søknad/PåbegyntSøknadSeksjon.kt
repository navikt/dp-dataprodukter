package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.soknad.OrkestratorSeksjon
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.PersonRepository

internal class PåbegyntSøknadSeksjon(
    rapidConnection: RapidsConnection,
    private val seksjonDataTopic: DataTopic<OrkestratorSeksjon>,
    private val personRepository: PersonRepository
) : River.PacketListener {
    init {
        River(rapidConnection)
            .apply {
                precondition { it.requireValue("@event_name", "paabegynt_soknad_seksjon") }
                validate { it.requireKey("søknad_uuid", "ident", "seksjon_id", "opprettet", "oppdatert") }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.PåbegyntSøknadSeksjon")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val ident = packet["ident"].asText()
        val person = personRepository.hentPersonMedKode6Og7BeskyttelseInfo(ident)
        if (person.harAdressebeskyttelse) return

        val søknadId = packet["søknad_uuid"].asUUID()
        val seksjonId = packet["seksjon_id"].asText()
        val opprettet = packet["opprettet"].asLocalDateTime().asTimestamp()
        val oppdatert = packet["oppdatert"].asLocalDateTime().asTimestamp()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
            "dataprodukt" to "orkestrator-søknadsdata",
        ) {
            logger.info { "Mottok paabegynt_soknad_seksjon for påbegynt søknad $søknadId" }

            OrkestratorSeksjon.newBuilder().apply {
                this.seksjonId = seksjonId
                this.soknadId = søknadId
                this.opprettet = opprettet
                this.oppdatert = oppdatert
                this.versjon = "1"
                this.seksjonsvar = emptyMap()
            }.build().also { data ->
                seksjonDataTopic.publiser(
                    søknadId.toString(), data
                )
                logger.info { "Publiserte seksjon for påbegynt søknad $søknadId til seksjonDataTopic" }
                sikkerlogg.info { logger.info { "Publiserte seksjon for påbegynt søknad $søknadId til seksjonDataTopic: $data" } }
            }
        }

    }

}