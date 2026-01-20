package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.coffer.coffer2.application.valuation.CoinValuationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/coins/{coinId}/current-prices")
@Tag(name = "Coin Valuation", description = "Get historical coin valuations based on metal prices and issue prices")
class CurrentPricesController(
    private val coinValuationService: CoinValuationService
) {

    @GetMapping
    @Operation(
        summary = "Get current prices",
        description = """
            Returns the current/latest prices for a coin including:
            - Current metal value based on the latest metal quote
            - All available collector prices by grade (not just the coin's grade)

            This endpoint is optimized for displaying current prices without historical data.
            The coin's grade is included in the response to highlight the relevant price.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Current prices returned successfully",
                content = [Content(schema = Schema(implementation = CurrentPricesResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Coin not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getCurrentPrices(
        @Parameter(description = "Coin UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable coinId: UUID
    ): ResponseEntity<CurrentPricesResponse> {
        val result = coinValuationService.getCurrentPrices(coinId)
        return ResponseEntity.ok(CurrentPricesResponse.from(result))
    }
}
