package no.nav.dagpenger.data.inntekt.utils

import java.time.LocalDate

class TemporalCollection<V> {
    private val contents = mutableMapOf<LocalDate, V>()
    private val milestones get() = contents.keys.toList().sortedDescending()

    fun get(date: LocalDate): V = milestones
        .firstOrNull { it.isBefore(date) || it.isEqual(date) }?.let {
            contents[it]
        } ?: throw IllegalArgumentException("No records that early")

    fun getOrPut(date: LocalDate, defaultValue: () -> V): V =
        if (milestones.contains(date)) get(date) else defaultValue().also {
            put(date, it)
        }

    fun put(at: LocalDate, item: V) {
        contents[at] = item
    }
}
