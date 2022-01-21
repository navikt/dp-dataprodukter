package no.nav.dagpenger.data.inntekt

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig

private val unleash = DefaultUnleash(
    UnleashConfig.builder()
        .appName(System.getenv("NAIS_APP_NAME"))
        .instanceId(System.getenv("HOSTNAME"))
        .environment(System.getenv("NAIS_CLUSTER_NAME"))
        .unleashAPI(System.getenv("https://unleash.nais.io/"))
        // .customHttpHeader("Authorization", "API token")
        .build()
)

fun skalLageFalskeData() = unleash.isEnabled("dp-data-inntekt.produser-mock-data", false)
