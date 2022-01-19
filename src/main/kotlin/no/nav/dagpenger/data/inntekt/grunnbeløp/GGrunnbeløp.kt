package no.nav.dagpenger.data.inntekt.grunnbeløp

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.dagpenger.data.inntekt.Grunnbeløp
import no.nav.dagpenger.data.inntekt.utils.CachedProperty
import no.nav.dagpenger.data.inntekt.utils.TemporalCollection
import java.io.BufferedReader
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GGrunnbeløp(timeToLive: Duration) : Grunnbeløp {
    constructor() : this(Duration.ofHours(1))

    companion object {
        private val objectMapper = ObjectMapper()
    }

    private val grunnbeløp by CachedProperty(timeToLive) {
        TemporalCollection<Double>()
    }

    override fun gjeldendeGrunnbeløp(fom: LocalDate) = grunnbeløp.getOrPut(fom) { hentGrunnbeløpFraG(fom) }

    private fun hentGrunnbeløpFraG(fom: LocalDate) =
        getHTTP("https://g.nav.no/api/v1/grunnbeloep?dato=${fom.asISO8601()}").use(::parseResponse)

    private fun getHTTP(url: String) = URL(url).openStream().bufferedReader()

    private fun parseResponse(it: BufferedReader) = with(objectMapper.readTree(it)) {
        this["grunnbeloep"].asDouble()
    }

    private fun LocalDate.asISO8601() = format(DateTimeFormatter.ISO_DATE)
}
