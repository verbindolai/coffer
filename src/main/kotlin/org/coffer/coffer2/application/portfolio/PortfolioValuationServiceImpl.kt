package org.coffer.coffer2.application.portfolio

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.bucketByInterval
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.repository.PortfolioSnapshotRepository
import org.coffer.coffer2.util.MetalValuationCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class PortfolioValuationServiceImpl(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val metalQuoteRepository: MetalQuoteRepository,
    private val issuePriceRepository: IssuePriceRepository,
    private val coinIssueRepository: CoinIssueRepository,
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository
) : PortfolioValuationService {

    private val logger = KotlinLogging.logger {}

    private data class CoinIssueInfo(
        val coin: Coin,
        val issueIds: List<java.util.UUID>,
        val grade: CoinGrade,
        val isExactMatch: Boolean
    )

    companion object {
        private const val DEFAULT_CURRENCY = "EUR"
        private val REAL_TIME_TIMEFRAMES = setOf(ValuationTimeframe.HOUR_1, ValuationTimeframe.DAY_1)
    }

    @Transactional(readOnly = true)
    override fun getValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        return if (timeframe in REAL_TIME_TIMEFRAMES) {
            computeRealTimeValuation(timeframe)
        } else {
            computeSnapshotValuation(timeframe)
        }
    }

    /**
     * Computes real-time valuation for short timeframes (1h, 1d).
     * Uses actual metal quotes and issue prices from the database.
     */
    private fun computeRealTimeValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        val coins = coinRepositoryAdapter.findAll()
        if (coins.isEmpty()) {
            return emptyValuationResult(timeframe)
        }

        val now = ZonedDateTime.now()
        val startTime = timeframe.getStartTime(now)!!

        val metalValuation = computeRealTimeMetalValuation(coins, startTime, timeframe)
        val collectorValuation = computeRealTimeCollectorValuation(coins, startTime, timeframe)

        return PortfolioValuationResult(
            timeframe = timeframe,
            currency = DEFAULT_CURRENCY,
            metalValuation = metalValuation,
            collectorValuation = collectorValuation
        )
    }

    private fun computeRealTimeMetalValuation(
        coins: List<Coin>,
        startTime: ZonedDateTime,
        timeframe: ValuationTimeframe
    ): PortfolioMetalValuationResult? {
        val now = ZonedDateTime.now()
        val metalCoins = coins.filter { it.metalType != null && it.purity != null }
        if (metalCoins.isEmpty()) return null

        // Seed last known prices from before the window start
        val lastKnownPrices = mutableMapOf<MetalType, BigDecimal>()
        val seedQuotes = metalQuoteRepository.findLatestBefore(startTime)
        seedQuotes.forEach { quote ->
            lastKnownPrices[quote.metalType] = quote.pricePerGram
        }

        // Get all metal quotes within the time range
        val allQuotes = MetalType.entries.flatMap { metalType ->
            metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, startTime)
        }

        val bucketedQuotes = bucketByInterval(allQuotes, timeframe) { it.quotedAt }

        val dataPoints = mutableListOf<PortfolioMetalPoint>()
        val bucketStartTime = timeframe.truncateToBucket(startTime)

        // Generate initial data point at window start using seeded prices
        val firstBucketTime = bucketedQuotes.firstOrNull()?.first
        if (lastKnownPrices.isNotEmpty() && firstBucketTime != bucketStartTime) {
            computeMetalPoint(metalCoins, lastKnownPrices, bucketStartTime)?.let { dataPoints.add(it) }
        }

        // Process bucketed quotes within the window
        for ((bucketTime, quotesInBucket) in bucketedQuotes) {
            val pricesByMetal = quotesInBucket
                .groupBy { it.metalType }
                .mapValues { (_, quotes) -> quotes.last().pricePerGram }

            pricesByMetal.forEach { (metalType, price) ->
                lastKnownPrices[metalType] = price
            }

            computeMetalPoint(metalCoins, lastKnownPrices, bucketTime)?.let { dataPoints.add(it) }
        }

        // Append live data point at current time (only if it's in a new bucket)
        val lastBucketTime = dataPoints.lastOrNull()?.timestamp
        val liveBucketTime = timeframe.truncateToBucket(now)
        if (lastBucketTime == null || liveBucketTime != lastBucketTime) {
            val livePrices = mutableMapOf<MetalType, BigDecimal>()
            MetalType.entries.forEach { metalType ->
                metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                    livePrices[metalType] = it.pricePerGram
                }
            }
            if (livePrices.isNotEmpty()) {
                computeMetalPoint(metalCoins, livePrices, now)?.let { dataPoints.add(it) }
            }
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioMetalValuationResult(dataPoints)
        } else null
    }

    private fun computeMetalPoint(
        metalCoins: List<Coin>,
        pricesByMetal: Map<MetalType, BigDecimal>,
        timestamp: ZonedDateTime
    ): PortfolioMetalPoint? {
        val agg = MetalValuationCalculator.aggregateMetalValues(
            metalCoins, pricesByMetal
        ) { !it.createdAt.isAfter(timestamp) }

        if (agg.totalValue.compareTo(BigDecimal.ZERO) == 0) return null

        return PortfolioMetalPoint(
            timestamp = timestamp,
            totalValue = agg.totalValue,
            goldGrams = agg.goldGrams,
            silverGrams = agg.silverGrams,
            platinumGrams = agg.platinumGrams
        )
    }

    private fun computeRealTimeCollectorValuation(
        coins: List<Coin>,
        startTime: ZonedDateTime,
        timeframe: ValuationTimeframe
    ): PortfolioCollectorValuationResult? {
        val now = ZonedDateTime.now()
        if (coins.isEmpty()) return null

        val coinIssueInfos = coins.mapNotNull { coin ->
            val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
            if (issueIds.isEmpty()) return@mapNotNull null
            CoinIssueInfo(
                coin = coin,
                issueIds = issueIds,
                grade = coin.grade ?: CoinGrade.VERY_FINE,
                isExactMatch = issueIds.size == 1
            )
        }

        val coinsWithIssues = coinIssueInfos.map { it.coin.id }.toSet()
        val coinsWithoutCollector = coins.filter { it.id !in coinsWithIssues }

        val allIssueIds = coinIssueInfos.flatMap { it.issueIds }.distinct()

        // Seed last known issue prices from before the window start
        val lastKnownPrices = mutableMapOf<Pair<java.util.UUID, CoinGrade>, BigDecimal>()
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
            // Update issue prices
            issuePriceBucketMap[bucketTime]?.let { pricesInBucket ->
                val bucketPriceMap = pricesInBucket
                    .groupBy { it.issueId to it.grade }
                    .mapValues { (_, prices) -> prices.last().price }
                bucketPriceMap.forEach { (key, price) ->
                    lastKnownPrices[key] = price
                }
            }

            // Update metal prices
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
            val livePrices = mutableMapOf<Pair<java.util.UUID, CoinGrade>, BigDecimal>()
            for (info in coinIssueInfos) {
                for (issueId in info.issueIds) {
                    issuePriceRepository.findLatestByIssueIdAndGrade(issueId, info.grade)?.let {
                        livePrices[it.issueId to it.grade] = it.price
                    }
                }
            }
            val liveMetalPrices = mutableMapOf<MetalType, BigDecimal>()
            MetalType.entries.forEach { metalType ->
                metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                    liveMetalPrices[metalType] = it.pricePerGram
                }
            }
            computeCollectorPoint(coinIssueInfos, livePrices, coinsWithoutCollector, liveMetalPrices, now)?.let { dataPoints.add(it) }
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioCollectorValuationResult(dataPoints)
        } else null
    }

    private fun computeCollectorPoint(
        coinIssueInfos: List<CoinIssueInfo>,
        priceMap: Map<Pair<java.util.UUID, CoinGrade>, BigDecimal>,
        coinsWithoutCollector: List<Coin>,
        metalPrices: Map<MetalType, BigDecimal>,
        timestamp: ZonedDateTime
    ): PortfolioCollectorPoint? {
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        for (info in coinIssueInfos) {
            if (info.coin.createdAt.isAfter(timestamp)) continue
            val quantity = BigDecimal(info.coin.quantity)

            if (info.isExactMatch) {
                val key = info.issueIds.first() to info.grade
                val price = priceMap[key]
                if (price != null) {
                    val value = price.multiply(quantity)
                    minValue = minValue.add(value)
                    maxValue = maxValue.add(value)
                } else {
                    // No collector price available: use metal value as fallback
                    val metalValue = MetalValuationCalculator.coinMetalValue(info.coin, metalPrices)
                    if (metalValue != null) {
                        minValue = minValue.add(metalValue)
                        maxValue = maxValue.add(metalValue)
                    }
                }
            } else {
                val prices = info.issueIds.mapNotNull { issueId ->
                    priceMap[issueId to info.grade]
                }
                if (prices.isNotEmpty()) {
                    minValue = minValue.add(prices.min().multiply(quantity))
                    maxValue = maxValue.add(prices.max().multiply(quantity))
                } else {
                    // No collector prices available: use metal value as fallback
                    val metalValue = MetalValuationCalculator.coinMetalValue(info.coin, metalPrices)
                    if (metalValue != null) {
                        minValue = minValue.add(metalValue)
                        maxValue = maxValue.add(metalValue)
                    }
                }
            }
        }

        // For coins without collector data, use metal value as fallback
        for (coin in coinsWithoutCollector) {
            if (coin.createdAt.isAfter(timestamp)) continue
            val metalValue = MetalValuationCalculator.coinMetalValue(coin, metalPrices) ?: continue
            minValue = minValue.add(metalValue)
            maxValue = maxValue.add(metalValue)
        }

        val hasValues = minValue > BigDecimal.ZERO || maxValue > BigDecimal.ZERO
        if (!hasValues) return null

        return PortfolioCollectorPoint(
            timestamp = timestamp,
            exactValue = null,
            minValue = minValue.setScale(2, RoundingMode.HALF_UP),
            maxValue = maxValue.setScale(2, RoundingMode.HALF_UP)
        )
    }

    /**
     * Computes valuation from stored snapshots for longer timeframes (1w, 1m, 1y, max).
     * Appends a live data point at the current time to avoid stale chart endings.
     */
    private fun computeSnapshotValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        val now = ZonedDateTime.now()
        val startTime = timeframe.getStartTime(now)

        val snapshots = if (startTime != null) {
            portfolioSnapshotRepository.findBySnapshotDateAfter(startTime.toLocalDate())
        } else {
            portfolioSnapshotRepository.findAllOrderBySnapshotDateAsc()
        }

        val coins = coinRepositoryAdapter.findAll()

        if (snapshots.isEmpty() && coins.isEmpty()) {
            return emptyValuationResult(timeframe)
        }

        // Bucket snapshots by timeframe interval
        val zone = ZoneId.systemDefault()
        val bucketedSnapshots = bucketByInterval(snapshots, timeframe) { it.snapshotDate.atStartOfDay(zone) }

        val metalDataPoints = bucketedSnapshots.mapNotNull { (bucketTime, snapshotsInBucket) ->
            val lastSnapshot = snapshotsInBucket.last()
            if (lastSnapshot.metalValue == null) return@mapNotNull null

            PortfolioMetalPoint(
                timestamp = bucketTime,
                totalValue = lastSnapshot.metalValue,
                goldGrams = lastSnapshot.goldGrams ?: BigDecimal.ZERO,
                silverGrams = lastSnapshot.silverGrams ?: BigDecimal.ZERO,
                platinumGrams = lastSnapshot.platinumGrams ?: BigDecimal.ZERO
            )
        }

        val collectorDataPoints = bucketedSnapshots.mapNotNull { (bucketTime, snapshotsInBucket) ->
            val lastSnapshot = snapshotsInBucket.last()
            val hasCollectorValues = lastSnapshot.collectorValueExact != null ||
                lastSnapshot.collectorValueMin != null ||
                lastSnapshot.collectorValueMax != null

            if (!hasCollectorValues) return@mapNotNull null

            PortfolioCollectorPoint(
                timestamp = bucketTime,
                exactValue = lastSnapshot.collectorValueExact,
                minValue = lastSnapshot.collectorValueMin,
                maxValue = lastSnapshot.collectorValueMax
            )
        }

        // Append live data point computed from current state
        val liveMetalPoint = computeLiveMetalPoint(coins, now)
        val liveCollectorPoint = computeLiveCollectorPoint(coins, now)

        val finalMetalPoints = if (liveMetalPoint != null) {
            metalDataPoints + liveMetalPoint
        } else {
            metalDataPoints
        }

        val finalCollectorPoints = if (liveCollectorPoint != null) {
            collectorDataPoints + liveCollectorPoint
        } else {
            collectorDataPoints
        }

        return PortfolioValuationResult(
            timeframe = timeframe,
            currency = DEFAULT_CURRENCY,
            metalValuation = if (finalMetalPoints.isNotEmpty()) {
                PortfolioMetalValuationResult(finalMetalPoints)
            } else null,
            collectorValuation = if (finalCollectorPoints.isNotEmpty()) {
                PortfolioCollectorValuationResult(finalCollectorPoints)
            } else null
        )
    }

    private fun computeLiveMetalPoint(coins: List<Coin>, now: ZonedDateTime): PortfolioMetalPoint? {
        val metalCoins = coins.filter { it.metalType != null && it.purity != null }
        if (metalCoins.isEmpty()) return null

        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }
        if (metalPrices.isEmpty()) return null

        val agg = MetalValuationCalculator.aggregateMetalValues(metalCoins, metalPrices)

        return PortfolioMetalPoint(
            timestamp = now,
            totalValue = agg.totalValue,
            goldGrams = agg.goldGrams,
            silverGrams = agg.silverGrams,
            platinumGrams = agg.platinumGrams
        )
    }

    /**
     * Computes a live collector valuation point from current coin state and latest issue prices.
     * Falls back to metal value for coins without collector data.
     */
    private fun computeLiveCollectorPoint(coins: List<Coin>, now: ZonedDateTime): PortfolioCollectorPoint? {
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }

        for (coin in coins) {
            val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
            val quantity = BigDecimal(coin.quantity)

            if (issueIds.isEmpty()) {
                // No collector data: use metal value as fallback
                val metalValue = MetalValuationCalculator.coinMetalValue(coin, metalPrices)
                if (metalValue != null) {
                    minValue = minValue.add(metalValue)
                    maxValue = maxValue.add(metalValue)
                }
                continue
            }

            val grade = coin.grade ?: CoinGrade.VERY_FINE

            val prices = issueIds.mapNotNull { issueId ->
                issuePriceRepository.findLatestByIssueIdAndGrade(issueId, grade)?.price
            }
            if (prices.isEmpty()) {
                // No prices found for issues: use metal value as fallback
                val metalValue = MetalValuationCalculator.coinMetalValue(coin, metalPrices)
                if (metalValue != null) {
                    minValue = minValue.add(metalValue)
                    maxValue = maxValue.add(metalValue)
                }
                continue
            }

            if (issueIds.size == 1) {
                val price = prices.first().multiply(quantity)
                minValue = minValue.add(price)
                maxValue = maxValue.add(price)
            } else {
                minValue = minValue.add(prices.min().multiply(quantity))
                maxValue = maxValue.add(prices.max().multiply(quantity))
            }
        }

        val hasValues = minValue > BigDecimal.ZERO || maxValue > BigDecimal.ZERO
        if (!hasValues) return null

        return PortfolioCollectorPoint(
            timestamp = now,
            exactValue = null,
            minValue = minValue.setScale(2, RoundingMode.HALF_UP),
            maxValue = maxValue.setScale(2, RoundingMode.HALF_UP)
        )
    }

    private fun emptyValuationResult(timeframe: ValuationTimeframe) = PortfolioValuationResult(
        timeframe = timeframe,
        currency = DEFAULT_CURRENCY,
        metalValuation = null,
        collectorValuation = null
    )
}
