package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.application.valuation.CoinValuationResult
import org.coffer.coffer2.application.valuation.IssueValuationPoint
import org.coffer.coffer2.application.valuation.IssueValuationResult
import org.coffer.coffer2.application.valuation.MetalValuationPoint
import org.coffer.coffer2.application.valuation.MetalValuationResult
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Coin valuation data including metal and collector values")
data class CoinValuationResponse(
    @Schema(description = "Coin identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val coinId: String,

    @Schema(description = "Requested timeframe", example = "1d", allowableValues = ["1h", "1d", "1w", "1m", "1y", "max"])
    val timeframe: String,

    @Schema(description = "Metal-based valuation (null if coin has no metal type or purity)")
    val metalValuation: MetalValuationResponse?,

    @Schema(description = "Collector/issue-based valuation (null if no linked issues)")
    val issueValuation: IssueValuationResponse?
) {
    companion object {
        fun from(result: CoinValuationResult) = CoinValuationResponse(
            coinId = result.coinId.toString(),
            timeframe = result.timeframe.code,
            metalValuation = result.metalValuation?.let { MetalValuationResponse.from(it) },
            issueValuation = result.issueValuation?.let { IssueValuationResponse.from(it) }
        )
    }
}

@Schema(description = "Metal-based valuation calculated from precious metal content")
data class MetalValuationResponse(
    @Schema(description = "Metal type", example = "GOLD")
    val metalType: String,

    @Schema(description = "Pure metal mass in grams (weight × purity)", example = "31.072")
    val pureMetalMassInGrams: BigDecimal,

    @Schema(description = "Currency code for values", example = "USD")
    val currency: String,

    @Schema(description = "Historical price data points")
    val dataPoints: List<MetalValuationPointResponse>
) {
    companion object {
        fun from(result: MetalValuationResult) = MetalValuationResponse(
            metalType = result.metalType.name,
            pureMetalMassInGrams = result.pureMetalMassInGrams,
            currency = result.currency,
            dataPoints = result.dataPoints.map { MetalValuationPointResponse.from(it) }
        )
    }
}

@Schema(description = "Single metal valuation data point")
data class MetalValuationPointResponse(
    @Schema(description = "Timestamp of the price quote", example = "2023-06-15T10:30:00Z")
    val timestamp: Instant,

    @Schema(description = "Metal price per gram at this timestamp", example = "62.45")
    val pricePerGram: BigDecimal,

    @Schema(description = "Total metal value (pureMetalMass × pricePerGram)", example = "1941.67")
    val totalValue: BigDecimal
) {
    companion object {
        fun from(point: MetalValuationPoint) = MetalValuationPointResponse(
            timestamp = point.timestamp.toInstant(),
            pricePerGram = point.pricePerGram,
            totalValue = point.totalValue
        )
    }
}

@Schema(description = "Collector/issue-based valuation from Numista price data")
data class IssueValuationResponse(
    @Schema(description = "Grade used for valuation (null if aggregating multiple grades)", example = "UNCIRCULATED")
    val grade: String?,

    @Schema(description = "True if exact issue match found, false if aggregating multiple issues")
    val isExactMatch: Boolean,

    @Schema(description = "Currency code for values", example = "USD")
    val currency: String,

    @Schema(description = "Historical price data points")
    val dataPoints: List<IssueValuationPointResponse>
) {
    companion object {
        fun from(result: IssueValuationResult) = IssueValuationResponse(
            grade = result.grade?.name,
            isExactMatch = result.isExactMatch,
            currency = result.currency,
            dataPoints = result.dataPoints.map { IssueValuationPointResponse.from(it) }
        )
    }
}

@Schema(description = "Single issue valuation data point")
data class IssueValuationPointResponse(
    @Schema(description = "Timestamp of the price data", example = "2023-06-15T10:30:00Z")
    val timestamp: Instant,

    @Schema(description = "Exact price if single issue match (null if range)", example = "2150.00")
    val price: BigDecimal?,

    @Schema(description = "Minimum price across matched issues (null if exact match)", example = "1950.00")
    val minPrice: BigDecimal?,

    @Schema(description = "Maximum price across matched issues (null if exact match)", example = "2350.00")
    val maxPrice: BigDecimal?
) {
    companion object {
        fun from(point: IssueValuationPoint) = IssueValuationPointResponse(
            timestamp = point.timestamp.toInstant(),
            price = point.price,
            minPrice = point.minPrice,
            maxPrice = point.maxPrice
        )
    }
}
