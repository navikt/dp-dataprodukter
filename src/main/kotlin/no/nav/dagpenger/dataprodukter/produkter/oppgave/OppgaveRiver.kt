package no.nav.dagpenger.dataprodukter.produkter.oppgave

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDateTime
import java.util.UUID
import no.nav.dagpenger.dataprodukt.oppgave.Behandling
import no.nav.dagpenger.dataprodukt.oppgave.Oppgave
import no.nav.dagpenger.dataprodukt.oppgave.OppgaveTilstand
import no.nav.dagpenger.dataprodukt.oppgave.UtloestAv
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.objectMapper

internal class OppgaveRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Oppgave>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("oppgave_til_statistikk")) }
                validate {
                    it.requireKey(
                        "@id",
                        "@opprettet",
                        "system_participating_services",
                    )
                    it.requireKey(
                        "oppgave",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val mapper = objectMapper.readerFor(OppgaveDTO::class.java)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingIdAsText = packet["oppgave"]["behandling"]["behandlingId"].asText()
        withLoggingContext("behandlingId" to behandlingIdAsText) {
            val oppgaveDTO = mapper.readValue<OppgaveDTO>(packet["oppgave"].toString())
            Oppgave
                .newBuilder()
                .apply {
                    sakId = oppgaveDTO.sakId
                    oppgaveId = oppgaveDTO.oppgaveId
                    behandling = oppgaveDTO.behandling.let {
                        Behandling(
                            it.behandlingId,
                            it.tidspunkt.asTimestamp(),
                            it.basertPåBehandlingId,
                            UtloestAv(
                                it.utløstAv.type,
                                it.utløstAv.tidspunkt.asTimestamp()
                            )
                        )
                    }
                    personIdent = oppgaveDTO.personIdent
                    saksbehandlerIdent = oppgaveDTO.saksbehandlerIdent
                    beslutterIdent = oppgaveDTO.beslutterIdent
                    oppgaveTilstander = oppgaveDTO.oppgaveTilstander.map { oppgaveTilstand ->
                        OppgaveTilstand(
                            oppgaveTilstand.tilstand,
                            oppgaveTilstand.tidspunkt.asTimestamp(),
                        )
                    }
                    versjon = oppgaveDTO.versjon
                    avsluttetTidspunkt = oppgaveDTO.avsluttetTidspunkt.asTimestamp()
                }.build()
                .also { oppgave ->
                    logger.info{ "Publiserer oppgave til statistikk for behandlingId $behandlingIdAsText"}
                    dataTopic.publiser(
                        ident = oppgave.personIdent,
                        innlop = oppgave,
                    )
                }
        }

    }
}

data class OppgaveDTO(
    @param:JsonProperty("sakId")
    @get:JsonProperty("sakId")
    val sakId: UUID,
    @param:JsonProperty("oppgaveId")
    @get:JsonProperty("oppgaveId")
    val oppgaveId: UUID,
    @param:JsonProperty("behandling")
    @get:JsonProperty("behandling")
    val behandling: BehandlingDTO,
    @param:JsonProperty("personIdent")
    @get:JsonProperty("personIdent")
    val personIdent: String,
    @param:JsonProperty("saksbehandlerIdent")
    @get:JsonProperty("saksbehandlerIdent")
    val saksbehandlerIdent: String?,
    @param:JsonProperty("beslutterIdent")
    @get:JsonProperty("beslutterIdent")
    val beslutterIdent: String?,
    @param:JsonProperty("oppgaveTilstander")
    @get:JsonProperty("oppgaveTilstander")
    val oppgaveTilstander: List<OppgaveTilstandDTO>,
    @param:JsonProperty("versjon")
    @get:JsonProperty("versjon")
    val versjon: String,
    @param:JsonProperty("avsluttetTidspunkt")
    @get:JsonProperty("avsluttetTidspunkt")
    val avsluttetTidspunkt: LocalDateTime,
)

data class BehandlingDTO(
    @param:JsonProperty("behandlingId")
    @get:JsonProperty("behandlingId")
    val behandlingId: UUID,
    @param:JsonProperty("tidspunkt")
    @get:JsonProperty("tidspunkt")
    val tidspunkt: LocalDateTime,
    @param:JsonProperty("basertPåBehandlingId")
    @get:JsonProperty("basertPåBehandlingId")
    val basertPåBehandlingId: UUID?,
    @param:JsonProperty("utløstAv")
    @get:JsonProperty("utløstAv")
    val utløstAv: UtløstAvDTO,
)

data class UtløstAvDTO(
    @param:JsonProperty("type")
    @get:JsonProperty("type")
    val type: String,
    @param:JsonProperty("tidspunkt")
    @get:JsonProperty("tidspunkt")
    val tidspunkt: LocalDateTime,
)

data class OppgaveTilstandDTO(
    @param:JsonProperty("tilstand")
    @get:JsonProperty("tilstand")
    val tilstand: String,
    @param:JsonProperty("tidspunkt")
    @get:JsonProperty("tidspunkt")
    val tidspunkt: LocalDateTime,
)