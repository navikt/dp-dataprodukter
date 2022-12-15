package no.nav.dagpenger.data.innlop.tjenester

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.data.innlop.DataTopic
import no.nav.dagpenger.data.innlop.SøknadFaktum
import no.nav.dagpenger.data.innlop.søknad.InMemorySøknadRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SøknadsdataRiverTest {
    private val repository = InMemorySøknadRepository()
    private val producer = mockk<KafkaProducer<String, SøknadFaktum>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid = TestRapid().also {
        SøknadsdataRiver(it, repository)
        SøknadInnsendtRiver(it, repository, dataTopic)
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun foo() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(
            getDataMessage(søknadId) {
                seksjon(
                    faktum("seksjon1-faktum1", "land", "NOR"),
                    faktum("seksjon1-faktum2", "land", "NOR")
                )
                seksjon(faktum("seksjon2-faktum1", "land", "NOR"))
            }
        )
        rapid.sendTestMessage(getInnsendtMessage(søknadId))
        val packet = mutableListOf<SøknadFaktum>()
        verify(exactly = 3) {
            producer.send(any())
            // dataTopic.publiser(allAny())
        }
    }
}

private fun getDataMessage(uuid: UUID, seksjoner: Seksjoner.() -> Seksjoner) =
    JsonMessage.newMessage(
        "søker_oppgave",
        mapOf(
            "versjon_navn" to "Dagpenger",
            "søknad_uuid" to uuid,
            "ferdig" to true,
            "seksjoner" to seksjoner(mutableListOf())
        )
    ).toJson()
typealias Seksjoner = MutableList<Map<String, Any>>

private fun Seksjoner.seksjon(vararg faktum: Map<String, Any>) = this.also { it.add(mapOf(fakta(*faktum))) }
private fun fakta(vararg faktum: Map<String, Any>) = "fakta" to faktum.toList()
private fun faktum(beskrivendeId: String, type: String, svar: Any) =
    mapOf("beskrivendeId" to beskrivendeId, "type" to type, "svar" to svar)

private fun getInnsendtMessage(uuid: UUID) = JsonMessage.newMessage(
    "søknad_endret_tilstand",
    mapOf(
        "søknad_uuid" to uuid,
        "gjeldendeTilstand" to "Innsendt"
    )
).toJson()
