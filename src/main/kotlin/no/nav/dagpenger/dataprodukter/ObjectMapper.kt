package no.nav.dagpenger.dataprodukter

import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.module.kotlin.jacksonMapperBuilder

internal val objectMapper =
    jacksonMapperBuilder()
        .disable(FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
