package org.coffer.coffer2.application.portfolio

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.repository.PortfolioSnapshotEntity
import org.coffer.coffer2.repository.PortfolioSnapshotRepository
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
        var totalValue = BigDecimal.ZERO
        var goldGrams = BigDecimal.ZERO
        var silverGrams = BigDecimal.ZERO
        var platinumGrams = BigDecimal.ZERO

        for (coin in metalCoins) {
            if (coin.createdAt.isAfter(timestamp)) continue

            val metalType = coin.metalType!!
            val pricePerGram = pricesByMetal[metalType] ?: continue

            val pureMetalMass = coin.weightInGrams.multiply(coin.purity!!)
                .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)
            val totalPureMetal = pureMetalMass.multiply(BigDecimal(coin.quantity))

            when (metalType) {
                MetalType.GOLD -> goldGrams = goldGrams.add(totalPureMetal)
                MetalType.SILVER -> silverGrams = silverGrams.add(totalPureMetal)
                MetalType.PLATINUM -> platinumGrams = platinumGrams.add(totalPureMetal)
                MetalType.NICKEL, MetalType.BASE_METAL -> {}
            }

            totalValue = totalValue.add(totalPureMetal.multiply(pricePerGram))
        }

        if (totalValue == BigDecimal.ZERO) return null

        return PortfolioMetalPoint(
            timestamp = timestamp,
            totalValue = totalValue.setScale(2, RoundingMode.HALF_UP),
            goldGrams = goldGrams.setScale(6, RoundingMode.HALF_UP),
            silverGrams = silverGrams.setScale(6, RoundingMode.HALF_UP),
            platinumGrams = platinumGrams.setScale(6, RoundingMode.HALF_UP)
        )
    }

    private fun computeRealTimeCollectorValuation(
        coins: List<Coin>,
        startTime: ZonedDateTime,
        timeframe: ValuationTimeframe
    ): PortfolioCollectorValuationResult? {
        val now = ZonedDateTime.now()

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

        if (coinIssueInfos.isEmpty()) return null

        val allIssueIds = coinIssueInfos.flatMap { it.issueIds }.distinct()

        // Seed last known prices from before the window start
        val lastKnownPrices = mutableMapOf<Pair<java.util.UUID, CoinGrade>, BigDecimal>()
        val grades = coinIssueInfos.map { it.grade }.distinct()
        for (grade in grades) {
            val seedPrices = issuePriceRepository.findLatestBeforeByIssueIdsAndGrade(allIssueIds, grade, startTime)
            seedPrices.forEach { entity ->
                lastKnownPrices[entity.issueId to entity.grade] = entity.price
            }
        }

        // Fetch prices within the time range
        val allPrices = issuePriceRepository.findByIssueIdsAfter(allIssueIds, startTime)

        val bucketedPrices = bucketByInterval(allPrices, timeframe) { it.createdAt }

        val dataPoints = mutableListOf<PortfolioCollectorPoint>()
        val bucketStartTime = timeframe.truncateToBucket(startTime)

        // Generate initial data point at window start using seeded prices
        val firstBucketTime = bucketedPrices.firstOrNull()?.first
        if (lastKnownPrices.isNotEmpty() && firstBucketTime != bucketStartTime) {
            computeCollectorPoint(coinIssueInfos, lastKnownPrices, bucketStartTime)?.let { dataPoints.add(it) }
        }

        // Process bucketed prices within the window
        for ((bucketTime, pricesInBucket) in bucketedPrices) {
            val bucketPriceMap = pricesInBucket
                .groupBy { it.issueId to it.grade }
                .mapValues { (_, prices) -> prices.last().price }

            bucketPriceMap.forEach { (key, price) ->
                lastKnownPrices[key] = price
            }

            computeCollectorPoint(coinIssueInfos, lastKnownPrices, bucketTime)?.let { dataPoints.add(it) }
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
            if (livePrices.isNotEmpty()) {
                computeCollectorPoint(coinIssueInfos, livePrices, now)?.let { dataPoints.add(it) }
            }
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioCollectorValuationResult(dataPoints)
        } else null
    }

    private fun computeCollectorPoint(
        coinIssueInfos: List<CoinIssueInfo>,
        priceMap: Map<Pair<java.util.UUID, CoinGrade>, BigDecimal>,
        timestamp: ZonedDateTime
    ): PortfolioCollectorPoint? {
        var exactValue = BigDecimal.ZERO
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        for (info in coinIssueInfos) {
            val quantity = BigDecimal(info.coin.quantity)

            if (info.isExactMatch) {
                val key = info.issueIds.first() to info.grade
                val price = priceMap[key] ?: continue
                exactValue = exactValue.add(price.multiply(quantity))
            } else {
                val prices = info.issueIds.mapNotNull { issueId ->
                    priceMap[issueId to info.grade]
                }
                if (prices.isEmpty()) continue

                minValue = minValue.add(prices.minOrNull()!!.multiply(quantity))
                maxValue = maxValue.add(prices.maxOrNull()!!.multiply(quantity))
            }
        }

        val hasValues = exactValue > BigDecimal.ZERO || minValue > BigDecimal.ZERO || maxValue > BigDecimal.ZERO
        if (!hasValues) return null

        return PortfolioCollectorPoint(
            timestamp = timestamp,
            exactValue = if (exactValue > BigDecimal.ZERO) exactValue.setScale(2, RoundingMode.HALF_UP) else null,
            minValue = if (minValue > BigDecimal.ZERO) minValue.setScale(2, RoundingMode.HALF_UP) else null,
            maxValue = if (maxValue > BigDecimal.ZERO) maxValue.setScale(2, RoundingMode.HALF_UP) else null
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
        val bucketedSnapshots = bucketSnapshotsByInterval(snapshots, timeframe)

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

    /**
     * Computes a live metal valuation point from current coin state and latest metal quotes.
     */
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

        var totalValue = BigDecimal.ZERO
        var goldGrams = BigDecimal.ZERO
        var silverGrams = BigDecimal.ZERO
        var platinumGrams = BigDecimal.ZERO

        for (coin in metalCoins) {
            val metalType = coin.metalType!!
            val pricePerGram = metalPrices[metalType] ?: continue

            val pureMetalMass = coin.weightInGrams.multiply(coin.purity!!)
                .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)
            val totalPureMetal = pureMetalMass.multiply(BigDecimal(coin.quantity))

            when (metalType) {
                MetalType.GOLD -> goldGrams = goldGrams.add(totalPureMetal)
                MetalType.SILVER -> silverGrams = silverGrams.add(totalPureMetal)
                MetalType.PLATINUM -> platinumGrams = platinumGrams.add(totalPureMetal)
                MetalType.NICKEL, MetalType.BASE_METAL -> {}
            }

            totalValue = totalValue.add(totalPureMetal.multiply(pricePerGram))
        }

        return PortfolioMetalPoint(
            timestamp = now,
            totalValue = totalValue.setScale(2, RoundingMode.HALF_UP),
            goldGrams = goldGrams.setScale(6, RoundingMode.HALF_UP),
            silverGrams = silverGrams.setScale(6, RoundingMode.HALF_UP),
            platinumGrams = platinumGrams.setScale(6, RoundingMode.HALF_UP)
        )
    }

    /**
     * Computes a live collector valuation point from current coin state and latest issue prices.
     */
    private fun computeLiveCollectorPoint(coins: List<Coin>, now: ZonedDateTime): PortfolioCollectorPoint? {
        var exactValue = BigDecimal.ZERO
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        for (coin in coins) {
            val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
            if (issueIds.isEmpty()) continue

            val grade = coin.grade ?: CoinGrade.VERY_FINE

            val prices = issueIds.mapNotNull { issueId ->
                issuePriceRepository.findLatestByIssueIdAndGrade(issueId, grade)?.price
            }
            if (prices.isEmpty()) continue

            val quantity = BigDecimal(coin.quantity)

            if (issueIds.size == 1) {
                val price = prices.first()
                exactValue = exactValue.add(price.multiply(quantity))
            } else {
                val min = prices.minOrNull() ?: continue
                val max = prices.maxOrNull() ?: continue
                minValue = minValue.add(min.multiply(quantity))
                maxValue = maxValue.add(max.multiply(quantity))
            }
        }

        val hasValues = exactValue > BigDecimal.ZERO || minValue > BigDecimal.ZERO || maxValue > BigDecimal.ZERO
        if (!hasValues) return null

        return PortfolioCollectorPoint(
            timestamp = now,
            exactValue = if (exactValue > BigDecimal.ZERO) exactValue.setScale(2, RoundingMode.HALF_UP) else null,
            minValue = if (minValue > BigDecimal.ZERO) minValue.setScale(2, RoundingMode.HALF_UP) else null,
            maxValue = if (maxValue > BigDecimal.ZERO) maxValue.setScale(2, RoundingMode.HALF_UP) else null
        )
    }

    private fun <T> bucketByInterval(
        items: List<T>,
        timeframe: ValuationTimeframe,
        timestampSelector: (T) -> ZonedDateTime
    ): List<Pair<ZonedDateTime, List<T>>> {
        if (items.isEmpty()) return emptyList()

        val grouped = items.groupBy { item ->
            timeframe.truncateToBucket(timestampSelector(item))
        }

        return grouped.entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }

    private fun bucketSnapshotsByInterval(
        snapshots: List<PortfolioSnapshotEntity>,
        timeframe: ValuationTimeframe
    ): List<Pair<ZonedDateTime, List<PortfolioSnapshotEntity>>> {
        if (snapshots.isEmpty()) return emptyList()

        val zone = ZoneId.systemDefault()

        val grouped = snapshots.groupBy { snapshot ->
            val zonedTime = snapshot.snapshotDate.atStartOfDay(zone)
            timeframe.truncateToBucket(zonedTime)
        }

        return grouped.entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }

    private fun emptyValuationResult(timeframe: ValuationTimeframe) = PortfolioValuationResult(
        timeframe = timeframe,
        currency = DEFAULT_CURRENCY,
        metalValuation = null,
        collectorValuation = null
    )
}
