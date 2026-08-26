package no.nav.dagpenger.dataprodukter

import tools.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asString().let { UUID.fromString(it) }
