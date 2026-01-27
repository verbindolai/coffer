package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.repository.CoinIssueRepository
import org.coffer.coffer2.repository.IssuePriceRepository
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.util.CollectorValuationCalculator
import org.coffer.coffer2.util.MetalValuationCalculator
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * Computes live portfolio valuations from current prices.
 */
@Component
class LiveValuationService(
    private val coinIssueRepository: CoinIssueRepository,
    private val issuePriceRepository: IssuePriceRepository,
    private val metalQuoteRepository: MetalQuoteRepository
) {

    fun computeLiveMetalPoint(coins: List<Coin>, now: ZonedDateTime): PortfolioMetalPoint? {
        val activeCoins = coins.filter { it.deletedAt == null && it.metalType != null && it.purity != null }
        if (activeCoins.isEmpty()) return null

        val metalPrices = mutableMapOf<MetalType, BigDecimal>()
        MetalType.entries.forEach { metalType ->
            metalQuoteRepository.findLatestByMetalType(metalType)?.let {
                metalPrices[metalType] = it.pricePerGram
            }
        }
        if (metalPrices.isEmpty()) return null

        val agg = MetalValuationCalculator.aggregateMetalValues(activeCoins, metalPrices)

        return PortfolioMetalPoint(
            timestamp = now,
            totalValue = agg.totalValue,
            goldGrams = agg.goldGrams,
            silverGrams = agg.silverGrams,
            platinumGrams = agg.platinumGrams
        )
    }

    fun computeLiveCollectorPoint(coins: List<Coin>, now: ZonedDateTime): PortfolioCollectorPoint? {
        val activeCoins = coins.filter { it.deletedAt == null }
        if (activeCoins.isEmpty()) return null

        val coinIssueInfos = resolveCoinIssueInfos(activeCoins)
        val coinsWithIssues = coinIssueInfos.map { it.coin.id }.toSet()
        val coinsWithoutCollector = activeCoins.filter { it.id !in coinsWithIssues }

        val livePrices = mutableMapOf<Pair<java.util.UUID, CoinGrade>, BigDecimal>()
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

        val agg = CollectorValuationCalculator.aggregateCollectorValues(
            coinIssueInfos, livePrices, coinsWithoutCollector, metalPrices
        )

        val hasValues = agg.minValue > BigDecimal.ZERO || agg.maxValue > BigDecimal.ZERO
        if (!hasValues) return null

        return PortfolioCollectorPoint(
            timestamp = now,
            minValue = agg.minValue,
            maxValue = agg.maxValue
        )
    }

    private fun resolveCoinIssueInfos(coins: List<Coin>): List<CoinIssueInfo> {
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
}
