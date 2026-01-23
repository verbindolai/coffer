package org.coffer.coffer2.util

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.YearOfMinting
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MetalValuationCalculatorTest {

    private fun createCoin(
        metalType: MetalType? = MetalType.GOLD,
        purity: BigDecimal? = BigDecimal("999"),
        weightInGrams: BigDecimal = BigDecimal("31.1035"),
        quantity: Int = 1
    ) = Coin(
        id = UUID.randomUUID(),
        title = "Test Coin",
        denomination = BigDecimal("1"),
        currency = Currency.getInstance("USD"),
        yearOfMinting = YearOfMinting(2024),
        issuerCountry = Locale.of("", "US"),
        mintMark = null,
        grade = null,
        type = CoinType.BULLION,
        notes = null,
        numistaId = null,
        shape = CoinShape.CIRCULAR,
        weightInGrams = weightInGrams,
        purity = purity,
        metalType = metalType,
        rarity = null,
        createdAt = ZonedDateTime.now(),
        diameterInMillimeters = null,
        thicknessInMillimeters = null,
        quantity = quantity
    )

    // ==================== pureMetalMass Tests ====================

    @Test
    fun `pureMetalMass calculates correctly for 999 purity`() {
        val result = MetalValuationCalculator.pureMetalMass(
            BigDecimal("31.1035"),
            BigDecimal("999")
        )
        // 31.1035 * 999 / 1000 = 31.072397 (rounded to 6 dp)
        assertEquals(BigDecimal("31.072397"), result)
    }

    @Test
    fun `pureMetalMass calculates correctly for 916 purity`() {
        val result = MetalValuationCalculator.pureMetalMass(
            BigDecimal("31.1035"),
            BigDecimal("916")
        )
        // 31.1035 * 916 / 1000 = 28.490806 (rounded to 6 dp)
        assertEquals(BigDecimal("28.490806"), result)
    }

    @Test
    fun `pureMetalMass calculates correctly for 500 purity`() {
        val result = MetalValuationCalculator.pureMetalMass(
            BigDecimal("10.0"),
            BigDecimal("500")
        )
        // 10.0 * 500 / 1000 = 5.0
        assertEquals(BigDecimal("5.000000"), result)
    }

    // ==================== coinMetalValue Tests ====================

    @Test
    fun `coinMetalValue returns null when coin has no metal type`() {
        val coin = createCoin(metalType = null)
        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        assertNull(MetalValuationCalculator.coinMetalValue(coin, prices))
    }

    @Test
    fun `coinMetalValue returns null when coin has no purity`() {
        val coin = createCoin(purity = null)
        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        assertNull(MetalValuationCalculator.coinMetalValue(coin, prices))
    }

    @Test
    fun `coinMetalValue returns null when no price for metal type`() {
        val coin = createCoin(metalType = MetalType.GOLD)
        val prices = mapOf(MetalType.SILVER to BigDecimal("0.85"))

        assertNull(MetalValuationCalculator.coinMetalValue(coin, prices))
    }

    @Test
    fun `coinMetalValue calculates correctly for single coin`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035"),
            quantity = 1
        )
        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = MetalValuationCalculator.coinMetalValue(coin, prices)

        assertNotNull(result)
        // pureMass = 31.1035 * 999 / 1000 = 31.072397
        // value = 31.072397 * 65.00 = 2019.705805
        assertEquals(0, BigDecimal("2019.705805").compareTo(result))
    }

    @Test
    fun `coinMetalValue applies quantity multiplier`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035"),
            quantity = 3
        )
        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = MetalValuationCalculator.coinMetalValue(coin, prices)

        assertNotNull(result)
        // pureMass = 31.072397, totalPureMetal = 31.072397 * 3 = 93.217191
        // value = 93.217191 * 65.00 = 6059.117415
        assertEquals(0, BigDecimal("6059.117415").compareTo(result))
    }

    // ==================== aggregateMetalValues Tests ====================

    @Test
    fun `aggregateMetalValues returns zeros for empty list`() {
        val result = MetalValuationCalculator.aggregateMetalValues(
            emptyList(),
            mapOf(MetalType.GOLD to BigDecimal("65.00"))
        )

        assertEquals(BigDecimal("0.00"), result.totalValue)
        assertEquals(BigDecimal("0.000000"), result.goldGrams)
        assertEquals(BigDecimal("0.000000"), result.silverGrams)
        assertEquals(BigDecimal("0.000000"), result.platinumGrams)
    }

    @Test
    fun `aggregateMetalValues returns zeros when no prices available`() {
        val coins = listOf(createCoin(metalType = MetalType.GOLD))
        val result = MetalValuationCalculator.aggregateMetalValues(coins, emptyMap())

        assertEquals(BigDecimal("0.00"), result.totalValue)
        assertEquals(BigDecimal("0.000000"), result.goldGrams)
    }

    @Test
    fun `aggregateMetalValues aggregates single gold coin`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )
        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = MetalValuationCalculator.aggregateMetalValues(listOf(coin), prices)

        assertEquals(BigDecimal("31.072397"), result.goldGrams)
        assertEquals(BigDecimal("0.000000"), result.silverGrams)
        assertEquals(BigDecimal("0.000000"), result.platinumGrams)
        assertEquals(BigDecimal("2019.71"), result.totalValue)
    }

    @Test
    fun `aggregateMetalValues aggregates multiple metal types`() {
        val goldCoin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )
        val silverCoin = createCoin(
            metalType = MetalType.SILVER,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )

        val prices = mapOf(
            MetalType.GOLD to BigDecimal("65.00"),
            MetalType.SILVER to BigDecimal("0.85")
        )

        val result = MetalValuationCalculator.aggregateMetalValues(listOf(goldCoin, silverCoin), prices)

        assertEquals(BigDecimal("31.072397"), result.goldGrams)
        assertEquals(BigDecimal("31.072397"), result.silverGrams)
        assertEquals(BigDecimal("0.000000"), result.platinumGrams)
        // gold: 31.072397 * 65 = 2019.705805, silver: 31.072397 * 0.85 = 26.41153745
        // total = 2046.11734245, rounded to 2046.12
        assertEquals(BigDecimal("2046.12"), result.totalValue)
    }

    @Test
    fun `aggregateMetalValues skips coins without metal type or purity`() {
        val validCoin = createCoin(metalType = MetalType.GOLD, purity = BigDecimal("999"))
        val noMetalCoin = createCoin(metalType = null, purity = BigDecimal("999"))
        val noPurityCoin = createCoin(metalType = MetalType.GOLD, purity = null)

        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))
        val result = MetalValuationCalculator.aggregateMetalValues(
            listOf(validCoin, noMetalCoin, noPurityCoin),
            prices
        )

        assertEquals(BigDecimal("31.072397"), result.goldGrams)
    }

    @Test
    fun `aggregateMetalValues applies coin filter`() {
        val cutoff = ZonedDateTime.now()
        val oldCoin = createCoin(metalType = MetalType.GOLD, purity = BigDecimal("999"))
        val newCoin = createCoin(metalType = MetalType.GOLD, purity = BigDecimal("999"))

        val prices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        // Filter that excludes one coin
        val result = MetalValuationCalculator.aggregateMetalValues(
            listOf(oldCoin, newCoin),
            prices
        ) { it == oldCoin }

        assertEquals(BigDecimal("31.072397"), result.goldGrams)
    }

    @Test
    fun `aggregateMetalValues does not track nickel or base metal grams`() {
        val nickelCoin = createCoin(
            metalType = MetalType.NICKEL,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("10.0")
        )
        val prices = mapOf(MetalType.NICKEL to BigDecimal("0.02"))

        val result = MetalValuationCalculator.aggregateMetalValues(listOf(nickelCoin), prices)

        assertEquals(BigDecimal("0.000000"), result.goldGrams)
        assertEquals(BigDecimal("0.000000"), result.silverGrams)
        assertEquals(BigDecimal("0.000000"), result.platinumGrams)
        // But value should still be computed: 10 * 999/1000 * 0.02 = 0.1998
        assertEquals(BigDecimal("0.20"), result.totalValue)
    }

    @Test
    fun `aggregateMetalValues handles platinum coins`() {
        val platinumCoin = createCoin(
            metalType = MetalType.PLATINUM,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )
        val prices = mapOf(MetalType.PLATINUM to BigDecimal("30.00"))

        val result = MetalValuationCalculator.aggregateMetalValues(listOf(platinumCoin), prices)

        assertEquals(BigDecimal("31.072397"), result.platinumGrams)
        assertEquals(BigDecimal("0.000000"), result.goldGrams)
        assertEquals(BigDecimal("0.000000"), result.silverGrams)
    }
}
