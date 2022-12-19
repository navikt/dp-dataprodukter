package no.nav.dagpenger.data.innlop.helpers

import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

typealias Seksjoner = MutableList<Map<String, Any>>

fun Seksjoner.seksjon(vararg faktum: Map<String, Any>) = this.also { it.add(mapOf("fakta" to faktum.toList())) }
fun Seksjoner.seksjon(generator: Map<String, Any>) =
    this.also { it.add(mapOf("fakta" to listOf(generator))) }

fun generator(beskrivendeId: String, vararg faktum: Map<String, Any>) = mapOf(
    "beskrivendeId" to beskrivendeId,
    "type" to "generator",
    "svar" to listOf(faktum.toList())
)

fun faktum(beskrivendeId: String, type: String, svar: Any, id: String = "1") =
    mapOf("beskrivendeId" to beskrivendeId, "type" to type, "svar" to svar, "id" to id)

fun tilstandEndretEvent(uuid: UUID, gjeldeneTilstand: String, forrigeTilstand: String = gjeldeneTilstand) =
    JsonMessage.newMessage(
        "søknad_endret_tilstand",
        mapOf(
            "søknad_uuid" to uuid,
            "forrigeTilstand" to forrigeTilstand,
            "gjeldendeTilstand" to gjeldeneTilstand
        )
    ).toJson()
