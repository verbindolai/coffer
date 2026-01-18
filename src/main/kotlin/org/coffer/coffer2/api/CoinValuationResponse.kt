package org.coffer.coffer2.api

import org.coffer.coffer2.application.valuation.CoinValuationResult
import org.coffer.coffer2.application.valuation.IssueValuationPoint
import org.coffer.coffer2.application.valuation.IssueValuationResult
import org.coffer.coffer2.application.valuation.MetalValuationPoint
import org.coffer.coffer2.application.valuation.MetalValuationResult
import java.math.BigDecimal
import java.time.Instant

data class CoinValuationResponse(
    val coinId: String,
    val timeframe: String,
    val metalValuation: MetalValuationResponse?,
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

data class MetalValuationResponse(
    val metalType: String,
    val pureMetalMassInGrams: BigDecimal,
    val currency: String,
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

data class MetalValuationPointResponse(
    val timestamp: Instant,
    val pricePerGram: BigDecimal,
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

data class IssueValuationResponse(
    val grade: String?,
    val isExactMatch: Boolean,
    val currency: String,
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

data class IssueValuationPointResponse(
    val timestamp: Instant,
    val price: BigDecimal?,
    val minPrice: BigDecimal?,
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
