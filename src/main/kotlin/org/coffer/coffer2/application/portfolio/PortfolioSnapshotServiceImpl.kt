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
import org.coffer.coffer2.util.CollectorAggregation
import org.coffer.coffer2.util.CollectorValuationCalculator
import org.coffer.coffer2.util.MetalAggregation
import org.coffer.coffer2.util.MetalValuationCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
            collectorValueExact = BigDecimal.ZERO,
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

    private fun computeCollectorValuation(coins: List<Coin>): CollectorAggregation {
        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }

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

        val priceMap = mutableMapOf<Pair<UUID, CoinGrade>, BigDecimal>()
        for (info in coinIssueInfos) {
            for (issueId in info.issueIds) {
                issuePriceRepository.findLatestByIssueIdAndGrade(issueId, info.grade)?.let {
                    priceMap[it.issueId to it.grade] = it.price
                }
            }
        }

        return CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos, priceMap, coinsWithoutCollector, metalPrices
        )
    }
}
