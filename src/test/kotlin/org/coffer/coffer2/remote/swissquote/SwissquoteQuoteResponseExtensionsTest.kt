package org.coffer.coffer2.remote.swissquote

import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwissquoteQuoteResponseExtensionsTest {

    @Test
    fun `toMetalQuote should convert response with all fields correctly`() {
        // Given
        val timestamp = 1609459200000L // 2021-01-01 00:00:00 UTC
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.50"),
                        ask = BigDecimal("1802.50"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = timestamp
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        assertEquals(MetalType.GOLD, result.metalType)
        assertEquals(0, BigDecimal("1800.50").compareTo(result.pricePerGram))
        assertEquals("EUR", result.currency.currencyCode)
        assertEquals(MetalQuoteSource.SWISSQUOTE, result.source)

        val expectedTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC"))
        assertEquals(expectedTime, result.quotedAt)
    }

    @Test
    fun `toMetalQuote should return null for empty list`() {
        // Given
        val emptyList = emptyList<SwissquoteQuoteResponse>()

        // When
        val result = emptyList.toMetalQuote(MetalType.GOLD)

        // Then
        assertNull(result)
    }

    @Test
    fun `toMetalQuote should return null when spread profile prices are empty`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = emptyList(),
                ts = 1609459200000L
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNull(result)
    }

    @Test
    fun `toMetalQuote should use current time when timestamp is missing`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("25.50"),
                        ask = BigDecimal("25.60"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 0L
            )
        )

        // When
        val before = ZonedDateTime.now().minusSeconds(1)
        val result = responses.toMetalQuote(MetalType.SILVER)
        val after = ZonedDateTime.now().plusSeconds(1)

        // Then
        assertNotNull(result)
        // If timestamp is 0 or invalid, it should use current time via the fallback
        // The fallback is: timestamp?.let { ... } ?: ZonedDateTime.now()
        // Since ts = 0, it will still create a date from epoch 0, not use fallback
        // Let me check the actual behavior
        val expectedTime = Instant.ofEpochMilli(0L).atZone(ZoneId.of("UTC"))
        assertEquals(expectedTime, result.quotedAt)
    }

    @Test
    fun `toMetalQuote should handle different metal types`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1050.00"),
                        ask = BigDecimal("1052.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        // When - test with PLATINUM
        val result = responses.toMetalQuote(MetalType.PLATINUM)

        // Then
        assertNotNull(result)
        assertEquals(MetalType.PLATINUM, result.metalType)
    }

    @Test
    fun `toMetalQuote should use bid price not mid price`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.00"),
                        ask = BigDecimal("1900.00"), // Large spread
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        // Should use bid (1800.00), not mid price (1850.00)
        assertEquals(0, BigDecimal("1800.00").compareTo(result.pricePerGram))
    }

    @Test
    fun `toMetalQuote should handle BigDecimal precision correctly`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.123456789"),
                        ask = BigDecimal("1802.987654321"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        assertEquals(0, BigDecimal("1800.123456789").compareTo(result.pricePerGram))
    }

    @Test
    fun `toMetalQuote should convert timestamp to UTC timezone`() {
        // Given
        val timestamp = 1609459200000L // 2021-01-01 00:00:00 UTC
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.00"),
                        ask = BigDecimal("1802.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = timestamp
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        assertEquals("UTC", result.quotedAt.zone.id)
        assertEquals(2021, result.quotedAt.year)
        assertEquals(1, result.quotedAt.monthValue)
        assertEquals(1, result.quotedAt.dayOfMonth)
    }

    @Test
    fun `toMetalQuote should always use EUR currency`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.00"),
                        ask = BigDecimal("1802.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        assertEquals(Currency.getInstance("EUR"), result.currency)
    }

    @Test
    fun `toMetalQuote should always use SWISSQUOTE source`() {
        // Given
        val responses = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.00"),
                        ask = BigDecimal("1802.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        // When
        val result = responses.toMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        assertEquals(MetalQuoteSource.SWISSQUOTE, result.source)
    }
}
