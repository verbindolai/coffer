package org.coffer.coffer2.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.remote.swissquote.SpreadProfilePrice
import org.coffer.coffer2.remote.swissquote.SwissquoteClient
import org.coffer.coffer2.remote.swissquote.SwissquoteQuoteResponse
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwissquoteFetcherServiceImplTest {

    @Test
    fun `should fetch and convert GOLD quote successfully`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        val apiResponse = listOf(
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
                ts = 1609459200000L
            )
        )

        every { swissquoteClient.getMetalPrice("XAU") } returns apiResponse

        // When
        val result = service.getMetalQuote(MetalType.GOLD)

        // Then
        assertNotNull(result)
        assertEquals(MetalType.GOLD, result.metalType)
        // bid = 1800.50 per troy ounce, converted to per gram
        val expectedPricePerGram = BigDecimal("1800.50").divide(BigDecimal("31.1034768"), 8, java.math.RoundingMode.HALF_UP)
        assertEquals(0, expectedPricePerGram.compareTo(result.pricePerGram))
        assertEquals("EUR", result.currency.currencyCode)
        assertEquals(MetalQuoteSource.SWISSQUOTE, result.source)

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
    }

    @Test
    fun `should fetch and convert SILVER quote successfully`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        val apiResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("25.75"),
                        ask = BigDecimal("25.85"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        every { swissquoteClient.getMetalPrice("XAG") } returns apiResponse

        // When
        val result = service.getMetalQuote(MetalType.SILVER)

        // Then
        assertNotNull(result)
        assertEquals(MetalType.SILVER, result.metalType)
        // bid = 25.75 per troy ounce, converted to per gram
        val expectedPricePerGram = BigDecimal("25.75").divide(BigDecimal("31.1034768"), 8, java.math.RoundingMode.HALF_UP)
        assertEquals(0, expectedPricePerGram.compareTo(result.pricePerGram))

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAG") }
    }

    @Test
    fun `should fetch and convert PLATINUM quote successfully`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        val apiResponse = listOf(
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

        every { swissquoteClient.getMetalPrice("XPT") } returns apiResponse

        // When
        val result = service.getMetalQuote(MetalType.PLATINUM)

        // Then
        assertNotNull(result)
        assertEquals(MetalType.PLATINUM, result.metalType)
        // bid = 1050.00 per troy ounce, converted to per gram
        val expectedPricePerGram = BigDecimal("1050.00").divide(BigDecimal("31.1034768"), 8, java.math.RoundingMode.HALF_UP)
        assertEquals(0, expectedPricePerGram.compareTo(result.pricePerGram))

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XPT") }
    }

    @Test
    fun `should return null when API returns empty list`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        every { swissquoteClient.getMetalPrice("XAU") } returns emptyList()

        // When
        val result = service.getMetalQuote(MetalType.GOLD)

        // Then
        assertNull(result)
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
    }

    @Test
    fun `should return null when API returns response with empty prices`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        val apiResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = emptyList(),
                ts = 1609459200000L
            )
        )

        every { swissquoteClient.getMetalPrice("XAU") } returns apiResponse

        // When
        val result = service.getMetalQuote(MetalType.GOLD)

        // Then
        assertNull(result)
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
    }

    @Test
    fun `should convert metal type to correct symbol`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        val apiResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("100.00"),
                        ask = BigDecimal("102.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        // Setup mocks for all metal types
        every { swissquoteClient.getMetalPrice("XAU") } returns apiResponse
        every { swissquoteClient.getMetalPrice("XAG") } returns apiResponse
        every { swissquoteClient.getMetalPrice("XPT") } returns apiResponse
        every { swissquoteClient.getMetalPrice("XNIK") } returns apiResponse

        // When & Then - Verify each metal type converts to correct symbol
        service.getMetalQuote(MetalType.GOLD)
        verify { swissquoteClient.getMetalPrice("XAU") }

        service.getMetalQuote(MetalType.SILVER)
        verify { swissquoteClient.getMetalPrice("XAG") }

        service.getMetalQuote(MetalType.PLATINUM)
        verify { swissquoteClient.getMetalPrice("XPT") }

        service.getMetalQuote(MetalType.NICKEL)
        verify { swissquoteClient.getMetalPrice("XNIK") }
    }

    @Test
    fun `should propagate exception from Feign client`() {
        // Given
        val swissquoteClient = mockk<SwissquoteClient>()
        val service = SwissquoteFetcherServiceImpl(swissquoteClient)

        every { swissquoteClient.getMetalPrice("XAU") } throws RuntimeException("API connection failed")

        // When & Then
        try {
            service.getMetalQuote(MetalType.GOLD)
            throw AssertionError("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("API connection failed", e.message)
        }

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
    }
}
