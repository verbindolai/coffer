package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

class CoinEntityTest {

    @Test
    fun `should convert entity to domain and back without losing data`() {
        // Given - entity with all fields populated
        val originalEntity = CoinEntity(
            id = UUID.randomUUID(),
            title = "American Gold Eagle",
            denomination = BigDecimal("50.00"),
            currencyCode = "USD",
            yearOfMinting = 2023,
            issuerCountryCode = "US",
            mintMark = "W",
            grade = CoinGrade.UNCIRCULATED,
            type = CoinType.BULLION,
            notes = "Test coin",
            numistaId = "TEST123",
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("31.10"),
            purity = BigDecimal("999.9"),
            metalType = MetalType.GOLD,
            rarityScore = 50,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87"),
            lastPriceUpdate = ZonedDateTime.now()
        )

        // When - convert to domain and back
        val coin = originalEntity.toCoin()
        val roundTripEntity = CoinEntity.fromCoin(coin)

        // Then - verify key fields match
        assertEquals(originalEntity.id, UUID.fromString(roundTripEntity.id.toString()))
        assertEquals(originalEntity.title, roundTripEntity.title)
        assertEquals(0, originalEntity.denomination!!.compareTo(roundTripEntity.denomination))
        assertEquals(originalEntity.currencyCode, roundTripEntity.currencyCode)
        assertEquals(originalEntity.yearOfMinting, roundTripEntity.yearOfMinting)
        assertEquals(originalEntity.metalType, roundTripEntity.metalType)
    }
}
