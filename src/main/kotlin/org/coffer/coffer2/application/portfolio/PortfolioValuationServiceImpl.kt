package org.coffer.coffer2.application.portfolio

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceEntity
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteEntity
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

    companion object {
        private const val DEFAULT_CURRENCY = "EUR"
        private val REAL_TIME_TIMEFRAMES = setOf(ValuationTimeframe.HOUR_1, ValuationTimeframe.DAY_1)
    }

    @Transactional(readOnly = true)
    override fun getValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        logger.info { "Getting portfolio valuation for timeframe ${timeframe.code}" }

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
        // Get coins grouped by metal type with their pure metal mass
        val metalCoins = coins.filter { it.metalType != null && it.purity != null }
        if (metalCoins.isEmpty()) return null

        // Get all metal quotes for the time range
        val quotesByMetal = MetalType.entries.associateWith { metalType ->
            metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, startTime)
        }

        // Group all quotes by bucket time
        val allQuotes = quotesByMetal.values.flatten()
        if (allQuotes.isEmpty()) return null

        val bucketedQuotes = bucketByInterval(allQuotes, timeframe) { it.quotedAt }

        // Track last known price for each metal type (forward-fill for missing data)
        val lastKnownPrices = mutableMapOf<MetalType, BigDecimal>()

        val dataPoints = bucketedQuotes.map { (bucketTime, quotesInBucket) ->
            // Get the last price for each metal type in this bucket
            val pricesByMetal = quotesInBucket
                .groupBy { it.metalType }
                .mapValues { (_, quotes) -> quotes.last().pricePerGram }

            // Update last known prices with any new prices from this bucket
            pricesByMetal.forEach { (metalType, price) ->
                lastKnownPrices[metalType] = price
            }

            // Calculate totals for this bucket
            var totalValue = BigDecimal.ZERO
            var goldGrams = BigDecimal.ZERO
            var silverGrams = BigDecimal.ZERO
            var platinumGrams = BigDecimal.ZERO

            for (coin in metalCoins) {
                val metalType = coin.metalType!!
                // Use current bucket price, or fall back to last known price
                val pricePerGram = pricesByMetal[metalType] ?: lastKnownPrices[metalType] ?: continue

                val pureMetalMass = coin.weightInGrams.multiply(coin.purity!!)
                    .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)
                val totalPureMetal = pureMetalMass.multiply(BigDecimal(coin.quantity))

                when (metalType) {
                    MetalType.GOLD -> goldGrams = goldGrams.add(totalPureMetal)
                    MetalType.SILVER -> silverGrams = silverGrams.add(totalPureMetal)
                    MetalType.PLATINUM -> platinumGrams = platinumGrams.add(totalPureMetal)
                    MetalType.NICKEL, MetalType.BASE_METAL -> {} // Non-precious metal, skip gram accumulation
                }

                totalValue = totalValue.add(totalPureMetal.multiply(pricePerGram))
            }

            PortfolioMetalPoint(
                timestamp = bucketTime,
                totalValue = totalValue.setScale(2, RoundingMode.HALF_UP),
                goldGrams = goldGrams.setScale(6, RoundingMode.HALF_UP),
                silverGrams = silverGrams.setScale(6, RoundingMode.HALF_UP),
                platinumGrams = platinumGrams.setScale(6, RoundingMode.HALF_UP)
            )
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioMetalValuationResult(dataPoints)
        } else null
    }

    private fun computeRealTimeCollectorValuation(
        coins: List<Coin>,
        startTime: ZonedDateTime,
        timeframe: ValuationTimeframe
    ): PortfolioCollectorValuationResult? {
        // Build map of coin -> issue ids and grades
        data class CoinIssueInfo(
            val coin: Coin,
            val issueIds: List<java.util.UUID>,
            val grade: CoinGrade,
            val isExactMatch: Boolean
        )

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

        // Get all issue IDs and fetch prices
        val allIssueIds = coinIssueInfos.flatMap { it.issueIds }.distinct()

        // Fetch all prices for all issues in the time range
        val allPrices = issuePriceRepository.findByIssueIdsAfter(allIssueIds, startTime)
        if (allPrices.isEmpty()) return null

        // Bucket prices by time
        val bucketedPrices = bucketByInterval(allPrices, timeframe) { it.createdAt }

        // Track last known price for each (issueId, grade) combination (forward-fill for missing data)
        val lastKnownPrices = mutableMapOf<Pair<java.util.UUID, CoinGrade>, BigDecimal>()

        val dataPoints = bucketedPrices.map { (bucketTime, pricesInBucket) ->
            // Get the last price for each (issueId, grade) combination in this bucket
            val bucketPriceMap = pricesInBucket
                .groupBy { it.issueId to it.grade }
                .mapValues { (_, prices) -> prices.last().price }

            // Update last known prices with any new prices from this bucket
            bucketPriceMap.forEach { (key, price) ->
                lastKnownPrices[key] = price
            }

            var exactValue = BigDecimal.ZERO
            var minValue = BigDecimal.ZERO
            var maxValue = BigDecimal.ZERO

            for (info in coinIssueInfos) {
                val quantity = BigDecimal(info.coin.quantity)

                if (info.isExactMatch) {
                    val key = info.issueIds.first() to info.grade
                    // Use current bucket price, or fall back to last known price
                    val price = bucketPriceMap[key] ?: lastKnownPrices[key] ?: continue
                    exactValue = exactValue.add(price.multiply(quantity))
                } else {
                    val prices = info.issueIds.mapNotNull { issueId ->
                        val key = issueId to info.grade
                        // Use current bucket price, or fall back to last known price
                        bucketPriceMap[key] ?: lastKnownPrices[key]
                    }
                    if (prices.isEmpty()) continue

                    val min = prices.minOrNull()!!
                    val max = prices.maxOrNull()!!
                    minValue = minValue.add(min.multiply(quantity))
                    maxValue = maxValue.add(max.multiply(quantity))
                }
            }

            PortfolioCollectorPoint(
                timestamp = bucketTime,
                exactValue = if (exactValue > BigDecimal.ZERO) exactValue.setScale(2, RoundingMode.HALF_UP) else null,
                minValue = if (minValue > BigDecimal.ZERO) minValue.setScale(2, RoundingMode.HALF_UP) else null,
                maxValue = if (maxValue > BigDecimal.ZERO) maxValue.setScale(2, RoundingMode.HALF_UP) else null
            )
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioCollectorValuationResult(dataPoints)
        } else null
    }

    /**
     * Computes valuation from stored snapshots for longer timeframes (1w, 1m, 1y, max).
     */
    private fun computeSnapshotValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        val now = ZonedDateTime.now()
        val startTime = timeframe.getStartTime(now)

        val snapshots = if (startTime != null) {
            portfolioSnapshotRepository.findBySnapshotDateAfter(startTime.toLocalDate())
        } else {
            portfolioSnapshotRepository.findAllOrderBySnapshotDateAsc()
        }

        if (snapshots.isEmpty()) {
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

        return PortfolioValuationResult(
            timeframe = timeframe,
            currency = DEFAULT_CURRENCY,
            metalValuation = if (metalDataPoints.isNotEmpty()) {
                PortfolioMetalValuationResult(metalDataPoints)
            } else null,
            collectorValuation = if (collectorDataPoints.isNotEmpty()) {
                PortfolioCollectorValuationResult(collectorDataPoints)
            } else null
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
