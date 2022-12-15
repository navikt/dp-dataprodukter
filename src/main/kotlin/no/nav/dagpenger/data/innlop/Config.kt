package no.nav.dagpenger.data.innlop

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

val config = ConfigurationProperties.systemProperties() overriding
    EnvironmentVariables()

val kafka_produkt_topic by stringType
val kafka_produkt_utland_topic by stringType
val kafka_produkt_ident_topic by stringType
val kafka_produkt_faktum_svar_topic by stringType
