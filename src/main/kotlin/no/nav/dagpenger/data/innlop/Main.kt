package no.nav.dagpenger.data.innlop

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.dagpenger.data.Behandling
import no.nav.dagpenger.data.innlop.kafka.DataTopic.Companion.dataTopic
import no.nav.dagpenger.data.innlop.oauth2.AzureAD
import no.nav.dagpenger.data.innlop.person.PdlPersonRepository
import no.nav.dagpenger.data.innlop.søknad.InMemorySøknadRepository
import no.nav.dagpenger.data.innlop.tjenester.BehandlingEndretTilstandRiver
import no.nav.dagpenger.data.innlop.tjenester.DokumentkravRiver
import no.nav.dagpenger.data.innlop.tjenester.SoknadsinnlopRiver
import no.nav.dagpenger.data.innlop.tjenester.SøknadIdentRiver
import no.nav.dagpenger.data.innlop.tjenester.SøknadInnsendtRiver
import no.nav.dagpenger.data.innlop.tjenester.SøknadTilstandRiver
import no.nav.dagpenger.data.innlop.tjenester.SøknadsdataRiver
import no.nav.dagpenger.data.innlop.tjenester.UtlandRiver
import no.nav.helse.rapids_rivers.RapidApplication

internal object DataTopics {
    val soknadsinnlop = dataTopic<Soknadsinnlop>(config[kafka_produkt_topic])
    val ident = dataTopic<Ident>(config[kafka_produkt_ident_topic])
    val utland = dataTopic<Utland>(config[kafka_produkt_utland_topic])
    val soknadFaktum = dataTopic<SoknadFaktum>(config[kafka_produkt_soknad_faktum_topic])
    val soknadTilstand = dataTopic<SoknadTilstand>(config[kafka_produkt_soknad_tilstand_topic])
    val soknadIdent = dataTopic<SoknadIdent>(config[kafka_produkt_soknad_ident_topic])
    val dokumentkrav = dataTopic<Dokumentkrav>(config[kafka_produkt_soknad_dokumentkrav_topic])
    val behandlingTopic = dataTopic<Behandling>(config[kafka_produkt_behandling_topic])
}

fun main() {
    val env = System.getenv()
    val søknadRepository = InMemorySøknadRepository()
    val azureAdClient = AzureAD(config[pdl.scope])
    val personRepository =
        PdlPersonRepository(
            GraphQLWebClient(url = config[pdl.endpoint]),
        ) { azureAdClient.token().toAuthorizationHeader() }

    RapidApplication
        .create(env) { _, rapidsConnection ->
            SoknadsinnlopRiver(rapidsConnection, DataTopics.soknadsinnlop, DataTopics.ident)
            UtlandRiver(rapidsConnection, DataTopics.utland)
            SøknadsdataRiver(rapidsConnection, søknadRepository)
            SøknadInnsendtRiver(rapidsConnection, søknadRepository, DataTopics.soknadFaktum)
            SøknadTilstandRiver(rapidsConnection, DataTopics.soknadTilstand)
            SøknadIdentRiver(rapidsConnection, DataTopics.soknadIdent, personRepository)
            DokumentkravRiver(rapidsConnection, DataTopics.dokumentkrav)
            BehandlingEndretTilstandRiver(rapidsConnection, DataTopics.behandlingTopic)
        }.start()
}
