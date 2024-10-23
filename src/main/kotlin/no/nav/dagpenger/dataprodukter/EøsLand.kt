package no.nav.dagpenger.dataprodukter

fun String.erEØS() = this in eøsLand

private val eøsLand =
    setOf(
        "AUT",
        "BEL",
        "BGR",
        "CHE",
        "CYP",
        "CZE",
        "DEU",
        "DNK",
        "ESP",
        "EST",
        "FIN",
        "FRA",
        "GBR",
        "GRC",
        "HRV",
        "HUN",
        "IRL",
        "ISL",
        "ITA",
        "LIE",
        "LTU",
        "LUX",
        "LVA",
        "MLT",
        "NLD",
        "POL",
        "PRT",
        "ROU",
        "SVK",
        "SVN",
        "SWE",
    )
