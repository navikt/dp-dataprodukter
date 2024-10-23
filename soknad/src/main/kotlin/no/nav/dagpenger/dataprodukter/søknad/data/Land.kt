package no.nav.dagpenger.dataprodukter.søknad.data

internal class Land(
    val navn: String,
) : Comparable<Land> {
    fun erEØS() = navn in eøsLand

    override fun compareTo(other: Land) = navn.compareTo(other.navn)

    companion object {
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
    }
}
