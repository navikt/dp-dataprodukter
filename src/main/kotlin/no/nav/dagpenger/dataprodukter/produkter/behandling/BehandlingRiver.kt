package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.behandling.BehandletHendelseIdentifikasjon
import no.nav.dagpenger.dataprodukt.behandling.Behandlingsresultat
import no.nav.dagpenger.dataprodukt.behandling.Kvote
import no.nav.dagpenger.dataprodukt.behandling.Rettighetsperiode
import no.nav.dagpenger.dataprodukt.behandling.Rettighetstype
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic

internal class BehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Behandlingsresultat>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("behandlingsresultat")) }
                validate {
                    it.requireKey(
                        "@id",
                        "@opprettet",
                        "system_participating_services",
                    )
                    it.requireKey(
                        "behandlingId",
                        "ident",
                        "behandletHendelse",
                        "opplysninger",
                        "rettighetsperioder",
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
            val resultat = BehandlingsresultatParser(packet)

            Behandlingsresultat
                .newBuilder()
                .apply {
                    behandlingId = resultat.behandlingId
                    fagsakId = resultat.saksnummer
                    soknadId = resultat.søknadId
                    ident = resultat.ident
                    behandletHendelse = resultat.behandletHendelse
                    this.resultat = resultat.utfall().name
                    rettighet = resultat.rettighetstype
                    rettighetsperioder = resultat.rettighetsperioder
                    automatisk = resultat.erAutomatisk
                    vilkaar = listOf()
                    kvote = resultat.kvoter
                    opprettetTid = resultat.opprettetTid
                    sistEndretTid = resultat.sistEndretTid
                    meldingsreferanseId = resultat.meldingsreferanseId
                    versjon = resultat.image
                }.build()
                .also { behandling ->
                    logger.info { "Publiserer rad for ${behandling::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${behandling::class.java.simpleName}: $behandling " }

                    dataTopic.publiser(packet["ident"].asText(), behandling)
                }
        }
    }
}

class BehandlingsresultatParser(
    private val packet: JsonMessage,
) {
    val behandlingId get() = packet["behandlingId"].asUUID()
    val fagsakId: JsonNode? get() = packet["opplysninger"].singleOrNull { it["navn"].asText() == "fagsakId" }
    val søknadId: String?
        get() =
            packet["opplysninger"]
                .find { opplysning ->
                    opplysning["opplysningTypeId"].asText() == søknadTypeId
                }?.map { it["perioder"] }
                // Vi vil alltid bare ha den siste søknaden
                ?.lastOrNull()
                ?.let { sistePeriode ->
                    sistePeriode["verdi"]["verdi"].asText()
                }
    val ident: String get() = packet["ident"].asText()

    val behandletHendelse
        get() =
            BehandletHendelseIdentifikasjon(
                packet["behandletHendelse"]["type"].asText(),
                packet["behandletHendelse"]["id"].asText(),
            )
    val rettighetstype: Rettighetstype
        get() {
            return Rettighetstype().apply {
                ordinaer = rettighet("0194881f-9444-7a73-a458-0af81c034d85")
                permittert = rettighet("0194881f-9444-7a73-a458-0af81c034d86")
                loennsgaranti = rettighet("0194881f-9444-7a73-a458-0af81c034d87")
                fiskeforedling = rettighet("0194881f-9444-7a73-a458-0af81c034d88")
            }
        }

    val harRett get() = packet["rettighetsperioder"]

    val rettighetsperioder
        get() =
            packet["rettighetsperioder"].map {
                Rettighetsperiode(
                    it["fraOgMed"].asLocalDate(),
                    it["tilOgMed"]?.asOptionalLocalDate(),
                    it["harRett"].asBoolean(),
                    it["opprinnelse"].asText(),
                )
            }

    val kvoter: List<Kvote>
        get() {
            val kvoter =
                packet["opplysninger"]
                    .filter { it["opplysningTypeId"].asText() in kvoteOpplysninger }
            return kvoter.map {
                val sistePeriode = it["perioder"].last()
                val opprettet = sistePeriode["opprettet"].asLocalDateTime().asTimestamp()
                Kvote(
                    it["navn"].asText(),
                    sistePeriode["verdi"]["verdi"].asText(),
                    "dager",
                    opprettet,
                    opprettet,
                )
            }
        }

    val erAutomatisk: Boolean get() = true // packet["automatisk"].asBoolean()

    val saksnummer: String get() = fagsakId?.let { it["perioder"].single()["verdi"]["verdi"].asText() } ?: "0"
    val image: String get() = packet["system_participating_services"].first()["image"]?.asText() ?: ""
    val opprettetTid get() = packet["@opprettet"].asLocalDateTime().asTimestamp()

    val sistEndretTid get() = packet["@opprettet"].asLocalDateTime().asTimestamp()
    val meldingsreferanseId get() = packet["@id"].asUUID()

    fun utfall() = utfall(rettighetsperioder)

    fun utfall(perioder: List<Rettighetsperiode>): Utfall {
        val (nye, arvede) = perioder.partition { it.opprinnelse == "Ny" }

        return when {
            // Ingen endring
            nye.isEmpty() -> Utfall.Beregning
            // Ny kjede
            arvede.isEmpty() -> if (nye.harRett()) Utfall.Innvilgelse else Utfall.Avslag
            // Bygger videre på en kjede
            arvede.sisteHarRett() && !nye.harRett() -> Utfall.Stans
            else -> Utfall.Gjenopptak
        }
    }

    private fun List<Rettighetsperiode>.harRett() = any { it.harRett }

    private fun List<Rettighetsperiode>.sisteHarRett() = last().harRett

    enum class Utfall {
        Innvilgelse,
        Avslag,
        Stans,
        Gjenopptak,
        Beregning,
    }

    private fun rettighet(opplysningTypeId: String) =
        packet.opplysning(opplysningTypeId)?.let {
            it["perioder"].any { periode -> periode["verdi"]["verdi"].asBoolean() }
        } ?: false

    private companion object {
        private val søknadTypeId = "0194881f-91d1-7df2-ba1d-4533f37fcc77"
        private val kvoteOpplysninger =
            listOf(
                // Fobrukt
                "01992934-66e4-7606-bdd3-c6c9dd420ffd",
                // Gjenstår
                "01992956-e349-76b1-8f68-c9d481df3a32",
            )

        private fun JsonMessage.opplysning(navn: String) = this["opplysninger"].singleOrNull { it["opplysningTypeId"].asText() == navn }
    }
}
