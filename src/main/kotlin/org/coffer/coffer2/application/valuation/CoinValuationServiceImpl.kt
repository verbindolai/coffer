package org.coffer.coffer2.application.valuation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.CoinValuationResult
import org.coffer.coffer2.domain.CollectorPricesResult
import org.coffer.coffer2.domain.CurrentPricesResult
import org.coffer.coffer2.domain.GradePriceResult
import org.coffer.coffer2.domain.IssueValuationPoint
import org.coffer.coffer2.domain.IssueValuationResult
import org.coffer.coffer2.domain.MetalValuationPoint
import org.coffer.coffer2.domain.MetalValuationResult
import org.coffer.coffer2.domain.MetalValueResult
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceEntity
import org.coffer.coffer2.repository.IssuePriceRepository
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

        val currency = quotes.first().currencyCode

        // Bucket quotes by timeframe interval, taking last value per bucket
        val bucketedQuotes = bucketByInterval(quotes, timeframe) { it.quotedAt }

        val dataPoints = bucketedQuotes.map { (bucketTime, quotesInBucket) ->
            // Use the last quote in the bucket (most recent)
            val lastQuote = quotesInBucket.last()
            MetalValuationPoint(
                timestamp = bucketTime,
                pricePerGram = lastQuote.pricePerGram,
                totalValue = pureMetalMass.multiply(lastQuote.pricePerGram)
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

        val currency = prices.first().currencyCode

        // Bucket prices by timeframe interval
        val bucketedPrices = bucketByInterval(prices, timeframe) { it.createdAt }

        val dataPoints = bucketedPrices.map { (bucketTime, pricesInBucket) ->
            createIssueValuationPoint(bucketTime, pricesInBucket, isExactMatch)
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
        val priceValues = prices.map { it.price }

        return if (isExactMatch) {
            // Single issue: use last price in bucket
            IssueValuationPoint(
                timestamp = timestamp,
                price = prices.last().price,
                minPrice = null,
                maxPrice = null
            )
        } else {
            // Multiple issues: show min/max range
            IssueValuationPoint(
                timestamp = timestamp,
                price = null,
                minPrice = priceValues.minOrNull(),
                maxPrice = priceValues.maxOrNull()
            )
        }
    }

    /**
     * Groups items into time buckets based on the timeframe interval.
     * Returns a sorted map of bucket start time -> items in that bucket.
     * Buckets with no data are skipped (gaps are allowed).
     */
    private fun <T> bucketByInterval(
        items: List<T>,
        timeframe: ValuationTimeframe,
        timestampSelector: (T) -> ZonedDateTime
    ): List<Pair<ZonedDateTime, List<T>>> {
        if (items.isEmpty()) return emptyList()

        // Group items by their bucket
        val grouped = items.groupBy { item ->
            timeframe.truncateToBucket(timestampSelector(item))
        }

        // Sort by bucket time and return as list of pairs
        return grouped.entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }

    @Transactional(readOnly = true)
    override fun getCurrentPrices(coinId: UUID): CurrentPricesResult {
        logger.info { "Getting current prices for coin $coinId" }

        val coin = coinRepositoryAdapter.findById(coinId)
            ?: throw CoinNotFoundException(coinId)

        val metalValue = getCurrentMetalValue(coin)
        val collectorPrices = getCurrentCollectorPrices(coin)

        // Determine currency (prefer metal, fall back to collector, default to USD)
        val currency = metalValue?.let { "USD" }
            ?: collectorPrices?.gradePrices?.firstOrNull()?.let { "USD" }
            ?: "USD"

        return CurrentPricesResult(
            coinId = coinId,
            currency = currency,
            metalValue = metalValue,
            collectorPrices = collectorPrices
        )
    }

    private fun getCurrentMetalValue(coin: Coin): MetalValueResult? {
        val metalType = coin.metalType ?: return null
        val purity = coin.purity ?: return null

        val pureMetalMass = coin.weightInGrams.multiply(purity)
            .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)

        val latestQuote = metalQuoteRepository.findLatestByMetalType(metalType)
            ?: return null

        val totalValue = pureMetalMass.multiply(latestQuote.pricePerGram)
            .setScale(2, RoundingMode.HALF_UP)

        return MetalValueResult(
            metalType = metalType,
            pureMetalMassInGrams = pureMetalMass,
            pricePerGram = latestQuote.pricePerGram,
            totalValue = totalValue,
            timestamp = latestQuote.quotedAt
        )
    }

    private fun getCurrentCollectorPrices(coin: Coin): CollectorPricesResult? {
        val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
        if (issueIds.isEmpty()) {
            return null
        }

        // Get latest prices for all grades from all linked issues
        val allPrices = issueIds.flatMap { issueId ->
            issuePriceRepository.findLatestPricesByIssueId(issueId)
        }

        if (allPrices.isEmpty()) {
            return null
        }

        // Group by grade and get min/max prices
        val pricesByGrade = allPrices
            .groupBy { it.grade }
            .mapValues { (_, prices) ->
                val priceValues = prices.map { it.price }
                Pair(priceValues.min(), priceValues.max())
            }

        // Convert to sorted list of grade prices with min/max
        val gradePrices = pricesByGrade
            .map { (grade, minMax) -> GradePriceResult(grade, minMax.first, minMax.second) }
            .sortedBy { it.grade.ordinal }

        // Check if coin's grade has an exact match (single price, not a range)
        val coinGrade = coin.grade
        val coinGradePrice = pricesByGrade[coinGrade]
        val hasExactMatch = coinGrade != null && coinGradePrice != null && coinGradePrice.first == coinGradePrice.second

        // Get the latest timestamp
        val latestTimestamp = allPrices.maxOfOrNull { it.createdAt }

        return CollectorPricesResult(
            coinGrade = coinGrade,
            hasExactMatch = hasExactMatch,
            gradePrices = gradePrices,
            timestamp = latestTimestamp
        )
    }
}
