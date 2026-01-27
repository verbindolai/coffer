package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.application.portfolio.PortfolioCollectorPoint
import org.coffer.coffer2.application.portfolio.PortfolioCollectorValuationResult
import org.coffer.coffer2.application.portfolio.PortfolioMetalPoint
import org.coffer.coffer2.application.portfolio.PortfolioMetalValuationResult
import org.coffer.coffer2.application.portfolio.PortfolioValuationResult
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Aggregated portfolio valuation including metal and collector values")
data class PortfolioValuationResponse(
    @Schema(description = "Requested timeframe", example = "1d", allowableValues = ["1h", "1d", "1w", "1m", "1y", "max"])
    val timeframe: String,

    @Schema(description = "Currency code for all values", example = "USD")
    val currency: String,

    @Schema(description = "Aggregated metal-based valuation (null if no coins with metal content)")
    val metalValuation: PortfolioMetalValuationResponse?,

    @Schema(description = "Aggregated collector valuation (null if no coins with issue data)")
    val collectorValuation: PortfolioCollectorValuationResponse?
) {
    companion object {
        fun from(result: PortfolioValuationResult) = PortfolioValuationResponse(
            timeframe = result.timeframe.code,
            currency = result.currency,
            metalValuation = result.metalValuation?.let { PortfolioMetalValuationResponse.from(it) },
            collectorValuation = result.collectorValuation?.let { PortfolioCollectorValuationResponse.from(it) }
        )
    }
}

@Schema(description = "Portfolio metal valuation with breakdown by metal type")
data class PortfolioMetalValuationResponse(
    @Schema(description = "Historical data points with metal breakdown")
    val dataPoints: List<PortfolioMetalPointResponse>
) {
    companion object {
        fun from(result: PortfolioMetalValuationResult) = PortfolioMetalValuationResponse(
            dataPoints = result.dataPoints.map { PortfolioMetalPointResponse.from(it) }
        )
    }
}

@Schema(description = "Single portfolio metal valuation data point")
data class PortfolioMetalPointResponse(
    @Schema(description = "Timestamp of the valuation", example = "2023-06-15T10:30:00Z")
    val timestamp: Instant,

    @Schema(description = "Total metal value across all coins", example = "45678.90")
    val totalValue: BigDecimal,

    @Schema(description = "Total pure gold grams in portfolio", example = "62.207")
    val goldGrams: BigDecimal,

    @Schema(description = "Total pure silver grams in portfolio", example = "311.035")
    val silverGrams: BigDecimal,

    @Schema(description = "Total pure platinum grams in portfolio", example = "31.103")
    val platinumGrams: BigDecimal
) {
    companion object {
        fun from(point: PortfolioMetalPoint) = PortfolioMetalPointResponse(
            timestamp = point.timestamp.toInstant(),
            totalValue = point.totalValue,
            goldGrams = point.goldGrams,
            silverGrams = point.silverGrams,
            platinumGrams = point.platinumGrams
        )
    }
}

@Schema(description = "Portfolio collector valuation from issue prices")
data class PortfolioCollectorValuationResponse(
    @Schema(description = "Historical data points")
    val dataPoints: List<PortfolioCollectorPointResponse>
) {
    companion object {
        fun from(result: PortfolioCollectorValuationResult) = PortfolioCollectorValuationResponse(
            dataPoints = result.dataPoints.map { PortfolioCollectorPointResponse.from(it) }
        )
    }
}

@Schema(description = "Single portfolio collector valuation data point")
data class PortfolioCollectorPointResponse(
    @Schema(description = "Timestamp of the valuation", example = "2023-06-15T10:30:00Z")
    val timestamp: Instant,

    @Schema(description = "Minimum total value across all coins", example = "48500.00")
    val minValue: BigDecimal?,

    @Schema(description = "Maximum total value across all coins", example = "56200.00")
    val maxValue: BigDecimal?
) {
    companion object {
        fun from(point: PortfolioCollectorPoint) = PortfolioCollectorPointResponse(
            timestamp = point.timestamp.toInstant(),
            minValue = point.minValue,
            maxValue = point.maxValue
        )
    }
}
