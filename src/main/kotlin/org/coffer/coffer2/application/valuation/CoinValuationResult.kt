package org.coffer.coffer2.application.valuation

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.CoinGrade
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.UUID

data class CoinValuationResult(
    val coinId: UUID,
    val timeframe: ValuationTimeframe,
    val metalValuation: MetalValuationResult?,
    val issueValuation: IssueValuationResult?
)

data class MetalValuationResult(
    val metalType: MetalType,
    val pureMetalMassInGrams: BigDecimal,
    val currency: String,
    val dataPoints: List<MetalValuationPoint>
)

data class MetalValuationPoint(
    val timestamp: ZonedDateTime,
    val pricePerGram: BigDecimal,
    val totalValue: BigDecimal
)

data class IssueValuationResult(
    val grade: CoinGrade?,
    val isExactMatch: Boolean,
    val currency: String,
    val dataPoints: List<IssueValuationPoint>
)

data class IssueValuationPoint(
    val timestamp: ZonedDateTime,
    val price: BigDecimal?,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?
)
