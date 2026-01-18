package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.coffer.coffer2.application.portfolio.PortfolioValuationService
import org.coffer.coffer2.domain.ValuationTimeframe
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/portfolio/valuation")
@Tag(name = "Portfolio Valuation", description = "Get historical portfolio valuations based on metal prices and collector prices")
class PortfolioValuationController(
    private val portfolioValuationService: PortfolioValuationService
) {

    @GetMapping
    @Operation(
        summary = "Get portfolio valuation",
        description = """
            Returns historical valuation data for the entire portfolio, aggregating all coins.

            Metal valuation: Calculates total value based on pure metal content across all coins,
            broken down by gold, silver, and platinum grams.

            Collector valuation: Returns aggregated collector prices. For coins with exact issue matches,
            returns exact values. For coins with multiple issue matches, returns min/max range.

            For short timeframes (1h, 1d): Computes real-time from current price data.
            For longer timeframes (1w, 1m, 1y, max): Uses pre-computed daily snapshots.

            Timeframes: 1h, 1d, 1w, 1m, 1y, max
        """
    )
    fun getValuation(
        @Parameter(description = "Timeframe: 1h, 1d, 1w, 1m, 1y, max")
        @RequestParam(defaultValue = "1d") timeframe: String
    ): ResponseEntity<PortfolioValuationResponse> {
        val tf = ValuationTimeframe.fromCode(timeframe)
        val result = portfolioValuationService.getValuation(tf)
        return ResponseEntity.ok(PortfolioValuationResponse.from(result))
    }
}
