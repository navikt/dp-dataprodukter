package no.nav.dagpenger.data.inntekt

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding

val config = ConfigurationProperties.systemProperties() overriding
    EnvironmentVariables()
