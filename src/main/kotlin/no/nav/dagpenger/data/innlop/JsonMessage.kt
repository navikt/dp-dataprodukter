package no.nav.dagpenger.data.innlop

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
