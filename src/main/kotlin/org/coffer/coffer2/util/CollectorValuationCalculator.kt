package org.coffer.coffer2.util

import org.coffer.coffer2.application.portfolio.CoinIssueInfo
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class CollectorAggregation(
    val minValue: BigDecimal,
    val maxValue: BigDecimal
)

object CollectorValuationCalculator {

    fun aggregateCollectorValues(
        coinIssueInfos: List<CoinIssueInfo>,
        priceMap: Map<Pair<UUID, CoinGrade>, BigDecimal>,
        coinsWithoutCollector: List<Coin>,
        metalPrices: Map<MetalType, BigDecimal>,
        coinFilter: (Coin) -> Boolean = { true }
    ): CollectorAggregation {
        var minValue = BigDecimal.ZERO
        var maxValue = BigDecimal.ZERO

        for (info in coinIssueInfos) {
            if (!coinFilter(info.coin)) continue
            val quantity = BigDecimal(info.coin.quantity)

            if (info.isExactMatch) {
                val key = info.issueIds.first() to info.grade
                val price = priceMap[key]
                if (price != null) {
                    val value = price.multiply(quantity)
                    minValue = minValue.add(value)
                    maxValue = maxValue.add(value)
                } else {
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
                    val metalValue = MetalValuationCalculator.coinMetalValue(info.coin, metalPrices)
                    if (metalValue != null) {
                        minValue = minValue.add(metalValue)
                        maxValue = maxValue.add(metalValue)
                    }
                }
            }
        }

        for (coin in coinsWithoutCollector) {
            if (!coinFilter(coin)) continue
            val metalValue = MetalValuationCalculator.coinMetalValue(coin, metalPrices) ?: continue
            minValue = minValue.add(metalValue)
            maxValue = maxValue.add(metalValue)
        }

        return CollectorAggregation(
            minValue = minValue.setScale(2, RoundingMode.HALF_UP),
            maxValue = maxValue.setScale(2, RoundingMode.HALF_UP)
        )
    }
}
