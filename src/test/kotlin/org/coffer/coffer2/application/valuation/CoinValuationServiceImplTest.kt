package org.coffer.coffer2.application.valuation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceEntity
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteEntity
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.domain.MetalQuoteSource
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoinValuationServiceImplTest {

    private val coinRepositoryAdapter = mockk<CoinRepositoryAdapter>()
    private val metalQuoteRepository = mockk<MetalQuoteRepository>()
    private val issuePriceRepository = mockk<IssuePriceRepository>()
    private val coinIssueRepository = mockk<CoinIssueRepository>()

    private val service = CoinValuationServiceImpl(
        coinRepositoryAdapter,
        metalQuoteRepository,
        issuePriceRepository,
        coinIssueRepository
    )

    private val zone = ZoneId.of("UTC")
    private val baseTime = ZonedDateTime.of(2025, 1, 15, 12, 0, 0, 0, zone)

    // ==================== Test Fixtures ====================

    private fun createTestCoin(
        id: UUID = UUID.randomUUID(),
        metalType: MetalType? = MetalType.GOLD,
        purity: BigDecimal? = BigDecimal("999"),
        weightInGrams: BigDecimal = BigDecimal("31.1035"),
        grade: CoinGrade? = CoinGrade.UNCIRCULATED
    ) = Coin(
        id = id,
        title = "American Gold Eagle",
        denomination = BigDecimal("50"),
        currency = Currency.getInstance("USD"),
        yearOfMinting = YearOfMinting(2024),
        issuerCountry = Locale.of("", "US"),
        mintMark = null,
        grade = grade,
        type = CoinType.BULLION,
        notes = null,
        numistaId = "12345",
        shape = CoinShape.CIRCULAR,
        weightInGrams = weightInGrams,
        purity = purity,
        metalType = metalType,
        rarity = null,
        createdAt = ZonedDateTime.now(),
        diameterInMillimeters = BigDecimal("32.7"),
        thicknessInMillimeters = BigDecimal("2.87")
    )

    private fun createMetalQuote(
        metalType: MetalType,
        pricePerGram: BigDecimal,
        quotedAt: ZonedDateTime,
        currencyCode: String = "USD"
    ) = MetalQuoteEntity(
        id = UUID.randomUUID(),
        metalType = metalType,
        pricePerGram = pricePerGram,
        currencyCode = currencyCode,
        quotedAt = quotedAt,
        source = MetalQuoteSource.SWISSQUOTE,
        createdAt = quotedAt
    )

    private fun createIssuePrice(
        issueId: UUID,
        grade: CoinGrade,
        price: BigDecimal,
        createdAt: ZonedDateTime,
        currencyCode: String = "USD"
    ) = IssuePriceEntity(
        id = UUID.randomUUID(),
        issueId = issueId,
        grade = grade,
        price = price,
        currencyCode = currencyCode,
        createdAt = createdAt
    )

    // ==================== Coin Not Found Tests ====================

    @Test
    fun `getValuation should throw CoinNotFoundException when coin does not exist`() {
        val coinId = UUID.randomUUID()
        every { coinRepositoryAdapter.findById(coinId) } returns null

        assertFailsWith<CoinNotFoundException> {
            service.getValuation(coinId, ValuationTimeframe.DAY_1)
        }
    }

    // ==================== Metal Valuation Tests ====================

    @Test
    fun `getValuation should return null metalValuation when coin has no metal type`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null)

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNull(result.metalValuation)
    }

    @Test
    fun `getValuation should return null metalValuation when coin has no purity`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, purity = null)

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNull(result.metalValuation)
    }

    @Test
    fun `getValuation should return null metalValuation when no quotes exist`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(any(), any()) } returns emptyList()
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNull(result.metalValuation)
    }

    @Test
    fun `getValuation should calculate correct pure metal mass`() {
        val coinId = UUID.randomUUID()
        // 31.1035g weight, 999/1000 purity = 31.072 pure gold
        val coin = createTestCoin(
            id = coinId,
            weightInGrams = BigDecimal("31.1035"),
            purity = BigDecimal("999")
        )

        val quote = createMetalQuote(
            MetalType.GOLD,
            BigDecimal("65.00"),
            baseTime
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns listOf(quote)
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.metalValuation)
        // 31.1035 * 999 / 1000 = 31.0723965, rounded to 6 decimals = 31.072397
        assertEquals(BigDecimal("31.072397"), result.metalValuation!!.pureMetalMassInGrams)
    }

    @Test
    fun `getValuation should calculate correct total value`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(
            id = coinId,
            weightInGrams = BigDecimal("31.1035"),
            purity = BigDecimal("999")
        )

        val quote = createMetalQuote(
            MetalType.GOLD,
            BigDecimal("65.00"),
            baseTime
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns listOf(quote)
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.metalValuation)
        assertEquals(1, result.metalValuation!!.dataPoints.size)
        // 31.072465 * 65.00 = 2019.71
        assertEquals(BigDecimal("2019.71"), result.metalValuation!!.dataPoints[0].totalValue)
    }

    @Test
    fun `getValuation should bucket metal quotes correctly`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        // Create quotes at different times within and across buckets
        // For DAY_1, bucket interval is 1 hour
        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("64.00"), baseTime.withHour(10).withMinute(15)),
            createMetalQuote(MetalType.GOLD, BigDecimal("64.50"), baseTime.withHour(10).withMinute(45)),  // Same bucket as above
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime.withHour(11).withMinute(30)),  // New bucket
            createMetalQuote(MetalType.GOLD, BigDecimal("65.50"), baseTime.withHour(12).withMinute(0)),   // New bucket
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.metalValuation)
        // Should have 3 buckets: 10:00, 11:00, 12:00
        assertEquals(3, result.metalValuation!!.dataPoints.size)

        // First bucket should use last value (64.50, not 64.00)
        assertEquals(BigDecimal("64.50"), result.metalValuation!!.dataPoints[0].pricePerGram)
        assertEquals(BigDecimal("65.00"), result.metalValuation!!.dataPoints[1].pricePerGram)
        assertEquals(BigDecimal("65.50"), result.metalValuation!!.dataPoints[2].pricePerGram)
    }

    @Test
    fun `getValuation should use all quotes for MAX timeframe`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("60.00"), baseTime.minusMonths(6)),
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime)
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeOrderByQuotedAt(MetalType.GOLD) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.MAX)

        assertNotNull(result.metalValuation)
        verify { metalQuoteRepository.findByMetalTypeOrderByQuotedAt(MetalType.GOLD) }
    }

    // ==================== Issue Valuation Tests ====================

    @Test
    fun `getValuation should return null issueValuation when no issues exist`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null, purity = null)

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNull(result.issueValuation)
    }

    @Test
    fun `getValuation should return null issueValuation when no prices exist`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null, purity = null)
        val issueId = UUID.randomUUID()

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findByIssueIdsAndGradeAfter(any(), any(), any()) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNull(result.issueValuation)
    }

    @Test
    fun `getValuation should return exact price for single issue match`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null, purity = null, grade = CoinGrade.UNCIRCULATED)
        val issueId = UUID.randomUUID()

        val prices = listOf(
            createIssuePrice(issueId, CoinGrade.UNCIRCULATED, BigDecimal("2500.00"), baseTime)
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findByIssueIdsAndGradeAfter(listOf(issueId), CoinGrade.UNCIRCULATED, any()) } returns prices

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.issueValuation)
        assertTrue(result.issueValuation!!.isExactMatch)
        assertEquals(1, result.issueValuation!!.dataPoints.size)
        assertEquals(BigDecimal("2500.00"), result.issueValuation!!.dataPoints[0].price)
        assertNull(result.issueValuation!!.dataPoints[0].minPrice)
        assertNull(result.issueValuation!!.dataPoints[0].maxPrice)
    }

    @Test
    fun `getValuation should return min and max for multiple issue matches`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null, purity = null, grade = CoinGrade.UNCIRCULATED)
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()

        val prices = listOf(
            createIssuePrice(issueId1, CoinGrade.UNCIRCULATED, BigDecimal("2400.00"), baseTime),
            createIssuePrice(issueId2, CoinGrade.UNCIRCULATED, BigDecimal("2600.00"), baseTime)
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId1, issueId2)
        every { issuePriceRepository.findByIssueIdsAndGradeAfter(any(), CoinGrade.UNCIRCULATED, any()) } returns prices

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.issueValuation)
        assertTrue(!result.issueValuation!!.isExactMatch)
        assertEquals(1, result.issueValuation!!.dataPoints.size)
        assertNull(result.issueValuation!!.dataPoints[0].price)
        assertEquals(BigDecimal("2400.00"), result.issueValuation!!.dataPoints[0].minPrice)
        assertEquals(BigDecimal("2600.00"), result.issueValuation!!.dataPoints[0].maxPrice)
    }

    @Test
    fun `getValuation should default to VERY_FINE grade when coin has no grade`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null, purity = null, grade = null)
        val issueId = UUID.randomUUID()

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findByIssueIdsAndGradeAfter(listOf(issueId), CoinGrade.VERY_FINE, any()) } returns emptyList()

        service.getValuation(coinId, ValuationTimeframe.DAY_1)

        verify { issuePriceRepository.findByIssueIdsAndGradeAfter(listOf(issueId), CoinGrade.VERY_FINE, any()) }
    }

    @Test
    fun `getValuation should bucket issue prices correctly`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, metalType = null, purity = null)
        val issueId = UUID.randomUUID()

        // Create prices at different hourly intervals (DAY_1 uses 1-hour buckets)
        val prices = listOf(
            createIssuePrice(issueId, CoinGrade.UNCIRCULATED, BigDecimal("2450.00"), baseTime.withHour(10).withMinute(15)),
            createIssuePrice(issueId, CoinGrade.UNCIRCULATED, BigDecimal("2500.00"), baseTime.withHour(10).withMinute(45)),  // Same bucket
            createIssuePrice(issueId, CoinGrade.UNCIRCULATED, BigDecimal("2550.00"), baseTime.withHour(11).withMinute(30)),  // New bucket
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findByIssueIdsAndGradeAfter(any(), any(), any()) } returns prices

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.issueValuation)
        // Should have 2 buckets: 10:00 and 11:00
        assertEquals(2, result.issueValuation!!.dataPoints.size)
        // First bucket should use last value (2500, not 2450)
        assertEquals(BigDecimal("2500.00"), result.issueValuation!!.dataPoints[0].price)
        assertEquals(BigDecimal("2550.00"), result.issueValuation!!.dataPoints[1].price)
    }

    // ==================== Combined Tests ====================

    @Test
    fun `getValuation should return both metal and issue valuations when data exists`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)
        val issueId = UUID.randomUUID()

        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime)
        )

        val prices = listOf(
            createIssuePrice(issueId, CoinGrade.UNCIRCULATED, BigDecimal("2500.00"), baseTime)
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findByIssueIdsAndGradeAfter(any(), any(), any()) } returns prices

        val result = service.getValuation(coinId, ValuationTimeframe.DAY_1)

        assertNotNull(result.metalValuation)
        assertNotNull(result.issueValuation)
        assertEquals(coinId, result.coinId)
        assertEquals(ValuationTimeframe.DAY_1, result.timeframe)
    }

    @Test
    fun `getValuation should handle gaps in data correctly`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        // Quotes with a gap (simulating weekend)
        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("64.00"), baseTime.minusDays(3)),  // Friday
            // Saturday and Sunday missing
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime)  // Monday
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.WEEK_1)

        assertNotNull(result.metalValuation)
        // Should have 2 data points - gaps are not filled
        assertEquals(2, result.metalValuation!!.dataPoints.size)
    }

    @Test
    fun `getValuation should sort data points chronologically`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        // Create quotes out of order
        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime.plusHours(2)),
            createMetalQuote(MetalType.GOLD, BigDecimal("64.00"), baseTime),
            createMetalQuote(MetalType.GOLD, BigDecimal("66.00"), baseTime.plusHours(4))
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.WEEK_1)

        assertNotNull(result.metalValuation)
        val timestamps = result.metalValuation!!.dataPoints.map { it.timestamp }
        assertEquals(timestamps.sorted(), timestamps)
    }

    // ==================== Different Timeframe Tests ====================

    @Test
    fun `getValuation should use correct bucket interval for WEEK_1`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        // Quotes within the same 4-hour bucket should be bucketed together
        // WEEK_1 uses 4-hour buckets (0-4, 4-8, 8-12, 12-16, 16-20, 20-24)
        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("64.00"), baseTime.withHour(13)),  // Bucket 12-16
            createMetalQuote(MetalType.GOLD, BigDecimal("64.50"), baseTime.withHour(14)),  // Same bucket
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime.withHour(15)),  // Same bucket
            createMetalQuote(MetalType.GOLD, BigDecimal("65.50"), baseTime.withHour(16))   // Bucket 16-20
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.WEEK_1)

        assertNotNull(result.metalValuation)
        // Should have 2 four-hour buckets: 12:00 and 16:00
        assertEquals(2, result.metalValuation!!.dataPoints.size)
        // First bucket should use last value (65.00)
        assertEquals(BigDecimal("65.00"), result.metalValuation!!.dataPoints[0].pricePerGram)
    }

    @Test
    fun `getValuation should use correct bucket interval for MONTH_1`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        // Quotes spanning different days (MONTH_1 uses daily buckets)
        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("64.00"), baseTime.minusDays(2).withHour(10)),
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime.minusDays(1).withHour(14)),
            createMetalQuote(MetalType.GOLD, BigDecimal("66.00"), baseTime.withHour(9)),
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.MONTH_1)

        assertNotNull(result.metalValuation)
        // Should have 3 daily buckets
        assertEquals(3, result.metalValuation!!.dataPoints.size)
    }

    @Test
    fun `getValuation should use correct bucket interval for YEAR_1`() {
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId)

        // Quotes on different days
        val quotes = listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("64.00"), baseTime.minusDays(2).withHour(10)),
            createMetalQuote(MetalType.GOLD, BigDecimal("64.50"), baseTime.minusDays(2).withHour(14)),  // Same day
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), baseTime.minusDays(1)),
            createMetalQuote(MetalType.GOLD, BigDecimal("66.00"), baseTime)
        )

        every { coinRepositoryAdapter.findById(coinId) } returns coin
        every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(MetalType.GOLD, any()) } returns quotes
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()

        val result = service.getValuation(coinId, ValuationTimeframe.YEAR_1)

        assertNotNull(result.metalValuation)
        // Should have 3 daily buckets
        assertEquals(3, result.metalValuation!!.dataPoints.size)
        // First bucket (2 days ago) should use last value (64.50)
        assertEquals(BigDecimal("64.50"), result.metalValuation!!.dataPoints[0].pricePerGram)
    }
}
