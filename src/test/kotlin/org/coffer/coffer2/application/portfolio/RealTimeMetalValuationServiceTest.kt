package org.coffer.coffer2.application.portfolio

import io.mockk.every
import io.mockk.mockk
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.repository.MetalQuoteEntity
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

class RealTimeMetalValuationServiceTest {

    private val metalQuoteRepository = mockk<MetalQuoteRepository>()
    private val service = RealTimeMetalValuationService(metalQuoteRepository)

    private fun createCoin(
        metalType: MetalType? = MetalType.GOLD,
        purity: BigDecimal? = BigDecimal("999"),
        weightInGrams: BigDecimal = BigDecimal("31.1035"),
        quantity: Int = 1,
        createdAt: ZonedDateTime = ZonedDateTime.now().minusDays(1)
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
        createdAt = createdAt,
        diameterInMillimeters = null,
        thicknessInMillimeters = null,
        quantity = quantity
    )

    private fun createMetalQuote(
        metalType: MetalType,
        pricePerGram: BigDecimal,
        quotedAt: ZonedDateTime = ZonedDateTime.now()
    ) = MetalQuoteEntity(
        id = UUID.randomUUID(),
        metalType = metalType,
        pricePerGram = pricePerGram,
        currencyCode = "EUR",
        quotedAt = quotedAt,
        source = MetalQuoteSource.SWISSQUOTE,
        createdAt = ZonedDateTime.now()
    )

    @Test
    fun `computeTimeSeries returns null when no metal coins`() {
        val coins = listOf(createCoin(metalType = null, purity = null))
        val startTime = ZonedDateTime.now().minusHours(1)

        val result = service.computeTimeSeries(coins, startTime, ValuationTimeframe.HOUR_1)

        assertNull(result)
    }

    @Test
    fun `computeTimeSeries returns null when no quotes available`() {
        val coins = listOf(createCoin())
        val startTime = ZonedDateTime.now().minusHours(1)

        every { metalQuoteRepository.findLatestBefore(any()) } returns emptyList()
        MetalType.entries.forEach { metalType ->
            every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, any()) } returns emptyList()
        }
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null

        val result = service.computeTimeSeries(coins, startTime, ValuationTimeframe.HOUR_1)

        assertNull(result)
    }

    @Test
    fun `computeTimeSeries returns data points from seeded prices`() {
        val coins = listOf(createCoin())
        val startTime = ZonedDateTime.now().minusHours(1)

        every { metalQuoteRepository.findLatestBefore(any()) } returns listOf(
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"), startTime.minusMinutes(5))
        )
        MetalType.entries.forEach { metalType ->
            every { metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, any()) } returns emptyList()
        }
        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null

        val result = service.computeTimeSeries(coins, startTime, ValuationTimeframe.HOUR_1)

        assertNotNull(result)
        assert(result.dataPoints.isNotEmpty())
    }

    @Test
    fun `computeLivePoint returns null when no metal coins`() {
        val coins = listOf(createCoin(metalType = null, purity = null))

        val result = service.computeLivePoint(coins, ZonedDateTime.now())

        assertNull(result)
    }

    @Test
    fun `computeLivePoint returns null when no metal prices`() {
        val coins = listOf(createCoin())
        every { metalQuoteRepository.findLatestByMetalType(any()) } returns null

        val result = service.computeLivePoint(coins, ZonedDateTime.now())

        assertNull(result)
    }

    @Test
    fun `computeLivePoint computes correct values`() {
        val coins = listOf(createCoin(
            metalType = MetalType.GOLD,
            purity = BigDecimal("999"),
            weightInGrams = BigDecimal("31.1035")
        ))

        every { metalQuoteRepository.findLatestByMetalType(MetalType.GOLD) } returns
            createMetalQuote(MetalType.GOLD, BigDecimal("65.00"))
        every { metalQuoteRepository.findLatestByMetalType(MetalType.SILVER) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.PLATINUM) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.NICKEL) } returns null
        every { metalQuoteRepository.findLatestByMetalType(MetalType.BASE_METAL) } returns null

        val now = ZonedDateTime.now()
        val result = service.computeLivePoint(coins, now)

        assertNotNull(result)
        assertEquals(now, result.timestamp)
        assertEquals(BigDecimal("2019.71"), result.totalValue)
        assertEquals(BigDecimal("31.072397"), result.goldGrams)
        assertEquals(BigDecimal("0.000000"), result.silverGrams)
        assertEquals(BigDecimal("0.000000"), result.platinumGrams)
    }
}
