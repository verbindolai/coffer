package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.MetalType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

class MetalQuoteEntityTest {

    @Test
    fun `should convert entity to domain and back without losing data`() {
        // Given - entity with all fields populated
        val originalEntity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("65.50"),
            currencyCode = "USD",
            quotedAt = ZonedDateTime.now(),
            source = "SWISSQUOTE",
            createdAt = ZonedDateTime.now()
        )

        // When - convert to domain and back
        val metalQuote = originalEntity.toMetalQuote()
        val roundTripEntity = MetalQuoteEntity.fromMetalQuote(metalQuote)

        // Then - verify key fields match
        assertEquals(originalEntity.id, UUID.fromString(roundTripEntity.id.toString()))
        assertEquals(originalEntity.metalType, roundTripEntity.metalType)
        assertEquals(0, originalEntity.pricePerGram.compareTo(roundTripEntity.pricePerGram))
        assertEquals(originalEntity.currencyCode, roundTripEntity.currencyCode)
        assertEquals(originalEntity.source, roundTripEntity.source)
    }
}
