package org.coffer.coffer2.remote.swissquote

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class SwissquoteQuoteResponseTest {

    @Test
    fun `midPrice should calculate average of bid and ask`() {
        // Given
        val price = SpreadProfilePrice(
            spreadProfile = "standard",
            bid = BigDecimal("1800.00"),
            ask = BigDecimal("1802.00"),
            bidSpread = null,
            askSpread = null
        )

        // When
        val midPrice = price.midPrice()

        // Then
        assertEquals(0, BigDecimal("1801.00").compareTo(midPrice))
    }

    @Test
    fun `midPrice should handle decimal precision correctly`() {
        // Given - values that could have floating point issues
        val price = SpreadProfilePrice(
            spreadProfile = "standard",
            bid = BigDecimal("0.1"),
            ask = BigDecimal("0.2"),
            bidSpread = null,
            askSpread = null
        )

        // When
        val midPrice = price.midPrice()

        // Then - BigDecimal gives exact result
        assertEquals(0, BigDecimal("0.15").compareTo(midPrice))
    }
}
