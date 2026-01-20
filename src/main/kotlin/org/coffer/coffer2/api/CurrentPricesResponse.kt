package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.domain.CollectorPricesResult
import org.coffer.coffer2.domain.CurrentPricesResult
import org.coffer.coffer2.domain.GradePriceResult
import org.coffer.coffer2.domain.MetalValueResult
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Current prices for a coin including metal value and all grade prices")
data class CurrentPricesResponse(
    @Schema(description = "Coin identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val coinId: String,

    @Schema(description = "Currency code for all values", example = "USD")
    val currency: String,

    @Schema(description = "Current metal value (null if coin has no metal type or purity)")
    val metalValue: MetalValueResponse?,

    @Schema(description = "Collector prices by grade (null if no linked issues)")
    val collectorPrices: CollectorPricesResponse?
) {
    companion object {
        fun from(result: CurrentPricesResult) = CurrentPricesResponse(
            coinId = result.coinId.toString(),
            currency = result.currency,
            metalValue = result.metalValue?.let { MetalValueResponse.from(it) },
            collectorPrices = result.collectorPrices?.let { CollectorPricesResponse.from(it) }
        )
    }
}

@Schema(description = "Current metal value")
data class MetalValueResponse(
    @Schema(description = "Metal type", example = "GOLD")
    val metalType: String,

    @Schema(description = "Pure metal mass in grams", example = "31.072")
    val pureMetalMassInGrams: BigDecimal,

    @Schema(description = "Current price per gram", example = "62.45")
    val pricePerGram: BigDecimal,

    @Schema(description = "Total metal value", example = "1941.67")
    val totalValue: BigDecimal,

    @Schema(description = "Timestamp of the price quote", example = "2023-06-15T10:30:00Z")
    val timestamp: Instant
) {
    companion object {
        fun from(result: MetalValueResult) = MetalValueResponse(
            metalType = result.metalType.name,
            pureMetalMassInGrams = result.pureMetalMassInGrams,
            pricePerGram = result.pricePerGram,
            totalValue = result.totalValue,
            timestamp = result.timestamp.toInstant()
        )
    }
}

@Schema(description = "Collector prices for all available grades")
data class CollectorPricesResponse(
    @Schema(description = "The coin's grade (used to highlight the relevant price)", example = "UNCIRCULATED")
    val coinGrade: String?,

    @Schema(description = "True if the coin's grade has an exact price match")
    val hasExactMatch: Boolean,

    @Schema(description = "Prices for each available grade")
    val gradePrices: List<GradePriceResponse>,

    @Schema(description = "Timestamp of the latest price data", example = "2023-06-15T10:30:00Z")
    val timestamp: Instant?
) {
    companion object {
        fun from(result: CollectorPricesResult) = CollectorPricesResponse(
            coinGrade = result.coinGrade?.name,
            hasExactMatch = result.hasExactMatch,
            gradePrices = result.gradePrices.map { GradePriceResponse.from(it) },
            timestamp = result.timestamp?.toInstant()
        )
    }
}

@Schema(description = "Price range for a specific grade")
data class GradePriceResponse(
    @Schema(description = "Grade", example = "UNCIRCULATED")
    val grade: String,

    @Schema(description = "Minimum price for this grade (may equal maxPrice if single issue)", example = "102.71")
    val minPrice: BigDecimal,

    @Schema(description = "Maximum price for this grade (may equal minPrice if single issue)", example = "185.69")
    val maxPrice: BigDecimal
) {
    companion object {
        fun from(result: GradePriceResult) = GradePriceResponse(
            grade = result.grade.name,
            minPrice = result.minPrice,
            maxPrice = result.maxPrice
        )
    }
}
