package org.coffer.coffer2.application.portfolio

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceEntity
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteEntity
import org.coffer.coffer2.repository.MetalQuoteRepository
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

class PortfolioSnapshotServiceImplTest {

    private val coinRepositoryAdapter = mockk<CoinRepositoryAdapter>()
    private val metalQuoteRepository = mockk<MetalQuoteRepository>()
    private val issuePriceRepository = mockk<IssuePriceRepository>()
    private val coinIssueRepository = mockk<CoinIssueRepository>()
    private val portfolioSnapshotRepository = mockk<PortfolioSnapshotRepository>()

    private val service = PortfolioSnapshotServiceImpl(
        coinRepositoryAdapter,
        metalQuoteRepository,
        issuePriceRepository,
        coinIssueRepository,
        portfolioSnapshotRepository
    )

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

    private fun createMetalQuote(
        metalType: MetalType,
        pricePerGram: BigDecimal
    ) = MetalQuoteEntity(
        id = UUID.randomUUID(),
        metalType = metalType,
        pricePerGram = pricePerGram,
        currencyCode = "EUR",
        quotedAt = ZonedDateTime.now(),
        source = MetalQuoteSource.SWISSQUOTE,
        createdAt = ZonedDateTime.now()
    )

    private fun stubSaveSnapshot() {
        val slot = slot<PortfolioSnapshotEntity>()
        every { portfolioSnapshotRepository.save(capture(slot)) } answers { slot.captured }
    }

    // ==================== Empty Portfolio Tests ====================

    @Test
    fun `computeAndStoreSnapshot creates zero-value snapshot for empty portfolio`() {
        every { coinRepositoryAdapter.findAll() } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals(0, result.totalCoins)
        assertEquals(0, result.totalQuantity)
        assertEquals(BigDecimal.ZERO, result.metalValue)
        assertEquals(BigDecimal.ZERO, result.collectorValueMin)
        assertEquals(BigDecimal.ZERO, result.collectorValueMax)
        assertEquals(LocalDate.now(), result.snapshotDate)
    }

    // ==================== Metal Valuation Tests ====================

    @Test
    fun `computeAndStoreSnapshot calculates correct metal value for gold coin`() {
        val coin = createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )

        every { coinRepositoryAdapter.findAll() } returns listOf(coin)
        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(coin.id) } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        // 31.1035 * 999 / 1000 = 31.072397g pure gold
        // 31.072397 * 65.00 = 2019.71
        assertEquals(BigDecimal("2019.71"), result.metalValue)
        assertEquals(BigDecimal("31.072397"), result.goldGrams)
        assertEquals(BigDecimal("0.000000"), result.silverGrams)
        assertEquals(BigDecimal("0.000000"), result.platinumGrams)
    }

    @Test
    fun `computeAndStoreSnapshot aggregates multiple metal types correctly`() {
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

        every { coinRepositoryAdapter.findAll() } returns listOf(goldCoin, silverCoin)
        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns
            createMetalQuote(MetalType.SILVER, BigDecimal("0.85"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(goldCoin.id) } returns emptyList()
        every { coinIssueRepository.findIssueIdsByCoinId(silverCoin.id) } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals(BigDecimal("31.072397"), result.goldGrams)
        assertEquals(BigDecimal("31.072397"), result.silverGrams)
        assertEquals(2, result.totalCoins)
    }

    // ==================== Collector Valuation Tests ====================

    @Test
    fun `computeAndStoreSnapshot uses exact collector price for single issue`() {
        val coinId = UUID.randomUUID()
        val issueId = UUID.randomUUID()
        val coin = createCoin(id = coinId, grade = CoinGrade.UNCIRCULATED)

        every { coinRepositoryAdapter.findAll() } returns listOf(coin)
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findLatestByIssueIdAndGrade(issueId, CoinGrade.UNCIRCULATED) } returns
            IssuePriceEntity(
                id = UUID.randomUUID(),
                issueId = issueId,
                grade = CoinGrade.UNCIRCULATED,
                price = BigDecimal("2500.00"),
                currencyCode = "EUR",
                createdAt = ZonedDateTime.now()
            )
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals(BigDecimal("2500.00"), result.collectorValueMin)
        assertEquals(BigDecimal("2500.00"), result.collectorValueMax)
    }

    @Test
    fun `computeAndStoreSnapshot uses min and max for multiple issues`() {
        val coinId = UUID.randomUUID()
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()
        val coin = createCoin(id = coinId, grade = CoinGrade.UNCIRCULATED)

        every { coinRepositoryAdapter.findAll() } returns listOf(coin)
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId1, issueId2)
        every { issuePriceRepository.findLatestByIssueIdAndGrade(issueId1, CoinGrade.UNCIRCULATED) } returns
            IssuePriceEntity(
                id = UUID.randomUUID(),
                issueId = issueId1,
                grade = CoinGrade.UNCIRCULATED,
                price = BigDecimal("2400.00"),
                currencyCode = "EUR",
                createdAt = ZonedDateTime.now()
            )
        every { issuePriceRepository.findLatestByIssueIdAndGrade(issueId2, CoinGrade.UNCIRCULATED) } returns
            IssuePriceEntity(
                id = UUID.randomUUID(),
                issueId = issueId2,
                grade = CoinGrade.UNCIRCULATED,
                price = BigDecimal("2600.00"),
                currencyCode = "EUR",
                createdAt = ZonedDateTime.now()
            )
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals(BigDecimal("2400.00"), result.collectorValueMin)
        assertEquals(BigDecimal("2600.00"), result.collectorValueMax)
    }

    @Test
    fun `computeAndStoreSnapshot falls back to metal value when no issues exist`() {
        val coinId = UUID.randomUUID()
        val coin = createCoin(
            id = coinId,
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )

        every { coinRepositoryAdapter.findAll() } returns listOf(coin)
        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        // Collector values should equal metal value as fallback
        // metal value = 31.072397 * 65.00 = 2019.705805
        assertNotNull(result.collectorValueMin)
        assertNotNull(result.collectorValueMax)
    }

    @Test
    fun `computeAndStoreSnapshot falls back to metal value when no prices found for issues`() {
        val coinId = UUID.randomUUID()
        val issueId = UUID.randomUUID()
        val coin = createCoin(
            id = coinId,
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035"),
            grade = CoinGrade.UNCIRCULATED
        )

        every { coinRepositoryAdapter.findAll() } returns listOf(coin)
        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns listOf(issueId)
        every { issuePriceRepository.findLatestByIssueIdAndGrade(issueId, CoinGrade.UNCIRCULATED) } returns null
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        // Should fall back to metal value since no issue prices exist
        assertNotNull(result.collectorValueMin)
        assertNotNull(result.collectorValueMax)
    }

    // ==================== Snapshot Persistence Tests ====================

    @Test
    fun `computeAndStoreSnapshot saves snapshot with today's date`() {
        every { coinRepositoryAdapter.findAll() } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals(LocalDate.now(), result.snapshotDate)
        verify { portfolioSnapshotRepository.save(any()) }
    }

    @Test
    fun `computeAndStoreSnapshot saves correct coin counts`() {
        val coin1 = createCoin(quantity = 2)
        val coin2 = createCoin(quantity = 3)

        every { coinRepositoryAdapter.findAll() } returns listOf(coin1, coin2)
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null
        every { coinIssueRepository.findIssueIdsByCoinId(coin1.id) } returns emptyList()
        every { coinIssueRepository.findIssueIdsByCoinId(coin2.id) } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals(2, result.totalCoins)
        assertEquals(5, result.totalQuantity)
    }

    @Test
    fun `computeAndStoreSnapshot uses EUR as default currency`() {
        every { coinRepositoryAdapter.findAll() } returns emptyList()
        stubSaveSnapshot()

        val result = service.computeAndStoreSnapshot()

        assertEquals("EUR", result.currency)
    }
}
