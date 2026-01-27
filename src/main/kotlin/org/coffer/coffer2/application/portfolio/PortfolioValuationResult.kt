package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.ValuationTimeframe
import java.math.BigDecimal
import java.time.ZonedDateTime

data class PortfolioValuationResult(
    val timeframe: ValuationTimeframe,
    val currency: String,
    val metalValuation: PortfolioMetalValuationResult?,
    val collectorValuation: PortfolioCollectorValuationResult?
) {
    companion object {
        const val DEFAULT_CURRENCY = "EUR"

        fun create(
            timeframe: ValuationTimeframe,
            metalValuation: PortfolioMetalValuationResult?,
            collectorValuation: PortfolioCollectorValuationResult?,
            currency: String? = null
        ): PortfolioValuationResult = PortfolioValuationResult(timeframe, currency ?: DEFAULT_CURRENCY, metalValuation, collectorValuation)

        fun empty(timeframe: ValuationTimeframe) = PortfolioValuationResult(timeframe, DEFAULT_CURRENCY, null, null)
    }
}

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
    val minValue: BigDecimal?,
    val maxValue: BigDecimal?
)
