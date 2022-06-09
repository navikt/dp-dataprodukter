package no.nav.dagpenger.data.innlop.utils

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

internal class TemporalCollectionTest {
    private lateinit var collection: TemporalCollection<BigDecimal>
    private val verdi1 = BigDecimal(5)
    private val verdi2 = BigDecimal(10)

    @BeforeEach
    fun setUp() {
        collection = TemporalCollection()
        collection.put(LocalDate.of(2020, 3, 1), verdi1)
        collection.put(LocalDate.of(2020, 7, 4), verdi2)
    }

    @Test
    fun `får riktig verdi til riktig dato`() {
        assertThrows<IllegalArgumentException> {
            collection.get(LocalDate.of(2020, 1, 1))
        }
        assertSame(verdi1, collection.get(LocalDate.of(2020, 3, 1)))
        assertSame(verdi1, collection.get(LocalDate.of(2020, 7, 1)))
        assertSame(verdi2, collection.get(LocalDate.of(2020, 7, 4)))
        assertSame(verdi2, collection.get(LocalDate.of(2020, 7, 15)))
        assertSame(verdi2, collection.get(LocalDate.of(2022, 7, 15)))
    }

    @Test
    fun getOrPut() {
        val verdi3 = BigDecimal(15)
        val date = LocalDate.of(2022, 3, 1)

        assertSame(
            verdi3,
            collection.getOrPut(date) { verdi3 }
        )

        assertSame(verdi3, collection.get(date))
    }
}
