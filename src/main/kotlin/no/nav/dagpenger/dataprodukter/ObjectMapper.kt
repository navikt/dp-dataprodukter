package no.nav.dagpenger.dataprodukter

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

internal val objectMapper =
    jacksonObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
