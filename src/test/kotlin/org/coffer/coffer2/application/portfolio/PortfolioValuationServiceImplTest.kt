package org.coffer.coffer2.application.portfolio

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
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

class PortfolioValuationServiceImplTest {

    private val coinRepositoryAdapter = mockk<CoinRepositoryAdapter>()
    private val realTimeMetalValuationService = mockk<RealTimeMetalValuationService>()
    private val realTimeCollectorValuationService = mockk<RealTimeCollectorValuationService>()
    private val snapshotValuationService = mockk<SnapshotValuationService>()

    private val service = PortfolioValuationServiceImpl(
        coinRepositoryAdapter,
        realTimeMetalValuationService,
        realTimeCollectorValuationService,
        snapshotValuationService
    )

    private fun createCoin() = Coin(
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
        weightInGrams = BigDecimal("31.1035"),
        purity = BigDecimal("999"),
        metalType = MetalType.GOLD,
        rarity = null,
        createdAt = ZonedDateTime.now(),
        diameterInMillimeters = null,
        thicknessInMillimeters = null,
        quantity = 1
    )

    @Test
    fun `getValuation returns empty result when no coins`() {
        every { coinRepositoryAdapter.findAll() } returns emptyList()

        val result = service.getValuation(ValuationTimeframe.HOUR_1)

        assertEquals(ValuationTimeframe.HOUR_1, result.timeframe)
        assertEquals("EUR", result.currency)
        assertNull(result.metalValuation)
        assertNull(result.collectorValuation)
    }

    @Test
    fun `getValuation routes HOUR_1 to real-time services`() {
        val coins = listOf(createCoin())
        val metalResult = PortfolioMetalValuationResult(listOf(
            PortfolioMetalPoint(ZonedDateTime.now(), BigDecimal("2000"), BigDecimal("31"), BigDecimal.ZERO, BigDecimal.ZERO)
        ))
        val collectorResult = PortfolioCollectorValuationResult(listOf(
            PortfolioCollectorPoint(ZonedDateTime.now(), null, BigDecimal("2500"), BigDecimal("2700"))
        ))

        every { coinRepositoryAdapter.findAll() } returns coins
        every { realTimeMetalValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.HOUR_1) } returns metalResult
        every { realTimeCollectorValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.HOUR_1) } returns collectorResult

        val result = service.getValuation(ValuationTimeframe.HOUR_1)

        assertEquals(ValuationTimeframe.HOUR_1, result.timeframe)
        assertEquals("EUR", result.currency)
        assertNotNull(result.metalValuation)
        assertNotNull(result.collectorValuation)
        verify { realTimeMetalValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.HOUR_1) }
        verify { realTimeCollectorValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.HOUR_1) }
    }

    @Test
    fun `getValuation routes DAY_1 to real-time services`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { realTimeMetalValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.DAY_1) } returns null
        every { realTimeCollectorValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.DAY_1) } returns null

        val result = service.getValuation(ValuationTimeframe.DAY_1)

        assertEquals(ValuationTimeframe.DAY_1, result.timeframe)
        verify { realTimeMetalValuationService.computeTimeSeries(coins, any(), ValuationTimeframe.DAY_1) }
    }

    @Test
    fun `getValuation routes WEEK_1 to snapshot service`() {
        val coins = listOf(createCoin())
        val metalResult = PortfolioMetalValuationResult(listOf(
            PortfolioMetalPoint(ZonedDateTime.now(), BigDecimal("2000"), BigDecimal("31"), BigDecimal.ZERO, BigDecimal.ZERO)
        ))

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.WEEK_1) } returns (metalResult to null)

        val result = service.getValuation(ValuationTimeframe.WEEK_1)

        assertEquals(ValuationTimeframe.WEEK_1, result.timeframe)
        assertNotNull(result.metalValuation)
        assertNull(result.collectorValuation)
        verify { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.WEEK_1) }
    }

    @Test
    fun `getValuation routes MONTH_1 to snapshot service`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.MONTH_1) } returns (null to null)

        val result = service.getValuation(ValuationTimeframe.MONTH_1)

        assertEquals(ValuationTimeframe.MONTH_1, result.timeframe)
        verify { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.MONTH_1) }
    }

    @Test
    fun `getValuation routes YEAR_1 to snapshot service`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.YEAR_1) } returns (null to null)

        val result = service.getValuation(ValuationTimeframe.YEAR_1)

        verify { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.YEAR_1) }
    }

    @Test
    fun `getValuation routes MAX to snapshot service`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.MAX) } returns (null to null)

        val result = service.getValuation(ValuationTimeframe.MAX)

        verify { snapshotValuationService.computeSnapshotValuation(coins, ValuationTimeframe.MAX) }
    }
}
