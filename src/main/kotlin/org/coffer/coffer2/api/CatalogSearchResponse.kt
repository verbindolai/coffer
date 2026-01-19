package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.application.catalog.CatalogSearchResultItem
import org.coffer.coffer2.application.catalog.CatalogSearchResult

@Schema(description = "Catalog search results")
data class CatalogSearchResponse(
    @Schema(description = "Total number of matching results in the catalog", example = "1523")
    val totalCount: Int,

    @Schema(description = "List of matching coin types for the current page")
    val results: List<CatalogSearchResultItemResponse>
) {
    companion object {
        fun from(result: CatalogSearchResult) = CatalogSearchResponse(
            totalCount = result.totalCount,
            results = result.items.map { CatalogSearchResultItemResponse.from(it) }
        )
    }
}

@Schema(description = "Catalog search result item")
data class CatalogSearchResultItemResponse(
    @Schema(description = "Numista type ID", example = "12345")
    val typeId: String,

    @Schema(description = "Coin title", example = "1 Dollar - American Silver Eagle")
    val title: String,

    @Schema(description = "Coin category", example = "Bullion coins")
    val category: String?,

    @Schema(description = "ISO 3166-1 alpha-2 country code of issuer", example = "US")
    val issuerCode: String?,

    @Schema(description = "Full name of the issuing country/entity", example = "United States")
    val issuerName: String?,

    @Schema(description = "Earliest year of issue", example = "1986")
    val minYear: Int?,

    @Schema(description = "Latest year of issue (null if still being minted)", example = "2023")
    val maxYear: Int?,

    @Schema(description = "URL to obverse side thumbnail image", example = "https://en.numista.com/catalogue/photos/...")
    val obverseThumbnailUrl: String?,

    @Schema(description = "URL to reverse side thumbnail image", example = "https://en.numista.com/catalogue/photos/...")
    val reverseThumbnailUrl: String?
) {
    companion object {
        fun from(item: CatalogSearchResultItem) = CatalogSearchResultItemResponse(
            typeId = item.typeId,
            title = item.title,
            category = item.category,
            issuerCode = item.issuerCode,
            issuerName = item.issuerName,
            minYear = item.minYear,
            maxYear = item.maxYear,
            obverseThumbnailUrl = item.obverseThumbnailUrl,
            reverseThumbnailUrl = item.reverseThumbnailUrl
        )
    }
}
