package org.coffer.coffer2.api

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CoinResponseTest {

    @Test
    fun `from should convert coin UUID to string correctly`() {
        // Given
        val coinId = UUID.randomUUID()
        val coin = Coin(
            id = coinId,
            title = "Test Coin",
            denomination = BigDecimal("50.00"),
            currency = Currency.getInstance("USD"),
            yearOfMinting = YearOfMinting(2023),
            issuerCountry = Locale.US,
            mintMark = null,
            grade = CoinGrade.UNCIRCULATED,
            type = CoinType.BULLION,
            notes = null,
            numistaId = null,
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("31.10"),
            purity = BigDecimal("999"),
            metalType = MetalType.GOLD,
            rarity = null,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87")
        )

        // When
        val response = CoinResponse.from(coin)

        // Then
        assertEquals(coinId.toString(), response.id)
        // Verify the string can be parsed back to UUID
        val parsedUuid = UUID.fromString(response.id)
        assertEquals(coinId, parsedUuid)
    }

    @Test
    fun `from should preserve all coin fields in response`() {
        // Given
        val coin = Coin(
            id = UUID.randomUUID(),
            title = "American Gold Eagle",
            denomination = BigDecimal("100.00"),
            currency = Currency.getInstance("EUR"),
            yearOfMinting = YearOfMinting(2024),
            issuerCountry = Locale.GERMANY,
            mintMark = MintMark("D"),
            grade = CoinGrade.PROOF,
            type = CoinType.COMMEMORATIVE_NON_CIRCULATION,
            notes = "Special commemorative coin",
            numistaId = "NUMISTA123",
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("15.55"),
            purity = BigDecimal("916.7"),
            metalType = MetalType.SILVER,
            rarity = Rarity(85),
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = BigDecimal("28.00"),
            thicknessInMillimeters = BigDecimal("2.50"),
            lastPriceUpdate = ZonedDateTime.now()
        )

        // When
        val response = CoinResponse.from(coin)

        // Then
        assertNotNull(response.id)
        assertEquals("American Gold Eagle", response.title)
        assertEquals(0, BigDecimal("100.00").compareTo(response.denomination))
        assertEquals("EUR", response.currency)
        assertEquals(2024, response.yearOfMinting)
        assertEquals("DE", response.issuerCountry) // Locale.GERMANY country code
        assertEquals("D", response.mintMark)
        assertEquals(CoinGrade.PROOF, response.grade)
        assertEquals(CoinType.COMMEMORATIVE_NON_CIRCULATION, response.type)
        assertEquals("Special commemorative coin", response.notes)
        assertEquals("NUMISTA123", response.numistaId)
        assertEquals(CoinShape.CIRCULAR, response.shape)
        assertEquals(0, BigDecimal("15.55").compareTo(response.weightInGrams))
        assertEquals(0, BigDecimal("916.7").compareTo(response.purity))
        assertEquals(MetalType.SILVER, response.metalType)
        assertEquals(85, response.rarity?.score)
        assertEquals(0, BigDecimal("28.00").compareTo(response.diameterInMillimeters))
        assertEquals(0, BigDecimal("2.50").compareTo(response.thicknessInMillimeters))
        assertNotNull(response.createdAt)
        assertNotNull(response.lastPriceUpdate)
    }

    @Test
    fun `from should handle nullable fields correctly`() {
        // Given
        val coin = Coin(
            id = UUID.randomUUID(),
            title = "Minimal Coin",
            denomination = null,
            currency = Currency.getInstance("USD"),
            yearOfMinting = YearOfMinting(2023),
            issuerCountry = Locale.US,
            mintMark = null,
            grade = null,
            type = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            shape = CoinShape.UNKNOWN,
            weightInGrams = BigDecimal("5.00"),
            purity = null,
            metalType = null,
            rarity = null,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = null,
            thicknessInMillimeters = null
        )

        // When
        val response = CoinResponse.from(coin)

        // Then
        assertNotNull(response.id)
        assertEquals("Minimal Coin", response.title)
        assertEquals(null, response.denomination)
        assertEquals(null, response.mintMark)
        assertEquals(null, response.grade)
        assertEquals(null, response.notes)
        assertEquals(null, response.numistaId)
        assertEquals(null, response.purity)
        assertEquals(null, response.metalType)
        assertEquals(null, response.rarity)
        assertEquals(null, response.diameterInMillimeters)
        assertEquals(null, response.thicknessInMillimeters)
        assertEquals(null, response.lastPriceUpdate)
    }
}
