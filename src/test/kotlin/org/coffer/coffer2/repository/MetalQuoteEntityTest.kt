package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MetalQuoteEntityTest {

    @Test
    fun `toMetalQuote should convert entity to domain model correctly`() {
        // Given
        val entityId = UUID.randomUUID()
        val quotedAt = ZonedDateTime.now().minusHours(1)
        val createdAt = ZonedDateTime.now()

        val entity = MetalQuoteEntity(
            id = entityId,
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("65.50"),
            currencyCode = "USD",
            quotedAt = quotedAt,
            source = "Swissquote",
            createdAt = createdAt
        )

        // When
        val quote = entity.toMetalQuote()

        // Then
        assertEquals(entityId.toString(), quote.id)
        assertEquals(MetalType.GOLD, quote.metalType)
        assertEquals(BigDecimal("65.50"), quote.pricePerGram)
        assertEquals(Currency.getInstance("USD"), quote.currency)
        assertEquals(quotedAt, quote.quotedAt)
        assertEquals("Swissquote", quote.source)
        assertEquals(createdAt, quote.createdAt)
    }

    @Test
    fun `fromMetalQuote should convert domain model to entity correctly`() {
        // Given
        val quotedAt = ZonedDateTime.now().minusHours(2)
        val createdAt = ZonedDateTime.now()

        val quote = MetalQuote(
            id = UUID.randomUUID().toString(),
            metalType = MetalType.SILVER,
            pricePerGram = BigDecimal("0.85"),
            currency = Currency.getInstance("EUR"),
            quotedAt = quotedAt,
            source = "LME",
            createdAt = createdAt
        )

        // When
        val entity = MetalQuoteEntity.fromMetalQuote(quote)

        // Then
        assertEquals(quote.id, entity.id.toString())
        assertEquals(MetalType.SILVER, entity.metalType)
        assertEquals(BigDecimal("0.85"), entity.pricePerGram)
        assertEquals("EUR", entity.currencyCode)
        assertEquals(quotedAt, entity.quotedAt)
        assertEquals("LME", entity.source)
        assertEquals(createdAt, entity.createdAt)
    }

    @Test
    fun `fromMetalQuote should generate UUID when quote id is null`() {
        // Given
        val quote = MetalQuote(
            id = null,
            metalType = MetalType.PLATINUM,
            pricePerGram = BigDecimal("32.00"),
            currency = Currency.getInstance("USD"),
            quotedAt = ZonedDateTime.now(),
            source = "Test Source",
            createdAt = ZonedDateTime.now()
        )

        // When
        val entity = MetalQuoteEntity.fromMetalQuote(quote)

        // Then
        assertNotNull(entity.id)
    }

    @Test
    fun `round-trip conversion should preserve all fields`() {
        // Given
        val quotedAt = ZonedDateTime.now().minusDays(1)
        val createdAt = ZonedDateTime.now()

        val originalQuote = MetalQuote(
            id = UUID.randomUUID().toString(),
            metalType = MetalType.NICKEL,
            pricePerGram = BigDecimal("0.015"),
            currency = Currency.getInstance("GBP"),
            quotedAt = quotedAt,
            source = "Test Exchange",
            createdAt = createdAt
        )

        // When
        val entity = MetalQuoteEntity.fromMetalQuote(originalQuote)
        val resultQuote = entity.toMetalQuote()

        // Then
        assertEquals(originalQuote.id, resultQuote.id)
        assertEquals(originalQuote.metalType, resultQuote.metalType)
        assertEquals(originalQuote.pricePerGram, resultQuote.pricePerGram)
        assertEquals(originalQuote.currency, resultQuote.currency)
        assertEquals(originalQuote.quotedAt, resultQuote.quotedAt)
        assertEquals(originalQuote.source, resultQuote.source)
        assertEquals(originalQuote.createdAt, resultQuote.createdAt)
    }

    @Test
    fun `should handle different metal types correctly`() {
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
            val quote = MetalQuote(
                id = UUID.randomUUID().toString(),
                metalType = metalType,
                pricePerGram = BigDecimal("10.00"),
                currency = Currency.getInstance("USD"),
                quotedAt = ZonedDateTime.now(),
                source = "Test",
                createdAt = ZonedDateTime.now()
            )

            val entity = MetalQuoteEntity.fromMetalQuote(quote)
            val result = entity.toMetalQuote()

            assertEquals(metalType, result.metalType)
        }
    }
}
