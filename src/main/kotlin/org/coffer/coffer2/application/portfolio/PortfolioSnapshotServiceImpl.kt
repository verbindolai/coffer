package org.coffer.coffer2.application.portfolio

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.MetalType
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
import java.time.ZonedDateTime
import java.util.UUID

@Service
class PortfolioSnapshotServiceImpl(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val metalQuoteRepository: MetalQuoteRepository,
    private val issuePriceRepository: IssuePriceRepository,
    private val coinIssueRepository: CoinIssueRepository,
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository
) : PortfolioSnapshotService {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val DEFAULT_CURRENCY = "EUR"
    }

    @Transactional
    override fun computeAndStoreSnapshot(): PortfolioSnapshotResult {
        logger.info { "Computing portfolio snapshot" }

        val coins = coinRepositoryAdapter.findAll()
        val today = LocalDate.now()

        if (coins.isEmpty()) {
            logger.info { "No coins in portfolio, creating empty snapshot" }
            return createAndSaveEmptySnapshot(today)
        }

        // Compute metal valuations
        val metalValuation = computeMetalValuation(coins)

        // Compute collector valuations
        val collectorValuation = computeCollectorValuation(coins)

        val totalCoins = coins.size
        val totalQuantity = coins.sumOf { it.quantity }

        val entity = PortfolioSnapshotEntity(
            id = UUID.randomUUID(),
            snapshotDate = today,
            currencyCode = DEFAULT_CURRENCY,
            totalCoins = totalCoins,
            totalQuantity = totalQuantity,
            metalValue = metalValuation.totalValue,
            goldGrams = metalValuation.goldGrams,
            silverGrams = metalValuation.silverGrams,
            platinumGrams = metalValuation.platinumGrams,
            collectorValueExact = collectorValuation.exactValue,
            collectorValueMin = collectorValuation.minValue,
            collectorValueMax = collectorValuation.maxValue,
            createdAt = ZonedDateTime.now()
        )

        val saved = portfolioSnapshotRepository.save(entity)
        logger.info { "Portfolio snapshot saved for $today: totalCoins=$totalCoins, totalQuantity=$totalQuantity" }

        return PortfolioSnapshotResult.from(saved)
    }

    @Transactional(readOnly = true)
    override fun getSnapshotsAfter(startDate: LocalDate): List<PortfolioSnapshotResult> {
        return portfolioSnapshotRepository.findBySnapshotDateAfter(startDate)
            .map { PortfolioSnapshotResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getAllSnapshots(): List<PortfolioSnapshotResult> {
        return portfolioSnapshotRepository.findAllOrderBySnapshotDateAsc()
            .map { PortfolioSnapshotResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getLatestSnapshot(): PortfolioSnapshotResult? {
        return portfolioSnapshotRepository.findLatest()?.let { PortfolioSnapshotResult.from(it) }
    }

    private fun createAndSaveEmptySnapshot(date: LocalDate): PortfolioSnapshotResult {
        val entity = PortfolioSnapshotEntity(
            id = UUID.randomUUID(),
            snapshotDate = date,
            currencyCode = DEFAULT_CURRENCY,
            totalCoins = 0,
            totalQuantity = 0,
            metalValue = BigDecimal.ZERO,
            goldGrams = BigDecimal.ZERO,
            silverGrams = BigDecimal.ZERO,
            platinumGrams = BigDecimal.ZERO,
            collectorValueExact = BigDecimal.ZERO,
            collectorValueMin = BigDecimal.ZERO,
            collectorValueMax = BigDecimal.ZERO,
            createdAt = ZonedDateTime.now()
        )

        return PortfolioSnapshotResult.from(portfolioSnapshotRepository.save(entity))
    }

    private fun computeMetalValuation(coins: List<Coin>): MetalValuationAggregation {
        var totalValue = BigDecimal.ZERO
        var goldGrams = BigDecimal.ZERO
        var silverGrams = BigDecimal.ZERO
        var platinumGrams = BigDecimal.ZERO

        // Get latest prices for each metal type
        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }

        for (coin in coins) {
            val metalType = coin.metalType ?: continue
            val purity = coin.purity ?: continue
            val pricePerGram = metalPrices[metalType] ?: continue

            // Calculate pure metal mass in grams
            val pureMetalMass = coin.weightInGrams.multiply(purity)
                .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)

            // Apply quantity multiplier
            val totalPureMetal = pureMetalMass.multiply(BigDecimal(coin.quantity))

            // Accumulate by metal type (only precious metals tracked)
            when (metalType) {
                MetalType.GOLD -> goldGrams = goldGrams.add(totalPureMetal)
                MetalType.SILVER -> silverGrams = silverGrams.add(totalPureMetal)
                MetalType.PLATINUM -> platinumGrams = platinumGrams.add(totalPureMetal)
                MetalType.NICKEL -> {} // Non-precious metal, skip gram accumulation
            }

            // Calculate value
            val coinMetalValue = totalPureMetal.multiply(pricePerGram)
                .setScale(2, RoundingMode.HALF_UP)
            totalValue = totalValue.add(coinMetalValue)
        }

        return MetalValuationAggregation(
            totalValue = totalValue,
            goldGrams = goldGrams.setScale(6, RoundingMode.HALF_UP),
            silverGrams = silverGrams.setScale(6, RoundingMode.HALF_UP),
            platinumGrams = platinumGrams.setScale(6, RoundingMode.HALF_UP)
        )
    }

    private fun computeCollectorValuation(coins: List<Coin>): CollectorValuationAggregation {
        var exactValue = BigDecimal.ZERO
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        for (coin in coins) {
            val issueIds = coinIssueRepository.findIssueIdsByCoinId(coin.id)
            if (issueIds.isEmpty()) continue

            val grade = coin.grade ?: CoinGrade.VERY_FINE

            // Get latest prices for each issue
            val prices = issueIds.mapNotNull { issueId ->
                issuePriceRepository.findLatestByIssueIdAndGrade(issueId, grade)?.price
            }

            if (prices.isEmpty()) continue

            val quantity = BigDecimal(coin.quantity)

            if (issueIds.size == 1) {
                // Exact match: single issue, use exact price
                val price = prices.first()
                exactValue = exactValue.add(price.multiply(quantity))
            } else {
                // Multiple issues: use min/max range
                val min = prices.minOrNull() ?: continue
                val max = prices.maxOrNull() ?: continue
                minValue = minValue.add(min.multiply(quantity))
                maxValue = maxValue.add(max.multiply(quantity))
            }
        }

        return CollectorValuationAggregation(
            exactValue = exactValue.setScale(2, RoundingMode.HALF_UP),
            minValue = minValue.setScale(2, RoundingMode.HALF_UP),
            maxValue = maxValue.setScale(2, RoundingMode.HALF_UP)
        )
    }

    private data class MetalValuationAggregation(
        val totalValue: BigDecimal,
        val goldGrams: BigDecimal,
        val silverGrams: BigDecimal,
        val platinumGrams: BigDecimal
    )

    private data class CollectorValuationAggregation(
        val exactValue: BigDecimal,
        val minValue: BigDecimal,
        val maxValue: BigDecimal
    )
}
