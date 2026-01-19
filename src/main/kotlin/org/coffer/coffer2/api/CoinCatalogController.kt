package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.coffer.coffer2.application.CoinCatalogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Coin Catalog", description = "Search Numista catalog and get pre-populated coin data")
class CoinCatalogController(
    private val coinCatalogService: CoinCatalogService
) {

    @GetMapping("/search")
    @Operation(
        summary = "Search coin catalog",
        description = "Search Numista catalog for coins by title, description, or other attributes. Returns paginated results."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Search results returned successfully",
        content = [Content(schema = Schema(implementation = CatalogSearchResponse::class))]
    )
    fun searchCatalog(
        @Parameter(description = "Search query text", example = "American Eagle")
        @RequestParam query: String,

        @Parameter(description = "Page number (1-based)", example = "1")
        @RequestParam(defaultValue = "1") page: Int,

        @Parameter(description = "Results per page (max 50)", example = "20")
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<CatalogSearchResponse> {
        val result = coinCatalogService.searchCatalog(query, page, pageSize)
        return ResponseEntity.ok(CatalogSearchResponse.from(result))
    }

    @GetMapping("/types/{typeId}")
    @Operation(
        summary = "Get coin details for form population",
        description = "Retrieve detailed coin data from Numista to pre-populate the coin creation form. Returns physical specifications, composition, denomination, and image URLs."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Coin details returned successfully",
                content = [Content(schema = Schema(implementation = CatalogCoinDetails::class))]
            ),
            ApiResponse(responseCode = "404", description = "Coin type not found in Numista catalog")
        ]
    )
    fun getCoinDetails(
        @Parameter(description = "Numista type ID", example = "12345")
        @PathVariable typeId: String
    ): ResponseEntity<CatalogCoinDetails> {
        val result = coinCatalogService.getCoinDetails(typeId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CatalogCoinDetails.from(result))
    }
}
