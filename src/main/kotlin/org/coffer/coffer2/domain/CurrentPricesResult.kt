package org.coffer.coffer2.domain

import org.coffer.coffer2.domain.coin.CoinGrade
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.UUID

data class CurrentPricesResult(
    val coinId: UUID,
    val currency: String,
    val metalValue: MetalValueResult?,
    val collectorPrices: CollectorPricesResult?
)

data class MetalValueResult(
    val metalType: MetalType,
    val pureMetalMassInGrams: BigDecimal,
    val pricePerGram: BigDecimal,
    val totalValue: BigDecimal,
    val timestamp: ZonedDateTime
)

data class CollectorPricesResult(
    val coinGrade: CoinGrade?,
    val hasExactMatch: Boolean,
    val gradePrices: List<GradePriceResult>,
    val timestamp: ZonedDateTime?
)

data class GradePriceResult(
    val grade: CoinGrade,
    val minPrice: BigDecimal,
    val maxPrice: BigDecimal
)
