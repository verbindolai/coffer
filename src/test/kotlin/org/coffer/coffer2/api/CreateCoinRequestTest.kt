package org.coffer.coffer2.api

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CreateCoinRequestTest {

    @Test
    fun `toCoin should convert all fields correctly including fixed bugs`() {
        // Given
        val request = CreateCoinRequest(
            title = "Test Coin",
            denomination = BigDecimal("50.00"),
            year = 2023,
            countryCode = "US",
            currency = "USD",
            mintMark = "W",
            grade = CoinGrade.UNCIRCULATED,
            coinType = CoinType.BULLION,
            notes = "Test",
            numistaId = "TEST",
            metalType = MetalType.GOLD,
            weightInGrams = BigDecimal("31.10"),
            purity = 999,
            quantity = 1,
            rarityScore = 50,
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87")
        )

        // When
        val coin = request.toCommand()

        // Then - verify critical conversions
        assertEquals("US", coin.issuerCountry.country) // Bug fix: Locale.of("", countryCode)
        assertEquals(MetalType.GOLD, coin.metalType) // Bug fix: missing metalType field
        assertEquals("W", coin.mintMark?.value)
        assertEquals(0, BigDecimal("999").compareTo(coin.purity))
    }

    @Test
    fun `toCoin should handle nullable fields`() {
        // Given
        val request = CreateCoinRequest(
            title = "Minimal",
            denomination = null,
            year = 2023,
            countryCode = "US",
            currency = "USD",
            mintMark = null,
            grade = CoinGrade.FINE,
            coinType = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            metalType = null,
            weightInGrams = BigDecimal("5.00"),
            purity = null,
            quantity = 1,
            rarityScore = null,
            diameterInMillimeters = null,
            thicknessInMillimeters = null
        )

        // When
        val coin = request.toCommand()

        // Then
        assertNull(coin.denomination)
        assertNull(coin.mintMark)
        assertNull(coin.metalType)
        assertNull(coin.purity)
        assertNotNull(coin.title)
    }
}
