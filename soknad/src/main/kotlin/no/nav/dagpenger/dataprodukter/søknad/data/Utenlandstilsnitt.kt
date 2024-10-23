package no.nav.dagpenger.dataprodukter.søknad.data

import java.util.SortedSet

data class Utenlandstilsnitt internal constructor(
    private val bosted: Land,
    private val arbeidssted: SortedSet<Land>,
) {
    internal constructor(
        bosted: String,
        arbeidssted: Collection<String>,
    ) : this(
        Land(bosted),
        arbeidssted.map { Land(it) }.toSortedSet(),
    )

    val erUtland = (setOf(bosted) + arbeidssted).any { it.erEØS() }
    val harArbeidsforholdEØS = arbeidssted.any { it.erEØS() }

    val bostedsland: String = bosted.navn
    val arbeidsland = arbeidssted.map { it.navn }.toSortedSet()
}
