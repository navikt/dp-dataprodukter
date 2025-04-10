package no.nav.dagpenger.dataprodukter

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

val config =
    ConfigurationProperties.systemProperties() overriding
        EnvironmentVariables()
val kafka_produkt_topic by stringType
val kafka_produkt_soknad_faktum_topic by stringType
val kafka_produkt_soknad_tilstand_topic by stringType
val kafka_produkt_soknad_dokumentkrav_topic by stringType
val kafka_produkt_behandling_topic by stringType
val kafka_produkt_vedtak_topic by stringType

object pdl : PropertyGroup() {
    val endpoint by stringType
    val scope by stringType
}

object azure : PropertyGroup() {
    val app_client_id by stringType
    val app_client_secret by stringType
    val openid_config_token_endpoint by stringType
}
