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
import org.coffer.coffer2.util.MetalAggregation
import org.coffer.coffer2.util.MetalValuationCalculator
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
        logger.debug { "Computing portfolio snapshot" }

        val coins = coinRepositoryAdapter.findAll()
        val today = LocalDate.now()

        if (coins.isEmpty()) {
            logger.debug { "No coins in portfolio, creating empty snapshot" }
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

    private fun computeMetalValuation(coins: List<Coin>): MetalAggregation {
        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }

        return MetalValuationCalculator.aggregateMetalValues(coins, metalPrices)
    }

    private fun computeCollectorValuation(coins: List<Coin>): CollectorValuationAggregation {
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        // Get latest metal prices for fallback
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

            // Get latest prices for each issue
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

        return CollectorValuationAggregation(
            exactValue = BigDecimal.ZERO,
            minValue = minValue.setScale(2, RoundingMode.HALF_UP),
            maxValue = maxValue.setScale(2, RoundingMode.HALF_UP)
        )
    }

    private data class CollectorValuationAggregation(
        val exactValue: BigDecimal,
        val minValue: BigDecimal,
        val maxValue: BigDecimal
    )
}
