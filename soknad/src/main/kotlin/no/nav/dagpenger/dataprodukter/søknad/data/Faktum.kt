package no.nav.dagpenger.dataprodukter.søknad.data

data class Faktum internal constructor(
    val beskrivendeId: String,
    val type: String,
    val svar: String,
    val gruppe: String?,
    val gruppeId: String?,
) {
    val erFritekst = type == "tekst"
}
