package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.coffer.coffer2.application.CoinService
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/coins")
@Tag(name = "Coins", description = "Coin collection management endpoints")
class CoinController(
    private val coinService: CoinService
) {

    @PostMapping
    @Operation(summary = "Create a new coin", description = "Add a new coin to the collection")
    fun createCoin(@Valid @RequestBody request: CreateCoinRequest): ResponseEntity<CoinResponse> {
        val command = request.toCommand()
        val coin = coinService.createCoin(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(CoinResponse.from(coin))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coin by ID", description = "Retrieve a specific coin by its ID")
    fun getCoinById(@PathVariable id: UUID): ResponseEntity<CoinResponse> {
        val coin = coinService.getCoinById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CoinResponse.from(coin))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coin", description = "Update an existing coin's details")
    fun updateCoin(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCoinRequest
    ): ResponseEntity<CoinResponse> {
        val command = request.toCommand(id)
        val coin = coinService.updateCoin(command)
        return ResponseEntity.ok(CoinResponse.from(coin))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete coin", description = "Delete a coin from the collection")
    fun deleteCoin(@PathVariable id: UUID): ResponseEntity<Void> {
        coinService.deleteCoin(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    @Operation(summary = "Search coins", description = "Search and filter coins with pagination")
    fun searchCoins(
        @Parameter(description = "Filter by issuer country code (ISO 3166-1 alpha-2)")
        @RequestParam(required = false) country: String?,

        @Parameter(description = "Filter by denomination value")
        @RequestParam(required = false) denomination: String?,

        @Parameter(description = "Filter by coin grade")
        @RequestParam(required = false) grade: CoinGrade?,

        @Parameter(description = "Filter by coin type")
        @RequestParam(required = false) coinType: CoinType?,

        @Parameter(description = "Filter by minimum year of minting")
        @RequestParam(required = false) yearFrom: Int?,

        @Parameter(description = "Filter by maximum year of minting")
        @RequestParam(required = false) yearTo: Int?,

        @Parameter(description = "Filter by metal type")
        @RequestParam(required = false) metalType: MetalType?,

        @Parameter(description = "Filter by coin shape")
        @RequestParam(required = false) shape: CoinShape?,

        @Parameter(description = "Filter by currency code (ISO 4217)")
        @RequestParam(required = false) currency: String?,

        @Parameter(description = "Search by title (partial match)")
        @RequestParam(required = false) title: String?,

        @Parameter(description = "Filter by Numista ID")
        @RequestParam(required = false) numistaId: String?,

        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<Page<CoinResponse>> {
        val query = CoinSearchQuery(
            country = country,
            denomination = denomination,
            grade = grade,
            coinType = coinType,
            yearFrom = yearFrom,
            yearTo = yearTo,
            metalType = metalType,
            shape = shape,
            currency = currency,
            title = title,
            numistaId = numistaId
        )

        val coins = coinService.searchCoins(query, pageable)
        return ResponseEntity.ok(coins.map { CoinResponse.from(it) })
    }

    @GetMapping("/{coinId}/images")
    @Operation(summary = "Get coin images", description = "Retrieve all images for a specific coin")
    fun getCoinImages(@PathVariable coinId: UUID): ResponseEntity<List<CoinImageResponse>> {
        val images = coinService.getImagesForCoin(coinId)
        return ResponseEntity.ok(images.map { CoinImageResponse.from(it) })
    }

    @GetMapping("/{coinId}/images/{imageId}")
    @Operation(summary = "Get coin image metadata", description = "Retrieve metadata for a specific coin image")
    fun getCoinImage(
        @PathVariable coinId: UUID,
        @PathVariable imageId: UUID
    ): ResponseEntity<CoinImageResponse> {
        val image = coinService.getCoinImage(coinId, imageId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CoinImageResponse.from(image))
    }

    @GetMapping("/{coinId}/images/{imageId}/content")
    @Operation(summary = "Get coin image content", description = "Download the actual image file for a specific coin image")
    fun getCoinImageContent(
        @PathVariable coinId: UUID,
        @PathVariable imageId: UUID
    ): ResponseEntity<org.springframework.core.io.Resource> {
        val (image, path) = coinService.getImageContent(coinId, imageId)
            ?: return ResponseEntity.notFound().build()

        val resource = org.springframework.core.io.UrlResource(path.toUri())

        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.parseMediaType(image.contentType))
            .header(
                org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"${image.fileName}\""
            )
            .body(resource)
    }

}