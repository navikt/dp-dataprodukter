package no.nav.dagpenger.data.inntekt

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig

private val unleash = DefaultUnleash(
    UnleashConfig.builder()
        .appName(requireNotNull(System.getenv("NAIS_APP_NAME")) { "Expected NAIS_APP_NAME" })
        .instanceId(requireNotNull(System.getenv("HOSTNAME")) { "Expected HOSTNAME" })
        .environment(requireNotNull(System.getenv("NAIS_CLUSTER_NAME")) { "Expected NAIS_CLUSTER_NAME" })
        .unleashAPI("https://unleash.nais.io/")
        // .customHttpHeader("Authorization", "API token")
        .build()
)

fun skalLageFalskeData() = unleash.isEnabled("dp-data-inntekt.produser-mock-data", false)
