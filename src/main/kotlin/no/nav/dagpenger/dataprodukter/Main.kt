package no.nav.dagpenger.dataprodukter

import no.nav.dagpenger.dataprodukt.behandling.Behandling
import no.nav.dagpenger.dataprodukt.behandling.BehandlingEndretTilstand
import no.nav.dagpenger.dataprodukt.behandling.Vedtak
import no.nav.dagpenger.dataprodukt.innlop.Soknadsinnlop
import no.nav.dagpenger.dataprodukt.soknad.Dokumentkrav
import no.nav.dagpenger.dataprodukt.soknad.SoknadFaktum
import no.nav.dagpenger.dataprodukt.soknad.SoknadTilstand
import no.nav.dagpenger.dataprodukter.kafka.DataTopic.Companion.dataTopic
import no.nav.dagpenger.dataprodukter.person.PdlPersonRepository
import no.nav.dagpenger.dataprodukter.produkter.behandling.BehandlingEndretTilstandRiver
import no.nav.dagpenger.dataprodukter.produkter.behandling.BehandlingRiver
import no.nav.dagpenger.dataprodukter.produkter.behandling.VedtakFattetRiver
import no.nav.dagpenger.dataprodukter.produkter.innlop.SoknadsinnlopRiver
import no.nav.dagpenger.dataprodukter.produkter.søknad.DokumentkravRiver
import no.nav.dagpenger.dataprodukter.produkter.søknad.SøknadInnsendtRiver
import no.nav.dagpenger.dataprodukter.produkter.søknad.SøknadTilstandRiver
import no.nav.dagpenger.dataprodukter.produkter.søknad.SøknadsdataRiver
import no.nav.dagpenger.dataprodukter.søknad.InMemorySøknadRepository
import no.nav.helse.rapids_rivers.RapidApplication

internal object DataTopics {
    val soknadsinnlop = dataTopic<Soknadsinnlop>(config[kafka_produkt_topic])
    val soknadFaktum = dataTopic<SoknadFaktum>(config[kafka_produkt_soknad_faktum_topic])
    val soknadTilstand = dataTopic<SoknadTilstand>(config[kafka_produkt_soknad_tilstand_topic])
    val dokumentkrav = dataTopic<Dokumentkrav>(config[kafka_produkt_soknad_dokumentkrav_topic])
    val behandlingTopic = dataTopic<Behandling>(config[kafka_produkt_behandling_topic])
    val vedtakTopic = dataTopic<Vedtak>(config[kafka_produkt_vedtak_topic])
    val behandlingTilstandTopic = dataTopic<BehandlingEndretTilstand>(config[kafka_produkt_behandling_tilstand_topic])
}

fun main() {
    val env = System.getenv()
    val søknadRepository = InMemorySøknadRepository()
    val personRepository =
        PdlPersonRepository(
            endpoint = config[pdl.endpoint],
            scope = config[pdl.scope],
        )

    RapidApplication
        .create(env) { _, rapidsConnection ->
            SoknadsinnlopRiver(rapidsConnection, DataTopics.soknadsinnlop)
            SøknadsdataRiver(rapidsConnection, søknadRepository, personRepository)
            SøknadInnsendtRiver(rapidsConnection, søknadRepository, DataTopics.soknadFaktum)
            SøknadTilstandRiver(rapidsConnection, DataTopics.soknadTilstand)
            DokumentkravRiver(rapidsConnection, DataTopics.dokumentkrav)

            // Behandling
            BehandlingRiver(rapidsConnection, DataTopics.behandlingTopic)
            BehandlingEndretTilstandRiver(rapidsConnection, DataTopics.behandlingTilstandTopic)

            // Vedtak
            VedtakFattetRiver(rapidsConnection, DataTopics.vedtakTopic)
        }.start()
}
