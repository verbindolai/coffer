package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.bucketByInterval
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.util.CollectorValuationCalculator
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.UUID

@Component
class RealTimeCollectorValuationService(
    private val coinIssueRepository: CoinIssueRepository,
    private val issuePriceRepository: IssuePriceRepository,
    private val metalQuoteRepository: MetalQuoteRepository
) {

    fun resolveCoinIssueInfos(coins: List<Coin>): List<CoinIssueInfo> {
        return coins.mapNotNull { coin ->
            val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
            if (issueIds.isEmpty()) return@mapNotNull null
            CoinIssueInfo(
                coin = coin,
                issueIds = issueIds,
                grade = coin.grade ?: CoinGrade.VERY_FINE,
                isExactMatch = issueIds.size == 1
            )
        }
    }

    fun computeTimeSeries(
        coins: List<Coin>,
        startTime: ZonedDateTime,
        timeframe: ValuationTimeframe
    ): PortfolioCollectorValuationResult? {
        val now = ZonedDateTime.now()
        if (coins.isEmpty()) return null

        val coinIssueInfos = resolveCoinIssueInfos(coins)
        val coinsWithIssues = coinIssueInfos.map { it.coin.id }.toSet()
        val coinsWithoutCollector = coins.filter { it.id !in coinsWithIssues }

        val allIssueIds = coinIssueInfos.flatMap { it.issueIds }.distinct()

        // Seed last known issue prices from before the window start
        val lastKnownPrices = mutableMapOf<Pair<UUID, CoinGrade>, BigDecimal>()
        if (allIssueIds.isNotEmpty()) {
            val grades = coinIssueInfos.map { it.grade }.distinct()
            for (grade in grades) {
                val seedPrices = issuePriceRepository.findLatestBeforeByIssueIdsAndGrade(allIssueIds, grade, startTime)
                seedPrices.forEach { entity ->
                    lastKnownPrices[entity.issueId to entity.grade] = entity.price
                }
            }
        }

        // Seed last known metal prices from before the window start
        val lastKnownMetalPrices = mutableMapOf<MetalType, BigDecimal>()
        val seedMetalQuotes = metalQuoteRepository.findLatestBefore(startTime)
        seedMetalQuotes.forEach { quote ->
            lastKnownMetalPrices[quote.metalType] = quote.pricePerGram
        }

        // Fetch issue prices within the time range
        val allIssuePrices = if (allIssueIds.isNotEmpty()) {
            issuePriceRepository.findByIssueIdsAfter(allIssueIds, startTime)
        } else emptyList()

        // Fetch metal quotes within the time range
        val allMetalQuotes = MetalType.entries.flatMap { metalType ->
            metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, startTime)
        }

        // Combine issue price and metal quote timestamps into unified buckets
        data class TimestampedEvent(val timestamp: ZonedDateTime)
        val allEvents = allIssuePrices.map { TimestampedEvent(it.createdAt) } +
            allMetalQuotes.map { TimestampedEvent(it.quotedAt) }
        val allBucketTimes = allEvents
            .map { timeframe.truncateToBucket(it.timestamp) }
            .distinct()
            .sorted()

        // Build lookup maps for issue prices and metal quotes by bucket
        val issuePricesByBucket = bucketByInterval(allIssuePrices, timeframe) { it.createdAt }
        val metalQuotesByBucket = bucketByInterval(allMetalQuotes, timeframe) { it.quotedAt }
        val issuePriceBucketMap = issuePricesByBucket.toMap()
        val metalQuoteBucketMap = metalQuotesByBucket.toMap()

        val dataPoints = mutableListOf<PortfolioCollectorPoint>()
        val bucketStartTime = timeframe.truncateToBucket(startTime)

        // Generate initial data point at window start using seeded prices
        val firstBucketTime = allBucketTimes.firstOrNull()
        if ((lastKnownPrices.isNotEmpty() || lastKnownMetalPrices.isNotEmpty()) && firstBucketTime != bucketStartTime) {
            computeCollectorPoint(coinIssueInfos, lastKnownPrices, coinsWithoutCollector, lastKnownMetalPrices, bucketStartTime)?.let { dataPoints.add(it) }
        }

        // Process all bucket times
        for (bucketTime in allBucketTimes) {
            issuePriceBucketMap[bucketTime]?.let { pricesInBucket ->
                val bucketPriceMap = pricesInBucket
                    .groupBy { it.issueId to it.grade }
                    .mapValues { (_, prices) -> prices.last().price }
                bucketPriceMap.forEach { (key, price) ->
                    lastKnownPrices[key] = price
                }
            }

            metalQuoteBucketMap[bucketTime]?.let { quotesInBucket ->
                quotesInBucket
                    .groupBy { it.metalType }
                    .mapValues { (_, quotes) -> quotes.last().pricePerGram }
                    .forEach { (metalType, price) ->
                        lastKnownMetalPrices[metalType] = price
                    }
            }

            computeCollectorPoint(coinIssueInfos, lastKnownPrices, coinsWithoutCollector, lastKnownMetalPrices, bucketTime)?.let { dataPoints.add(it) }
        }

        // Append live data point at current time (only if it's in a new bucket)
        val lastCollectorBucketTime = dataPoints.lastOrNull()?.timestamp
        val liveCollectorBucketTime = timeframe.truncateToBucket(now)
        if (lastCollectorBucketTime == null || liveCollectorBucketTime != lastCollectorBucketTime) {
            computeLivePoint(coins, now)?.let { dataPoints.add(it) }
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioCollectorValuationResult(dataPoints)
        } else null
    }

    fun computeLivePoint(coins: List<Coin>, now: ZonedDateTime): PortfolioCollectorPoint? {
        val coinIssueInfos = resolveCoinIssueInfos(coins)
        val coinsWithIssues = coinIssueInfos.map { it.coin.id }.toSet()
        val coinsWithoutCollector = coins.filter { it.id !in coinsWithIssues }

        val livePrices = mutableMapOf<Pair<UUID, CoinGrade>, BigDecimal>()
        for (info in coinIssueInfos) {
            for (issueId in info.issueIds) {
                issuePriceRepository.findLatestByIssueIdAndGrade(issueId, info.grade)?.let {
                    livePrices[it.issueId to it.grade] = it.price
                }
            }
        }

        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }

        return computeCollectorPoint(coinIssueInfos, livePrices, coinsWithoutCollector, metalPrices, now)
    }

    private fun computeCollectorPoint(
        coinIssueInfos: List<CoinIssueInfo>,
        priceMap: Map<Pair<UUID, CoinGrade>, BigDecimal>,
        coinsWithoutCollector: List<Coin>,
        metalPrices: Map<MetalType, BigDecimal>,
        timestamp: ZonedDateTime
    ): PortfolioCollectorPoint? {
        val agg = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos, priceMap, coinsWithoutCollector, metalPrices
        ) { !it.createdAt.isAfter(timestamp) }

        val hasValues = agg.minValue > BigDecimal.ZERO || agg.maxValue > BigDecimal.ZERO
        if (!hasValues) return null

        return PortfolioCollectorPoint(
            timestamp = timestamp,
            exactValue = null,
            minValue = agg.minValue,
            maxValue = agg.maxValue
        )
    }
}
