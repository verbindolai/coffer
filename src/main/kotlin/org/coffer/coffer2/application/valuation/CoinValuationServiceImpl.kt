package org.coffer.coffer2.application.valuation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceEntity
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteEntity
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.util.UUID

@Service
class CoinValuationServiceImpl(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val metalQuoteRepository: MetalQuoteRepository,
    private val issuePriceRepository: IssuePriceRepository,
    private val coinIssueRepository: CoinIssueRepository
) : CoinValuationService {

    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    override fun getValuation(coinId: UUID, timeframe: ValuationTimeframe): CoinValuationResult {
        logger.info { "Getting valuation for coin $coinId with timeframe ${timeframe.code}" }

        val coin = coinRepositoryAdapter.findById(coinId)
            ?: throw CoinNotFoundException(coinId)

        val now = ZonedDateTime.now()
        val startTime = timeframe.getStartTime(now)

        val metalValuation = calculateMetalValuation(coin, startTime, timeframe)
        val issueValuation = calculateIssueValuation(coin, startTime, timeframe)

        return CoinValuationResult(
            coinId = coinId,
            timeframe = timeframe,
            metalValuation = metalValuation,
            issueValuation = issueValuation
        )
    }

    private fun calculateMetalValuation(
        coin: Coin,
        startTime: ZonedDateTime?,
        timeframe: ValuationTimeframe
    ): MetalValuationResult? {
        val metalType = coin.metalType ?: return null
        val purity = coin.purity ?: return null

        val pureMetalMass = coin.weightInGrams.multiply(purity)
            .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)

        val quotes = if (startTime != null) {
            metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, startTime)
        } else {
            metalQuoteRepository.findByMetalTypeOrderByQuotedAt(metalType)
        }

        if (quotes.isEmpty()) {
            return null
        }

        val sampledQuotes = sampleDataPoints(quotes, timeframe.targetDataPoints) { it.quotedAt }
        val currency = sampledQuotes.firstOrNull()?.currencyCode ?: "USD"

        val dataPoints = sampledQuotes.map { quote ->
            MetalValuationPoint(
                timestamp = quote.quotedAt,
                pricePerGram = quote.pricePerGram,
                totalValue = pureMetalMass.multiply(quote.pricePerGram)
                    .setScale(2, RoundingMode.HALF_UP)
            )
        }

        return MetalValuationResult(
            metalType = metalType,
            pureMetalMassInGrams = pureMetalMass,
            currency = currency,
            dataPoints = dataPoints
        )
    }

    private fun calculateIssueValuation(
        coin: Coin,
        startTime: ZonedDateTime?,
        timeframe: ValuationTimeframe
    ): IssueValuationResult? {
        val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
        if (issueIds.isEmpty()) {
            return null
        }

        val grade = coin.grade ?: CoinGrade.VERY_FINE
        val isExactMatch = issueIds.size == 1

        val prices = fetchIssuePrices(issueIds, grade, startTime)
        if (prices.isEmpty()) {
            return null
        }

        val currency = prices.firstOrNull()?.currencyCode ?: "USD"

        val groupedByTime = prices.groupBy { truncateToInterval(it.createdAt, timeframe) }
        val sortedTimes = groupedByTime.keys.sorted()
        val sampledTimes = sampleDataPoints(sortedTimes, timeframe.targetDataPoints) { it }

        val dataPoints = sampledTimes.map { time ->
            val pricesAtTime = groupedByTime[time] ?: emptyList()
            createIssueValuationPoint(time, pricesAtTime, isExactMatch)
        }

        return IssueValuationResult(
            grade = coin.grade,
            isExactMatch = isExactMatch,
            currency = currency,
            dataPoints = dataPoints
        )
    }

    private fun fetchIssuePrices(
        issueIds: List<UUID>,
        grade: CoinGrade,
        startTime: ZonedDateTime?
    ): List<IssuePriceEntity> {
        return if (startTime != null) {
            issuePriceRepository.findByIssueIdsAndGradeAfter(issueIds, grade, startTime)
        } else {
            issuePriceRepository.findByIssueIdsAndGrade(issueIds, grade)
        }
    }

    private fun createIssueValuationPoint(
        timestamp: ZonedDateTime,
        prices: List<IssuePriceEntity>,
        isExactMatch: Boolean
    ): IssueValuationPoint {
        return if (isExactMatch && prices.size == 1) {
            IssueValuationPoint(
                timestamp = timestamp,
                price = prices.first().price,
                minPrice = null,
                maxPrice = null
            )
        } else {
            val priceValues = prices.map { it.price }
            IssueValuationPoint(
                timestamp = timestamp,
                price = null,
                minPrice = priceValues.minOrNull(),
                maxPrice = priceValues.maxOrNull()
            )
        }
    }

    private fun truncateToInterval(time: ZonedDateTime, timeframe: ValuationTimeframe): ZonedDateTime {
        return when (timeframe) {
            ValuationTimeframe.HOUR_1 -> time.withMinute(time.minute / 5 * 5).withSecond(0).withNano(0)
            ValuationTimeframe.DAY_1 -> time.withMinute(0).withSecond(0).withNano(0)
            ValuationTimeframe.WEEK_1 -> time.withMinute(0).withSecond(0).withNano(0)
            ValuationTimeframe.MONTH_1 -> time.withHour(0).withMinute(0).withSecond(0).withNano(0)
            ValuationTimeframe.YEAR_1 -> time.withHour(0).withMinute(0).withSecond(0).withNano(0)
            ValuationTimeframe.MAX -> time.withHour(0).withMinute(0).withSecond(0).withNano(0)
        }
    }

    private fun <T> sampleDataPoints(
        items: List<T>,
        targetCount: Int,
        timestampSelector: (T) -> ZonedDateTime
    ): List<T> {
        if (items.size <= targetCount) {
            return items
        }

        val result = mutableListOf<T>()
        val step = items.size.toDouble() / targetCount

        var index = 0.0
        while (index < items.size && result.size < targetCount) {
            result.add(items[index.toInt()])
            index += step
        }

        if (result.last() != items.last()) {
            result[result.lastIndex] = items.last()
        }

        return result
    }
}
