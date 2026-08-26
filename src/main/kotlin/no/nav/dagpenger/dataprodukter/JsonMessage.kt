package no.nav.dagpenger.dataprodukter

import tools.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
