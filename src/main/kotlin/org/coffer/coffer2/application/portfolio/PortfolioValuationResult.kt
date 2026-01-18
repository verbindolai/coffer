package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.ValuationTimeframe
import java.math.BigDecimal
import java.time.ZonedDateTime

data class PortfolioValuationResult(
    val timeframe: ValuationTimeframe,
    val currency: String,
    val metalValuation: PortfolioMetalValuationResult?,
    val collectorValuation: PortfolioCollectorValuationResult?
)

data class PortfolioMetalValuationResult(
    val dataPoints: List<PortfolioMetalPoint>
)

data class PortfolioMetalPoint(
    val timestamp: ZonedDateTime,
    val totalValue: BigDecimal,
    val goldGrams: BigDecimal,
    val silverGrams: BigDecimal,
    val platinumGrams: BigDecimal
)

data class PortfolioCollectorValuationResult(
    val dataPoints: List<PortfolioCollectorPoint>
)

data class PortfolioCollectorPoint(
    val timestamp: ZonedDateTime,
    val exactValue: BigDecimal?,
    val minValue: BigDecimal?,
    val maxValue: BigDecimal?
)
