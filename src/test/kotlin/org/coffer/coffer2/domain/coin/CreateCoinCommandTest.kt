package org.coffer.coffer2.domain.coin

import org.coffer.coffer2.domain.MetalType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class CreateCoinCommandTest {

    @Test
    fun `toCoin should generate a valid UUID for new coin`() {
        // Given
        val command = CreateCoinCommand(
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
            weightInGrams = BigDecimal("31.10"),
            purity = BigDecimal("999"),
            metalType = MetalType.GOLD,
            rarity = null,
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87")
        )

        // When
        val coin = command.toCoin()

        // Then
        assertNotNull(coin.id)
        // Verify it's a valid UUID by parsing it
        val parsedUuid = UUID.fromString(coin.id.toString())
        assertNotNull(parsedUuid)
    }

    @Test
    fun `toCoin should generate unique UUIDs for each call`() {
        // Given
        val command = CreateCoinCommand(
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
            weightInGrams = BigDecimal("31.10"),
            metalType = MetalType.GOLD,
            diameterInMillimeters = null,
            thicknessInMillimeters = null
        )

        // When
        val coin1 = command.toCoin()
        val coin2 = command.toCoin()

        // Then
        assertNotEquals(coin1.id, coin2.id, "Each call to toCoin should generate a unique UUID")
    }

    @Test
    fun `toCoin should preserve all command fields in coin`() {
        // Given
        val command = CreateCoinCommand(
            title = "Test Gold Coin",
            denomination = BigDecimal("100.00"),
            currency = Currency.getInstance("EUR"),
            yearOfMinting = YearOfMinting(2024),
            issuerCountry = Locale.GERMANY,
            mintMark = MintMark("D"),
            grade = CoinGrade.PROOF,
            type = CoinType.COMMEMORATIVE_NON_CIRCULATION,
            notes = "Special edition",
            numistaId = "TEST456",
            weightInGrams = BigDecimal("15.55"),
            purity = BigDecimal("916.7"),
            metalType = MetalType.SILVER,
            rarity = Rarity(75),
            diameterInMillimeters = BigDecimal("28.00"),
            thicknessInMillimeters = BigDecimal("2.50")
        )

        // When
        val coin = command.toCoin()

        // Then
        assertEquals("Test Gold Coin", coin.title)
        assertEquals(0, BigDecimal("100.00").compareTo(coin.denomination))
        assertEquals(Currency.getInstance("EUR"), coin.currency)
        assertEquals(2024, coin.yearOfMinting.year)
        assertEquals(Locale.GERMANY, coin.issuerCountry)
        assertEquals("D", coin.mintMark?.value)
        assertEquals(CoinGrade.PROOF, coin.grade)
        assertEquals(CoinType.COMMEMORATIVE_NON_CIRCULATION, coin.type)
        assertEquals("Special edition", coin.notes)
        assertEquals("TEST456", coin.numistaId)
        assertEquals(0, BigDecimal("15.55").compareTo(coin.weightInGrams))
        assertEquals(0, BigDecimal("916.7").compareTo(coin.purity))
        assertEquals(MetalType.SILVER, coin.metalType)
        assertEquals(75, coin.rarity?.score)
        assertEquals(0, BigDecimal("28.00").compareTo(coin.diameterInMillimeters))
        assertEquals(0, BigDecimal("2.50").compareTo(coin.thicknessInMillimeters))
    }
}
