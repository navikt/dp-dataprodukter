package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.databind.JsonNode

internal class GammeltSøknadFormat(data: JsonNode) : SøknadData(data) {
    override val bostedsland: String
        get() = getFaktumValue(getFakta("bostedsland.land")).asText()
    override val arbeidsforholdLand: List<String>
        get() = avsluttetArbeidsforhold().map { it.land } + eosArbeidsforhold()

    private fun avsluttetArbeidsforhold() =
        this.getFakta("arbeidsforhold")
            .map {
                AvsluttetArbeidsforhold(
                    sluttårsak = asÅrsak(it["properties"]["type"].asText()),
                    fiskeforedling = it["properties"]["fangstogfiske"]?.asBoolean() ?: false,
                    land = it["properties"]["land"].asText()
                )
            }

    private fun eosArbeidsforhold() =
        this.getFakta("eosarbeidsforhold")
            .map { it["properties"]["land"].asText() }

    private fun getFakta(faktaNavn: String) =
        data.get("fakta")?.filter { it["key"].asText() == faktaNavn } ?: emptyList()

    private fun getFaktumValue(fakta: List<JsonNode>) = fakta.first().get("value")

    private fun asÅrsak(type: String) = when (type) {
        "permittert" -> AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT
        "avskjediget" -> AvsluttetArbeidsforhold.Sluttårsak.AVSKJEDIGET
        "kontraktutgaatt" -> AvsluttetArbeidsforhold.Sluttårsak.KONTRAKT_UTGAATT
        "redusertarbeidstid" -> AvsluttetArbeidsforhold.Sluttårsak.REDUSERT_ARBEIDSTID
        "sagtoppavarbeidsgiver" -> AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
        "sagtoppselv" -> AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_SELV
        "arbeidsgivererkonkurs" -> AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS
        "ikke-endret" -> AvsluttetArbeidsforhold.Sluttårsak.IKKE_ENDRET
        else -> throw Exception("Missing permitteringstype: $type")
    }
}
