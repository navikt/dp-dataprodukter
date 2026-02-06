package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsverdiDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UlidVerdiDTO
import no.nav.dagpenger.dataprodukt.behandling.BehandlerRolle
import no.nav.dagpenger.dataprodukt.behandling.BehandletAv
import no.nav.dagpenger.dataprodukt.behandling.BehandletHendelseIdentifikasjon
import no.nav.dagpenger.dataprodukt.behandling.Behandlingsresultat
import no.nav.dagpenger.dataprodukt.behandling.Kilde
import no.nav.dagpenger.dataprodukt.behandling.Opplysning
import no.nav.dagpenger.dataprodukt.behandling.OpplysningPeriode
import no.nav.dagpenger.dataprodukt.behandling.Opprinnelse
import no.nav.dagpenger.dataprodukt.behandling.Rettighetsperiode
import no.nav.dagpenger.dataprodukt.behandling.Utbetaling
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.objectMapper
import java.time.LocalDate

internal class BehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Behandlingsresultat>,
    private val datoViEierAvslag: LocalDate,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("behandlingsresultat", "behandling_datalast")) }
                validate {
                    it.requireKey(
                        "@id",
                        "@opprettet",
                        "system_participating_services",
                    )
                    it.requireKey(
                        "behandlingId",
                        "ident",
                        "opplysninger",
                        "førteTil",
                    )
                    it.require("rettighetsperioder") { perioder ->
                        // Kast alle behandlinger som mangler rettighetsperioder
                        // De er for gamle eller ufullstendige uten rettighetsperioder
                        require(perioder.isArray && perioder.size() > 0) { "Det må være minst en rettighetsperiode" }
                    }
                    it.require("opprettet") { dato ->
                        val førteTil = it["førteTil"].asText()
                        if (førteTil != "Avslag") {
                            return@require "Er ikke avslag"
                        }
                        val perioder = it["rettighetsperioder"]
                        if (perioder.isArray && perioder.size() > 1) {
                            return@require "Er avslag på gjenopptakelse"
                        }

                        val opprettet = dato.asLocalDateTime().toLocalDate()

                        // Kast alle avslag fram til en bestemt dato når vi også eier avslagene selv
                        require(opprettet.isAfter(datoViEierAvslag)) { "Avslag før $datoViEierAvslag eies av Arena" }
                    }
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.BehandlingRiver")
        private val mapper = objectMapper.readerFor(BehandlingsresultatDTO::class.java)
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
            val pakke = BehandlingsresultatParser(packet)
            val behandling = mapper.readValue<BehandlingsresultatDTO>(packet.toJson())

            Behandlingsresultat
                .newBuilder()
                .apply {
                    behandlingId = behandling.behandlingId
                    fagsakId = pakke.saksnummer
                    ident = behandling.ident
                    basertPaa = behandling.basertPå
                    behandlingskjedeId = behandling.behandlingskjedeId
                    behandletHendelse =
                        behandling.behandletHendelse.let {
                            BehandletHendelseIdentifikasjon(
                                it.type.name,
                                it.id,
                                it.skjedde,
                            )
                        }
                    this.foerteTil = behandling.førteTil.value
                    rettighetsperioder =
                        behandling.rettighetsperioder.map {
                            Rettighetsperiode(
                                it.fraOgMed,
                                it.tilOgMed,
                                it.harRett,
                                it.opprinnelse?.name,
                            )
                        }
                    automatisk = behandling.automatisk
                    opplysninger =
                        behandling.opplysninger.map { opplysning ->
                            Opplysning(
                                opplysning.opplysningTypeId,
                                opplysning.navn,
                                opplysning.datatype.name,
                                opplysning.perioder.map { periode ->
                                    OpplysningPeriode(
                                        periode.opprettet.asTimestamp(),
                                        periode.opprinnelse!!.let {
                                            Opprinnelse.valueOf(it.value)
                                        },
                                        periode.gyldigFraOgMed,
                                        periode.gyldigTilOgMed,
                                        periode.verdi.verdi().toString(),
                                        periode.kilde?.let {
                                            Kilde.valueOf(it.type.value)
                                        },
                                    )
                                },
                            )
                        }
                    utbetalinger =
                        behandling.utbetalinger.map {
                            Utbetaling(it.meldeperiode, it.dato, it.sats, it.utbetaling)
                        }
                    behandletAv =
                        behandling.behandletAv.map {
                            // TODO: Fiks at behandler ikke er nullable i API
                            BehandletAv(BehandlerRolle.valueOf(it.rolle.value), it.behandler!!.ident)
                        }
                    opprettetTid = behandling.opprettet.asTimestamp()
                    sistEndretTid = behandling.sistEndret.asTimestamp()
                    meldingsreferanseId = pakke.meldingsreferanseId
                    versjon = pakke.image
                }.build()
                .also { behandling ->
                    logger.info { "Publiserer rad for ${behandling::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${behandling::class.java.simpleName}: $behandling " }

                    dataTopic.publiser(packet["ident"].asText(), behandling)
                }
        }
    }
}

private fun OpplysningsverdiDTO.verdi() =
    when (this) {
        is BarnelisteDTO -> verdi
        is BoolskVerdiDTO -> verdi
        is DatoVerdiDTO -> verdi
        is DesimaltallVerdiDTO -> verdi
        is HeltallVerdiDTO -> verdi
        is PengeVerdiDTO -> verdi
        is PeriodeVerdiDTO -> fom..tom
        is TekstVerdiDTO -> verdi
        is UlidVerdiDTO -> verdi
    }

class BehandlingsresultatParser(
    private val packet: JsonMessage,
) {
    val fagsakId: JsonNode? get() = packet["opplysninger"].singleOrNull { it["navn"].asText() == "fagsakId" }

    val saksnummer: String get() = fagsakId?.let { it["perioder"].single()["verdi"]["verdi"].asText() } ?: "0"
    val image: String get() = packet["system_participating_services"].first()["image"]?.asText() ?: ""
    val meldingsreferanseId get() = packet["@id"].asUUID()
}
