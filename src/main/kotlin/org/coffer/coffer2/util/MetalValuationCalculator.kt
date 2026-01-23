package org.coffer.coffer2.util

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import java.math.BigDecimal
import java.math.RoundingMode

data class MetalAggregation(
    val totalValue: BigDecimal,
    val goldGrams: BigDecimal,
    val silverGrams: BigDecimal,
    val platinumGrams: BigDecimal
)

object MetalValuationCalculator {

    fun pureMetalMass(weightInGrams: BigDecimal, purity: BigDecimal): BigDecimal =
        weightInGrams.multiply(purity)
            .divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)

    fun coinMetalValue(coin: Coin, metalPrices: Map<MetalType, BigDecimal>): BigDecimal? {
        val metalType = coin.metalType ?: return null
        val purity = coin.purity ?: return null
        val pricePerGram = metalPrices[metalType] ?: return null

        val pureMass = pureMetalMass(coin.weightInGrams, purity)
        val totalPureMetal = pureMass.multiply(BigDecimal(coin.quantity))
        return totalPureMetal.multiply(pricePerGram)
    }

    fun aggregateMetalValues(
        coins: List<Coin>,
        metalPrices: Map<MetalType, BigDecimal>,
        coinFilter: (Coin) -> Boolean = { true }
    ): MetalAggregation {
        var totalValue = BigDecimal.ZERO
        var goldGrams = BigDecimal.ZERO
        var silverGrams = BigDecimal.ZERO
        var platinumGrams = BigDecimal.ZERO

        for (coin in coins) {
            if (!coinFilter(coin)) continue
            val metalType = coin.metalType ?: continue
            val purity = coin.purity ?: continue
            val pricePerGram = metalPrices[metalType] ?: continue

            val pureMass = pureMetalMass(coin.weightInGrams, purity)
            val totalPureMetal = pureMass.multiply(BigDecimal(coin.quantity))

            when (metalType) {
                MetalType.GOLD -> goldGrams = goldGrams.add(totalPureMetal)
                MetalType.SILVER -> silverGrams = silverGrams.add(totalPureMetal)
                MetalType.PLATINUM -> platinumGrams = platinumGrams.add(totalPureMetal)
                MetalType.NICKEL, MetalType.BASE_METAL -> {}
            }

            totalValue = totalValue.add(totalPureMetal.multiply(pricePerGram))
        }

        return MetalAggregation(
            totalValue = totalValue.setScale(2, RoundingMode.HALF_UP),
            goldGrams = goldGrams.setScale(6, RoundingMode.HALF_UP),
            silverGrams = silverGrams.setScale(6, RoundingMode.HALF_UP),
            platinumGrams = platinumGrams.setScale(6, RoundingMode.HALF_UP)
        )
    }
}
