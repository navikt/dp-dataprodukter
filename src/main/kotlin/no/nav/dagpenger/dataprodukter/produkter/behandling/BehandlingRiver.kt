package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.behandling.Behandling
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import java.time.LocalDateTime

internal class BehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Behandling>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("forslag_til_vedtak", "vedtak_fattet")) }
                validate {
                    it.requireKey(
                        "@id",
                        "@opprettet",
                        "system_participating_services",
                    )
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "automatisk",
                        "virkningsdato",
                        "vedtakstidspunkt",
                        "fastsatt",
                        "opplysninger",
                    )
                    it.interestedIn(
                        "behandletAv",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.VedtakFattetRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withLoggingContext(
            "behandlingId" to packet["behandlingId"].asText(),
            "dataprodukt" to dataTopic.topic,
        ) {
            val image = packet["system_participating_services"].first()["image"]?.asText() ?: ""
            val status = packet["@event_name"].asText()
            val ident = packet["ident"].asText()

            if (!packet.harBehandletAv && !packet["automatisk"].asBoolean()) {
                sikkerlogg.warn { "Behandlingen er ikke automatisk, men mangler behandlet av: ${packet.toJson()}" }
            }

            Behandling
                .newBuilder()
                .apply {
                    sekvensnummer = System.currentTimeMillis()
                    meldingsreferanseId = packet["@id"].asUUID()
                    behandlingId = packet["behandlingId"].asUUID()
                    tekniskTid = LocalDateTime.now().asTimestamp()
                    endretTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    this.ident = ident
                    saksnummer = packet.saksnummer
                    behandlingType = "søknad"
                    behandlingStatus = status
                    mottattTid = packet.registrert.asTimestamp()
                    registrertTid = packet.registrert.asTimestamp()
                    ferdigBehandletTid = packet["vedtakstidspunkt"].asLocalDateTime().asTimestamp()
                    virkningsdato = packet["virkningsdato"].asLocalDate()
                    resultat = packet["fastsatt"]["utfall"].asBoolean()
                    automatisk = packet["automatisk"].asBoolean()
                    if (status == "vedtak_fattet" && packet.harBehandletAv) {
                        val behandlere = packet.behandletAv
                        saksbehandler = behandlere.saksbehandler
                        beslutter = behandlere.beslutter
                    }
                    avsender = "digidag"
                    versjon = image
                }.build()
                .also { behandling ->
                    logger.info { "Publiserer rad for ${behandling::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${behandling::class.java.simpleName}: $behandling " }

                    dataTopic.publiser(ident, behandling)
                }
        }
    }
}

val JsonMessage.registrert: LocalDateTime get() = fagsakId?.let { it["kilde"]["registrert"] }?.asLocalDateTime() ?: LocalDateTime.now()
val JsonMessage.saksnummer: String get() = fagsakId?.let { it["verdi"].asText() } ?: "0"
val JsonMessage.fagsakId: JsonNode? get() = this["opplysninger"].singleOrNull { it["navn"].asText() == "fagsakId" }

val JsonMessage.harBehandletAv: Boolean get() = !this["behandletAv"].isMissingOrNull()
val JsonMessage.behandletAv: BehandletAv
    get() =
        this["behandletAv"].let { behandletAv ->
            val beslutter = behandletAv.singleOrNull { it["rolle"].asText() == "beslutter" }
            val saksbehandler = behandletAv.singleOrNull { it["rolle"].asText() == "saksbehandler" }
            BehandletAv(
                beslutter = beslutter?.get("behandler")?.get("ident")?.asText(),
                saksbehandler = saksbehandler?.get("behandler")?.get("ident")?.asText(),
            )
        }

data class BehandletAv(
    val beslutter: String?,
    val saksbehandler: String?,
)
