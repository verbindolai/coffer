package org.coffer.coffer2.application.portfolio

import io.mockk.every
import io.mockk.mockk
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceEntity
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteRepository
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RealTimeCollectorValuationServiceTest {

    private val coinIssueRepository = mockk<CoinIssueRepository>()
    private val issuePriceRepository = mockk<IssuePriceRepository>()
    private val metalQuoteRepository = mockk<MetalQuoteRepository>()

    private val service = RealTimeCollectorValuationService(
        coinIssueRepository, issuePriceRepository, metalQuoteRepository
    )

    private fun createCoin(
        id: UUID = UUID.randomUUID(),
        metalType: MetalType? = MetalType.GOLD,
        purity: BigDecimal? = BigDecimal("999"),
        weightInGrams: BigDecimal = BigDecimal("31.1035"),
        grade: CoinGrade? = CoinGrade.UNCIRCULATED,
        quantity: Int = 1,
        createdAt: ZonedDateTime = ZonedDateTime.now().minusDays(1)
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
        createdAt = createdAt,
        diameterInMillimeters = null,
        thicknessInMillimeters = null,
        quantity = quantity
    )

    @Test
    fun `resolveCoinIssueInfos returns empty list when no issues`() {
        val coin = createCoin()
        every { coinIssueRepository.findIssueIdsByCoinId(coin.id) } returns emptyList()

        val result = service.resolveCoinIssueInfos(listOf(coin))

        assertEquals(0, result.size)
    }

    @Test
    fun `resolveCoinIssueInfos resolves single issue as exact match`() {
        val coin = createCoin(grade = CoinGrade.UNCIRCULATED)
        val issueId = UUID.randomUUID()
        every { coinIssueRepository.findIssueIdsByCoinId(coin.id) } returns listOf(issueId)

        val result = service.resolveCoinIssueInfos(listOf(coin))

        assertEquals(1, result.size)
        assertEquals(coin, result[0].coin)
        assertEquals(listOf(issueId), result[0].issueIds)
        assertEquals(CoinGrade.UNCIRCULATED, result[0].grade)
        assertEquals(true, result[0].isExactMatch)
    }

    @Test
    fun `resolveCoinIssueInfos resolves multiple issues as non-exact match`() {
        val coin = createCoin(grade = CoinGrade.VERY_FINE)
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()
        every { coinIssueRepository.findIssueIdsByCoinId(coin.id) } returns listOf(issueId1, issueId2)

        val result = service.resolveCoinIssueInfos(listOf(coin))

        assertEquals(1, result.size)
        assertEquals(false, result[0].isExactMatch)
        assertEquals(CoinGrade.VERY_FINE, result[0].grade)
    }

    @Test
    fun `resolveCoinIssueInfos defaults to VERY_FINE when coin has no grade`() {
        val coin = createCoin(grade = null)
        val issueId = UUID.randomUUID()
        every { coinIssueRepository.findIssueIdsByCoinId(coin.id) } returns listOf(issueId)

        val result = service.resolveCoinIssueInfos(listOf(coin))

        assertEquals(CoinGrade.VERY_FINE, result[0].grade)
    }

    @Test
    fun `computeLivePoint returns null for empty coin list`() {
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null

        val result = service.computeLivePoint(emptyList(), ZonedDateTime.now())

        assertNull(result)
    }

    @Test
    fun `computeLivePoint computes exact values for single issue coin`() {
        val coinId = UUID.randomUUID()
        val issueId = UUID.randomUUID()
        val coin = createCoin(id = coinId, grade = CoinGrade.UNCIRCULATED)

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
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null

        val now = ZonedDateTime.now()
        val result = service.computeLivePoint(listOf(coin), now)

        assertNotNull(result)
        assertEquals(now, result.timestamp)
        assertEquals(BigDecimal("2500.00"), result.minValue)
        assertEquals(BigDecimal("2500.00"), result.maxValue)
    }

    @Test
    fun `computeLivePoint computes min-max range for multiple issues`() {
        val coinId = UUID.randomUUID()
        val issueId1 = UUID.randomUUID()
        val issueId2 = UUID.randomUUID()
        val coin = createCoin(id = coinId, grade = CoinGrade.UNCIRCULATED)

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
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null

        val now = ZonedDateTime.now()
        val result = service.computeLivePoint(listOf(coin), now)

        assertNotNull(result)
        assertEquals(BigDecimal("2400.00"), result.minValue)
        assertEquals(BigDecimal("2600.00"), result.maxValue)
    }

    @Test
    fun `computeLivePoint falls back to metal value when no issues`() {
        val coinId = UUID.randomUUID()
        val coin = createCoin(
            id = coinId,
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        )

        every { coinIssueRepository.findIssueIdsByCoinId(coinId) } returns emptyList()
        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            org.coffer.coffer2.repository.MetalQuoteEntity(
                id = UUID.randomUUID(),
                metalType = MetalType.GOLD,
                pricePerGram = BigDecimal("65.00"),
                currencyCode = "EUR",
                quotedAt = ZonedDateTime.now(),
                source = org.coffer.coffer2.domain.MetalQuoteSource.SWISSQUOTE,
                createdAt = ZonedDateTime.now()
            )
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null

        val now = ZonedDateTime.now()
        val result = service.computeLivePoint(listOf(coin), now)

        assertNotNull(result)
        assertEquals(BigDecimal("2019.71"), result.minValue)
        assertEquals(BigDecimal("2019.71"), result.maxValue)
    }
}
