package org.coffer.coffer2.util

import org.coffer.coffer2.application.portfolio.CoinIssueInfo
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
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

class CollectorValuationCalculatorTest {

    private fun createCoin(
        id: UUID = UUID.randomUUID(),
        metalType: MetalType? = MetalType.GOLD,
        purity: BigDecimal? = BigDecimal("999"),
        weightInGrams: BigDecimal = BigDecimal("31.1035"),
        grade: CoinGrade? = CoinGrade.UNCIRCULATED,
        quantity: Int = 1
    ) = Coin(
        id = id,
        title = "Test Coin",
        denomination = BigDecimal("1"),
        currency = Currency.getInstance("USD"),
        yearOfMinting = YearOfMinting(2024),
        issuerCountry = Locale.of("", "US"),
        mintMark = null,
        grade = grade,
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

    @Test
    fun `aggregateCollectorValues returns zeros for empty inputs`() {
        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = emptyList(),
            priceMap = emptyMap(),
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        )

        assertEquals(BigDecimal("0.00"), result.minValue)
        assertEquals(BigDecimal("0.00"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues uses exact price for single issue match`() {
        val coin = createCoin(grade = CoinGrade.UNCIRCULATED)
        val issueId = UUID.randomUUID()
        val info = CoinIssueInfo(
            coin = coin,
            issueIds = listOf(issueId),
            grade = CoinGrade.UNCIRCULATED,
            isExactMatch = true
        )

        val priceMap = mapOf((issueId to CoinGrade.UNCIRCULATED) to BigDecimal("2500.00"))

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = listOf(info),
            priceMap = priceMap,
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        )

        assertEquals(BigDecimal("2500.00"), result.minValue)
        assertEquals(BigDecimal("2500.00"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues applies quantity for exact match`() {
        val coin = createCoin(grade = CoinGrade.UNCIRCULATED, quantity = 3)
        val issueId = UUID.randomUUID()
        val info = CoinIssueInfo(
            coin = coin,
            issueIds = listOf(issueId),
            grade = CoinGrade.UNCIRCULATED,
            isExactMatch = true
        )

        val priceMap = mapOf((issueId to CoinGrade.UNCIRCULATED) to BigDecimal("1000.00"))

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = listOf(info),
            priceMap = priceMap,
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        )

        assertEquals(BigDecimal("3000.00"), result.minValue)
        assertEquals(BigDecimal("3000.00"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues uses min and max for multiple issues`() {
        val coin = createCoin(grade = CoinGrade.UNCIRCULATED)
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()
        val info = CoinIssueInfo(
            coin = coin,
            issueIds = listOf(issueId1, issueId2),
            grade = CoinGrade.UNCIRCULATED,
            isExactMatch = false
        )

        val priceMap = mapOf(
            (issueId1 to CoinGrade.UNCIRCULATED) to BigDecimal("2400.00"),
            (issueId2 to CoinGrade.UNCIRCULATED) to BigDecimal("2600.00")
        )

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = listOf(info),
            priceMap = priceMap,
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        )

        assertEquals(BigDecimal("2400.00"), result.minValue)
        assertEquals(BigDecimal("2600.00"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues falls back to metal value when no collector price for exact match`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035"),
            grade = CoinGrade.UNCIRCULATED
        )
        val issueId = UUID.randomUUID()
        val info = CoinIssueInfo(
            coin = coin,
            issueIds = listOf(issueId),
            grade = CoinGrade.UNCIRCULATED,
            isExactMatch = true
        )

        val metalPrices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = listOf(info),
            priceMap = emptyMap(),
            coinsWithoutCollector = emptyList(),
            metalPrices = metalPrices
        )

        // 31.1035 * 999 / 1000 = 31.072397g * 65.00 = 2019.705805
        assertEquals(BigDecimal("2019.71"), result.minValue)
        assertEquals(BigDecimal("2019.71"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues falls back to metal value when no prices for multi-issue`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035"),
            grade = CoinGrade.UNCIRCULATED
        )
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()
        val info = CoinIssueInfo(
            coin = coin,
            issueIds = listOf(issueId1, issueId2),
            grade = CoinGrade.UNCIRCULATED,
            isExactMatch = false
        )

        val metalPrices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = listOf(info),
            priceMap = emptyMap(),
            coinsWithoutCollector = emptyList(),
            metalPrices = metalPrices
        )

        assertEquals(BigDecimal("2019.71"), result.minValue)
        assertEquals(BigDecimal("2019.71"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues includes coins without collector data using metal fallback`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )

        val metalPrices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = emptyList(),
            priceMap = emptyMap(),
            coinsWithoutCollector = listOf(coin),
            metalPrices = metalPrices
        )

        assertEquals(BigDecimal("2019.71"), result.minValue)
        assertEquals(BigDecimal("2019.71"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues applies coinFilter`() {
        val coin1 = createCoin(grade = CoinGrade.UNCIRCULATED)
        val coin2 = createCoin(grade = CoinGrade.UNCIRCULATED)
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()

        val infos = listOf(
            CoinIssueInfo(coin = coin1, issueIds = listOf(issueId1), grade = CoinGrade.UNCIRCULATED, isExactMatch = true),
            CoinIssueInfo(coin = coin2, issueIds = listOf(issueId2), grade = CoinGrade.UNCIRCULATED, isExactMatch = true)
        )

        val priceMap = mapOf(
            (issueId1 to CoinGrade.UNCIRCULATED) to BigDecimal("1000.00"),
            (issueId2 to CoinGrade.UNCIRCULATED) to BigDecimal("2000.00")
        )

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = infos,
            priceMap = priceMap,
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        ) { it == coin1 }

        assertEquals(BigDecimal("1000.00"), result.minValue)
        assertEquals(BigDecimal("1000.00"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues applies coinFilter to coins without collector`() {
        val coin1 = createCoin(metalType = MetalType.GOLD, purity = BigDecimal("999"), weightInGrams = BigDecimal("31.1035"))
        val coin2 = createCoin(metalType = MetalType.GOLD, purity = BigDecimal("999"), weightInGrams = BigDecimal("31.1035"))

        val metalPrices = mapOf(MetalType.GOLD to BigDecimal("65.00"))

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = emptyList(),
            priceMap = emptyMap(),
            coinsWithoutCollector = listOf(coin1, coin2),
            metalPrices = metalPrices
        ) { it == coin1 }

        assertEquals(BigDecimal("2019.71"), result.minValue)
        assertEquals(BigDecimal("2019.71"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues aggregates multiple coins`() {
        val coin1 = createCoin(grade = CoinGrade.UNCIRCULATED)
        val coin2 = createCoin(grade = CoinGrade.VERY_FINE)
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()

        val infos = listOf(
            CoinIssueInfo(coin = coin1, issueIds = listOf(issueId1), grade = CoinGrade.UNCIRCULATED, isExactMatch = true),
            CoinIssueInfo(coin = coin2, issueIds = listOf(issueId2), grade = CoinGrade.VERY_FINE, isExactMatch = true)
        )

        val priceMap = mapOf(
            (issueId1 to CoinGrade.UNCIRCULATED) to BigDecimal("1500.00"),
            (issueId2 to CoinGrade.VERY_FINE) to BigDecimal("800.00")
        )

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = infos,
            priceMap = priceMap,
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        )

        assertEquals(BigDecimal("2300.00"), result.minValue)
        assertEquals(BigDecimal("2300.00"), result.maxValue)
    }

    @Test
    fun `aggregateCollectorValues returns zeros when no metal prices and no collector prices`() {
        val coin = createCoin(metalType = null, purity = null, grade = CoinGrade.UNCIRCULATED)
        val issueId = UUID.randomUUID()
        val info = CoinIssueInfo(
            coin = coin,
            issueIds = listOf(issueId),
            grade = CoinGrade.UNCIRCULATED,
            isExactMatch = true
        )

        val result = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos = listOf(info),
            priceMap = emptyMap(),
            coinsWithoutCollector = emptyList(),
            metalPrices = emptyMap()
        )

        assertEquals(BigDecimal("0.00"), result.minValue)
        assertEquals(BigDecimal("0.00"), result.maxValue)
    }
}
