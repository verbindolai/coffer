package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.coffer.coffer2.application.valuation.CoinValuationService
import org.coffer.coffer2.domain.ValuationTimeframe
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/coins/{coinId}/valuation")
@Tag(name = "Coin Valuation", description = "Get historical coin valuations based on metal prices and issue prices")
class CoinValuationController(
    private val coinValuationService: CoinValuationService
) {

    @GetMapping
    @Operation(
        summary = "Get coin valuation",
        description = """
            Returns historical valuation data for a coin based on metal prices and issue prices.

            Metal valuation: Calculates value based on pure metal content (weight × purity × metal price).
            Issue valuation: Returns collector prices from linked issues. If exact match exists, returns exact prices.
            If multiple issues match, returns min/max range.

            Timeframes: 1h, 1d, 1w, 1m, 1y, max
        """
    )
    fun getValuation(
        @Parameter(description = "Coin ID")
        @PathVariable coinId: UUID,

        @Parameter(description = "Timeframe: 1h, 1d, 1w, 1m, 1y, max")
        @RequestParam(defaultValue = "1d") timeframe: String
    ): ResponseEntity<CoinValuationResponse> {
        val tf = ValuationTimeframe.fromCode(timeframe)
        val result = coinValuationService.getValuation(coinId, tf)
        return ResponseEntity.ok(CoinValuationResponse.from(result))
    }
}
