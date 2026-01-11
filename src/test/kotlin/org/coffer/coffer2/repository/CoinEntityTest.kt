package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CoinEntityTest {

    @Test
    fun `toCoin should convert entity to domain model correctly`() {
        // Given
        val entityId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()
        val lastPriceUpdate = ZonedDateTime.now()

        val entity = CoinEntity(
            id = entityId,
            title = "American Gold Eagle",
            denomination = BigDecimal("50.00"),
            currencyCode = "USD",
            yearOfMinting = 2023,
            issuerCountryCode = "US",
            mintMark = "W",
            grade = CoinGrade.UNCIRCULATED,
            type = CoinType.BULLION,
            notes = "Test notes",
            numistaId = "12345",
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("31.10"),
            purity = BigDecimal("999.9"),
            metalType = MetalType.GOLD,
            rarityScore = 50,
            createdAt = createdAt,
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87"),
            lastPriceUpdate = lastPriceUpdate
        )

        // When
        val coin = entity.toCoin()

        // Then
        assertEquals(entityId.toString(), coin.id)
        assertEquals("American Gold Eagle", coin.title)
        assertEquals(BigDecimal("50.00"), coin.denomination)
        assertEquals(Currency.getInstance("USD"), coin.currency)
        assertEquals(2023, coin.yearOfMinting.year)
        assertEquals(Locale.of("", "US"), coin.issuerCountry)
        assertEquals("W", coin.mintMark?.value)
        assertEquals(CoinGrade.UNCIRCULATED, coin.grade)
        assertEquals(CoinType.BULLION, coin.type)
        assertEquals("Test notes", coin.notes)
        assertEquals("12345", coin.numistaId)
        assertEquals(CoinShape.CIRCULAR, coin.shape)
        assertEquals(BigDecimal("31.10"), coin.weightInGrams)
        assertEquals(BigDecimal("999.9"), coin.purity)
        assertEquals(MetalType.GOLD, coin.metalType)
        assertEquals(50, coin.rarity?.score)
        assertEquals(createdAt, coin.createdAt)
        assertEquals(BigDecimal("32.70"), coin.diameterInMillimeters)
        assertEquals(BigDecimal("2.87"), coin.thicknessInMillimeters)
        assertEquals(lastPriceUpdate, coin.lastPriceUpdate)
    }

    @Test
    fun `fromCoin should convert domain model to entity correctly`() {
        // Given
        val createdAt = ZonedDateTime.now()
        val lastPriceUpdate = ZonedDateTime.now()

        val coin = Coin(
            id = UUID.randomUUID().toString(),
            title = "Canadian Maple Leaf",
            denomination = BigDecimal("5.00"),
            currency = Currency.getInstance("CAD"),
            yearOfMinting = YearOfMinting(2023),
            issuerCountry = Locale.of("", "CA"),
            mintMark = MintMark("C"),
            grade = CoinGrade.PROOF,
            type = CoinType.BULLION,
            notes = "Test coin",
            numistaId = "67890",
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("31.10"),
            purity = BigDecimal("999.9"),
            metalType = MetalType.GOLD,
            rarity = Rarity(75),
            createdAt = createdAt,
            diameterInMillimeters = BigDecimal("30.00"),
            thicknessInMillimeters = BigDecimal("2.50"),
            lastPriceUpdate = lastPriceUpdate
        )

        // When
        val entity = CoinEntity.fromCoin(coin)

        // Then
        assertEquals(coin.id, entity.id.toString())
        assertEquals("Canadian Maple Leaf", entity.title)
        assertEquals(BigDecimal("5.00"), entity.denomination)
        assertEquals("CAD", entity.currencyCode)
        assertEquals(2023, entity.yearOfMinting)
        assertEquals("CA", entity.issuerCountryCode)
        assertEquals("C", entity.mintMark)
        assertEquals(CoinGrade.PROOF, entity.grade)
        assertEquals(CoinType.BULLION, entity.type)
        assertEquals("Test coin", entity.notes)
        assertEquals("67890", entity.numistaId)
        assertEquals(CoinShape.CIRCULAR, entity.shape)
        assertEquals(BigDecimal("31.10"), entity.weightInGrams)
        assertEquals(BigDecimal("999.9"), entity.purity)
        assertEquals(MetalType.GOLD, entity.metalType)
        assertEquals(75, entity.rarityScore)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(BigDecimal("30.00"), entity.diameterInMillimeters)
        assertEquals(BigDecimal("2.50"), entity.thicknessInMillimeters)
        assertEquals(lastPriceUpdate, entity.lastPriceUpdate)
    }

    @Test
    fun `fromCoin should generate UUID when coin id is null`() {
        // Given
        val coin = Coin(
            id = null,
            title = "Test Coin",
            denomination = null,
            currency = Currency.getInstance("USD"),
            yearOfMinting = YearOfMinting(2023),
            issuerCountry = Locale.of("", "US"),
            mintMark = null,
            grade = null,
            type = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            weightInGrams = BigDecimal("5.67"),
            diameterInMillimeters = null,
            thicknessInMillimeters = null
        )

        // When
        val entity = CoinEntity.fromCoin(coin)

        // Then
        assertNotNull(entity.id)
    }

    @Test
    fun `round-trip conversion should preserve all fields`() {
        // Given
        val originalCoin = Coin(
            id = UUID.randomUUID().toString(),
            title = "Test Round-Trip Coin",
            denomination = BigDecimal("10.00"),
            currency = Currency.getInstance("EUR"),
            yearOfMinting = YearOfMinting(2022),
            issuerCountry = Locale.of("", "FR"),
            mintMark = MintMark("F"),
            grade = CoinGrade.VERY_FINE,
            type = CoinType.COMMEMORATIVE_CIRCULATION,
            notes = "Round-trip test",
            numistaId = "11111",
            shape = CoinShape.HEPTAGON,
            weightInGrams = BigDecimal("8.50"),
            purity = BigDecimal("925.0"),
            metalType = MetalType.SILVER,
            rarity = Rarity(25),
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = BigDecimal("28.50"),
            thicknessInMillimeters = BigDecimal("2.20"),
            lastPriceUpdate = ZonedDateTime.now()
        )

        // When
        val entity = CoinEntity.fromCoin(originalCoin)
        val resultCoin = entity.toCoin()

        // Then
        assertEquals(originalCoin.id, resultCoin.id)
        assertEquals(originalCoin.title, resultCoin.title)
        assertEquals(originalCoin.denomination, resultCoin.denomination)
        assertEquals(originalCoin.currency, resultCoin.currency)
        assertEquals(originalCoin.yearOfMinting.year, resultCoin.yearOfMinting.year)
        assertEquals(originalCoin.issuerCountry, resultCoin.issuerCountry)
        assertEquals(originalCoin.mintMark?.value, resultCoin.mintMark?.value)
        assertEquals(originalCoin.grade, resultCoin.grade)
        assertEquals(originalCoin.type, resultCoin.type)
        assertEquals(originalCoin.notes, resultCoin.notes)
        assertEquals(originalCoin.numistaId, resultCoin.numistaId)
        assertEquals(originalCoin.shape, resultCoin.shape)
        assertEquals(originalCoin.weightInGrams, resultCoin.weightInGrams)
        assertEquals(originalCoin.purity, resultCoin.purity)
        assertEquals(originalCoin.metalType, resultCoin.metalType)
        assertEquals(originalCoin.rarity?.score, resultCoin.rarity?.score)
    }

    @Test
    fun `toCoin should handle nullable fields correctly`() {
        // Given
        val entity = CoinEntity(
            id = UUID.randomUUID(),
            title = "Minimal Coin",
            denomination = null,
            currencyCode = "USD",
            yearOfMinting = 2023,
            issuerCountryCode = "US",
            mintMark = null,
            grade = null,
            type = CoinType.OTHER,
            notes = null,
            numistaId = null,
            shape = CoinShape.UNKNOWN,
            weightInGrams = BigDecimal("1.00"),
            purity = null,
            metalType = null,
            rarityScore = null,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = null,
            thicknessInMillimeters = null,
            lastPriceUpdate = null
        )

        // When
        val coin = entity.toCoin()

        // Then
        assertEquals(null, coin.denomination)
        assertEquals(null, coin.mintMark)
        assertEquals(null, coin.grade)
        assertEquals(null, coin.notes)
        assertEquals(null, coin.numistaId)
        assertEquals(null, coin.purity)
        assertEquals(null, coin.metalType)
        assertEquals(null, coin.rarity)
        assertEquals(null, coin.diameterInMillimeters)
        assertEquals(null, coin.thicknessInMillimeters)
        assertEquals(null, coin.lastPriceUpdate)
    }
}
