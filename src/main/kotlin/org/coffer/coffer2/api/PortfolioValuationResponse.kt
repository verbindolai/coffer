package org.coffer.coffer2.api

import org.coffer.coffer2.application.portfolio.PortfolioCollectorPoint
import org.coffer.coffer2.application.portfolio.PortfolioCollectorValuationResult
import org.coffer.coffer2.application.portfolio.PortfolioMetalPoint
import org.coffer.coffer2.application.portfolio.PortfolioMetalValuationResult
import org.coffer.coffer2.application.portfolio.PortfolioValuationResult
import java.math.BigDecimal
import java.time.Instant

data class PortfolioValuationResponse(
    val timeframe: String,
    val currency: String,
    val metalValuation: PortfolioMetalValuationResponse?,
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

data class PortfolioMetalValuationResponse(
    val dataPoints: List<PortfolioMetalPointResponse>
) {
    companion object {
        fun from(result: PortfolioMetalValuationResult) = PortfolioMetalValuationResponse(
            dataPoints = result.dataPoints.map { PortfolioMetalPointResponse.from(it) }
        )
    }
}

data class PortfolioMetalPointResponse(
    val timestamp: Instant,
    val totalValue: BigDecimal,
    val goldGrams: BigDecimal,
    val silverGrams: BigDecimal,
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

data class PortfolioCollectorValuationResponse(
    val dataPoints: List<PortfolioCollectorPointResponse>
) {
    companion object {
        fun from(result: PortfolioCollectorValuationResult) = PortfolioCollectorValuationResponse(
            dataPoints = result.dataPoints.map { PortfolioCollectorPointResponse.from(it) }
        )
    }
}

data class PortfolioCollectorPointResponse(
    val timestamp: Instant,
    val exactValue: BigDecimal?,
    val minValue: BigDecimal?,
    val maxValue: BigDecimal?
) {
    companion object {
        fun from(point: PortfolioCollectorPoint) = PortfolioCollectorPointResponse(
            timestamp = point.timestamp.toInstant(),
            exactValue = point.exactValue,
            minValue = point.minValue,
            maxValue = point.maxValue
        )
    }
}
