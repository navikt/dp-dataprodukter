package no.nav.dagpenger.data.innlop.tjenester

import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.data.innlop.SoknadIdent
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.dagpenger.data.innlop.person.PersonRepository
import no.nav.dagpenger.data.innlop.person.PersonRepository.Person
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class SøknadIdentRiverTest {
    private val personRepository = mockk<PersonRepository>(relaxed = true)
    private val topic = mockk<DataTopic<SoknadIdent>>(relaxed = true)
    private val rapid =
        TestRapid().also {
            SøknadIdentRiver(it, topic, personRepository)
        }

    @Test
    fun `Skal opprette en ny søknad med ident`() {
        every {
            personRepository.hentPerson("12345678901")
        } returns mockk<Person>(relaxed = true)

        rapid.sendTestMessage(søknadIdentMessage)

        every {
            topic.publiser(any())
        }
    }

    @Language("JSON")
    private val søknadIdentMessage =
        """
        {
            "@event_name": "søknad_endret_tilstand",
            "gjeldendeTilstand": "opprettet",
            "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000",
            "ident": "12345678901"
        }
        """.trimIndent()
}
