package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.bucketByInterval
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.util.MetalValuationCalculator
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZonedDateTime

@Component
class RealTimeMetalValuationService(
    private val metalQuoteRepository: MetalQuoteRepository
) {

    fun computeTimeSeries(
        coins: List<Coin>,
        startTime: ZonedDateTime,
        timeframe: ValuationTimeframe
    ): PortfolioMetalValuationResult? {
        val now = ZonedDateTime.now()
        val metalCoins = coins.filter { it.metalType != null && it.purity != null }
        if (metalCoins.isEmpty()) return null

        val lastKnownPrices = mutableMapOf<MetalType, BigDecimal>()
        val seedQuotes = metalQuoteRepository.findLatestBefore(startTime)
        seedQuotes.forEach { quote ->
            lastKnownPrices[quote.metalType] = quote.pricePerGram
        }

        val allQuotes = MetalType.entries.flatMap { metalType ->
            metalQuoteRepository.findByMetalTypeAndQuotedAtAfter(metalType, startTime)
        }

        val bucketedQuotes = bucketByInterval(allQuotes, timeframe) { it.quotedAt }

        val dataPoints = mutableListOf<PortfolioMetalPoint>()
        val bucketStartTime = timeframe.truncateToBucket(startTime)

        val firstBucketTime = bucketedQuotes.firstOrNull()?.first
        if (lastKnownPrices.isNotEmpty() && firstBucketTime != bucketStartTime) {
            computeMetalPoint(metalCoins, lastKnownPrices, bucketStartTime)?.let { dataPoints.add(it) }
        }

        for ((bucketTime, quotesInBucket) in bucketedQuotes) {
            val pricesByMetal = quotesInBucket
                .groupBy { it.metalType }
                .mapValues { (_, quotes) -> quotes.last().pricePerGram }

            pricesByMetal.forEach { (metalType, price) ->
                lastKnownPrices[metalType] = price
            }

            computeMetalPoint(metalCoins, lastKnownPrices, bucketTime)?.let { dataPoints.add(it) }
        }

        val lastBucketTime = dataPoints.lastOrNull()?.timestamp
        val liveBucketTime = timeframe.truncateToBucket(now)
        if (lastBucketTime == null || liveBucketTime != lastBucketTime) {
            computeLivePoint(metalCoins, now)?.let { dataPoints.add(it) }
        }

        return if (dataPoints.isNotEmpty()) {
            PortfolioMetalValuationResult(dataPoints)
        } else null
    }

    fun computeLivePoint(coins: List<Coin>, now: ZonedDateTime): PortfolioMetalPoint? {
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
}
