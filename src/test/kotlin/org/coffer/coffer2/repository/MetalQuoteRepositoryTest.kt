package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.MetalType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetalQuoteRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var metalQuoteRepository: MetalQuoteRepository

    @Test
    fun `should save and retrieve metal quote entity`() {
        // Given
        val quote = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("65.50"),
            currencyCode = "USD",
            quotedAt = ZonedDateTime.now(),
            source = "Swissquote",
            createdAt = ZonedDateTime.now()
        )

        // When
        val savedQuote = metalQuoteRepository.save(quote)
        val retrievedQuote = metalQuoteRepository.findById(savedQuote.id)

        // Then
        assertTrue(retrievedQuote.isPresent)
        assertEquals(quote.metalType, retrievedQuote.get().metalType)
        assertEquals(0, quote.pricePerGram.compareTo(retrievedQuote.get().pricePerGram))
        assertEquals(quote.currencyCode, retrievedQuote.get().currencyCode)
        assertEquals(quote.source, retrievedQuote.get().source)
    }

    @Test
    fun `should update existing metal quote`() {
        // Given
        val quote = createTestQuote(MetalType.SILVER, BigDecimal("0.85"))
        val savedQuote = metalQuoteRepository.save(quote)

        // When
        val updatedQuote = savedQuote.copy(pricePerGram = BigDecimal("0.90"))
        metalQuoteRepository.save(updatedQuote)
        val retrievedQuote = metalQuoteRepository.findById(savedQuote.id)

        // Then
        assertTrue(retrievedQuote.isPresent)
        assertEquals(0, BigDecimal("0.90").compareTo(retrievedQuote.get().pricePerGram))
    }

    @Test
    fun `should delete metal quote`() {
        // Given
        val quote = createTestQuote(MetalType.PLATINUM, BigDecimal("32.00"))
        val savedQuote = metalQuoteRepository.save(quote)

        // When
        metalQuoteRepository.deleteById(savedQuote.id)
        val retrievedQuote = metalQuoteRepository.findById(savedQuote.id)

        // Then
        assertTrue(retrievedQuote.isEmpty)
    }

    @Test
    fun `should save quotes for all metal types`() {
        // Given
        val metalTypes = listOf(
            MetalType.GOLD,
            MetalType.SILVER,
            MetalType.PLATINUM,
            MetalType.NICKEL,
            MetalType.OTHER
        )

        // When & Then
        metalTypes.forEach { metalType ->
            val quote = createTestQuote(metalType, BigDecimal("10.00"))
            val savedQuote = metalQuoteRepository.save(quote)
            val retrieved = metalQuoteRepository.findById(savedQuote.id)
            assertTrue(retrieved.isPresent)
            assertEquals(metalType, retrieved.get().metalType)
        }
    }

    @Test
    fun `should handle different currencies`() {
        // Given
        val currencies = listOf("USD", "EUR", "GBP", "CHF")

        // When & Then
        currencies.forEach { currency ->
            val quote = createTestQuote(MetalType.GOLD, BigDecimal("50.00")).copy(currencyCode = currency)
            val savedQuote = metalQuoteRepository.save(quote)
            val retrieved = metalQuoteRepository.findById(savedQuote.id)
            assertTrue(retrieved.isPresent)
            assertEquals(currency, retrieved.get().currencyCode)
        }
    }

    private fun createTestQuote(metalType: MetalType, price: BigDecimal): MetalQuoteEntity =
        MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = metalType,
            pricePerGram = price,
            currencyCode = "USD",
            quotedAt = ZonedDateTime.now(),
            source = "Test Source",
            createdAt = ZonedDateTime.now()
        )
}
