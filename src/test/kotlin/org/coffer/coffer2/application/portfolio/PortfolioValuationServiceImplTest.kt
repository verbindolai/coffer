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
    private val snapshotValuationService = mockk<SnapshotValuationService>()

    private val service = PortfolioValuationServiceImpl(
        coinRepositoryAdapter,
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

        val result = service.getValuation(ValuationTimeframe.DAY_1)

        assertEquals(ValuationTimeframe.DAY_1, result.timeframe)
        assertEquals("EUR", result.currency)
        assertNull(result.metalValuation)
        assertNull(result.collectorValuation)
    }

    @Test
    fun `getValuation uses snapshot service for DAY_1`() {
        val coins = listOf(createCoin())
        val metalResult = PortfolioMetalValuationResult(listOf(
            PortfolioMetalPoint(ZonedDateTime.now(), BigDecimal("2000"), BigDecimal("31"), BigDecimal.ZERO, BigDecimal.ZERO)
        ))
        val collectorResult = PortfolioCollectorValuationResult(listOf(
            PortfolioCollectorPoint(ZonedDateTime.now(), BigDecimal("2500"), BigDecimal("2700"))
        ))

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeValuation(coins, ValuationTimeframe.DAY_1) } returns (metalResult to collectorResult)

        val result = service.getValuation(ValuationTimeframe.DAY_1)

        assertEquals(ValuationTimeframe.DAY_1, result.timeframe)
        assertEquals("EUR", result.currency)
        assertNotNull(result.metalValuation)
        assertNotNull(result.collectorValuation)
        verify { snapshotValuationService.computeValuation(coins, ValuationTimeframe.DAY_1) }
    }

    @Test
    fun `getValuation uses snapshot service for WEEK_1`() {
        val coins = listOf(createCoin())
        val metalResult = PortfolioMetalValuationResult(listOf(
            PortfolioMetalPoint(ZonedDateTime.now(), BigDecimal("2000"), BigDecimal("31"), BigDecimal.ZERO, BigDecimal.ZERO)
        ))

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeValuation(coins, ValuationTimeframe.WEEK_1) } returns (metalResult to null)

        val result = service.getValuation(ValuationTimeframe.WEEK_1)

        assertEquals(ValuationTimeframe.WEEK_1, result.timeframe)
        assertNotNull(result.metalValuation)
        assertNull(result.collectorValuation)
        verify { snapshotValuationService.computeValuation(coins, ValuationTimeframe.WEEK_1) }
    }

    @Test
    fun `getValuation uses snapshot service for MONTH_1`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeValuation(coins, ValuationTimeframe.MONTH_1) } returns (null to null)

        val result = service.getValuation(ValuationTimeframe.MONTH_1)

        assertEquals(ValuationTimeframe.MONTH_1, result.timeframe)
        verify { snapshotValuationService.computeValuation(coins, ValuationTimeframe.MONTH_1) }
    }

    @Test
    fun `getValuation uses snapshot service for YEAR_1`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeValuation(coins, ValuationTimeframe.YEAR_1) } returns (null to null)

        val result = service.getValuation(ValuationTimeframe.YEAR_1)

        verify { snapshotValuationService.computeValuation(coins, ValuationTimeframe.YEAR_1) }
    }

    @Test
    fun `getValuation uses snapshot service for MAX`() {
        val coins = listOf(createCoin())

        every { coinRepositoryAdapter.findAll() } returns coins
        every { snapshotValuationService.computeValuation(coins, ValuationTimeframe.MAX) } returns (null to null)

        val result = service.getValuation(ValuationTimeframe.MAX)

        verify { snapshotValuationService.computeValuation(coins, ValuationTimeframe.MAX) }
    }
}
