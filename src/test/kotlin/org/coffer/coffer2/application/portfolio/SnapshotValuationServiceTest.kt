package org.coffer.coffer2.application.portfolio

import io.mockk.every
import io.mockk.mockk
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.repository.PortfolioSnapshotEntity
import org.coffer.coffer2.repository.PortfolioSnapshotRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SnapshotValuationServiceTest {

    private val portfolioSnapshotRepository = mockk<PortfolioSnapshotRepository>()
    private val liveValuationService = mockk<LiveValuationService>()

    private val service = SnapshotValuationService(
        portfolioSnapshotRepository,
        liveValuationService
    )

    private fun createCoin(
        metalType: MetalType? = MetalType.GOLD,
        purity: BigDecimal? = BigDecimal("999"),
        weightInGrams: BigDecimal = BigDecimal("31.1035")
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
        quantity = 1
    )

    private fun createSnapshotEntity(
        createdAt: ZonedDateTime = ZonedDateTime.now().minusDays(1),
        metalValue: BigDecimal? = BigDecimal("2000.00"),
        goldGrams: BigDecimal? = BigDecimal("31.0"),
        silverGrams: BigDecimal? = BigDecimal("0.0"),
        platinumGrams: BigDecimal? = BigDecimal("0.0"),
        collectorValueMin: BigDecimal? = BigDecimal("2500.00"),
        collectorValueMax: BigDecimal? = BigDecimal("2700.00")
    ) = PortfolioSnapshotEntity(
        id = UUID.randomUUID(),
        snapshotDate = createdAt.toLocalDate(),
        currencyCode = "EUR",
        totalCoins = 1,
        totalQuantity = 1,
        metalValue = metalValue,
        goldGrams = goldGrams,
        silverGrams = silverGrams,
        platinumGrams = platinumGrams,
        collectorValueMin = collectorValueMin,
        collectorValueMax = collectorValueMax,
        createdAt = createdAt
    )

    @Test
    fun `computeValuation returns nulls when no snapshots and no coins`() {
        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns emptyList()
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (metalResult, collectorResult) = service.computeValuation(
            emptyList(), ValuationTimeframe.WEEK_1
        )

        assertNull(metalResult)
        assertNull(collectorResult)
    }

    @Test
    fun `computeValuation transforms snapshot to metal data points`() {
        val snapshot = createSnapshotEntity(
            createdAt = ZonedDateTime.now().minusDays(3),
            metalValue = BigDecimal("2000.00"),
            goldGrams = BigDecimal("31.0"),
            silverGrams = BigDecimal("0.0"),
            platinumGrams = BigDecimal("0.0")
        )
        val coins = listOf(createCoin())

        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (metalResult, _) = service.computeValuation(coins, ValuationTimeframe.WEEK_1)

        assertNotNull(metalResult)
        assertEquals(1, metalResult.dataPoints.size)
        assertEquals(BigDecimal("2000.00"), metalResult.dataPoints[0].totalValue)
    }

    @Test
    fun `computeValuation transforms snapshot to collector data points`() {
        val snapshot = createSnapshotEntity(
            createdAt = ZonedDateTime.now().minusDays(3),
            collectorValueMin = BigDecimal("2500.00"),
            collectorValueMax = BigDecimal("2700.00")
        )
        val coins = listOf(createCoin())

        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (_, collectorResult) = service.computeValuation(coins, ValuationTimeframe.WEEK_1)

        assertNotNull(collectorResult)
        assertEquals(1, collectorResult.dataPoints.size)
        assertEquals(BigDecimal("2500.00"), collectorResult.dataPoints[0].minValue)
        assertEquals(BigDecimal("2700.00"), collectorResult.dataPoints[0].maxValue)
    }

    @Test
    fun `computeValuation appends live metal point`() {
        val snapshot = createSnapshotEntity(createdAt = ZonedDateTime.now().minusDays(3))
        val coins = listOf(createCoin())
        val livePoint = PortfolioMetalPoint(
            timestamp = ZonedDateTime.now(),
            totalValue = BigDecimal("2100.00"),
            goldGrams = BigDecimal("32.0"),
            silverGrams = BigDecimal("0.0"),
            platinumGrams = BigDecimal("0.0")
        )

        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns livePoint
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (metalResult, _) = service.computeValuation(coins, ValuationTimeframe.WEEK_1)

        assertNotNull(metalResult)
        assertEquals(2, metalResult.dataPoints.size)
        assertEquals(BigDecimal("2100.00"), metalResult.dataPoints.last().totalValue)
    }

    @Test
    fun `computeValuation appends live collector point`() {
        val snapshot = createSnapshotEntity(createdAt = ZonedDateTime.now().minusDays(3))
        val coins = listOf(createCoin())
        val livePoint = PortfolioCollectorPoint(
            timestamp = ZonedDateTime.now(),
            minValue = BigDecimal("2600.00"),
            maxValue = BigDecimal("2800.00")
        )

        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns livePoint

        val (_, collectorResult) = service.computeValuation(coins, ValuationTimeframe.WEEK_1)

        assertNotNull(collectorResult)
        assertEquals(2, collectorResult.dataPoints.size)
        assertEquals(BigDecimal("2600.00"), collectorResult.dataPoints.last().minValue)
    }

    @Test
    fun `computeValuation uses findAllOrderByCreatedAtAsc for MAX timeframe`() {
        val snapshot = createSnapshotEntity(createdAt = ZonedDateTime.now().minusYears(2))
        val coins = listOf(createCoin())

        every { portfolioSnapshotRepository.findAllOrderByCreatedAtAsc() } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (metalResult, _) = service.computeValuation(coins, ValuationTimeframe.MAX)

        assertNotNull(metalResult)
        assertEquals(1, metalResult.dataPoints.size)
    }

    @Test
    fun `computeValuation skips null metal value snapshots`() {
        val snapshot = createSnapshotEntity(
            createdAt = ZonedDateTime.now().minusDays(3),
            metalValue = null
        )
        val coins = listOf(createCoin())

        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (metalResult, _) = service.computeValuation(coins, ValuationTimeframe.WEEK_1)

        assertNull(metalResult)
    }

    @Test
    fun `computeValuation skips snapshots without collector values`() {
        val snapshot = createSnapshotEntity(
            createdAt = ZonedDateTime.now().minusDays(3),
            collectorValueMin = null,
            collectorValueMax = null
        )
        val coins = listOf(createCoin())

        every { portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()) } returns listOf(snapshot)
        every { liveValuationService.computeLiveMetalPoint(any(), any()) } returns null
        every { liveValuationService.computeLiveCollectorPoint(any(), any()) } returns null

        val (_, collectorResult) = service.computeValuation(coins, ValuationTimeframe.WEEK_1)

        assertNull(collectorResult)
    }
}
