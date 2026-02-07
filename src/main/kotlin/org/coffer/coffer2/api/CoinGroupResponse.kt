package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import java.math.BigDecimal

@Schema(description = "A group of coins sharing the same Numista type ID, or a standalone coin")
data class CoinGroupResponse(
    @Schema(description = "Numista type ID (null for standalone coins)", example = "12345")
    val numistaId: String?,

    @Schema(description = "Whether this represents a group of multiple coins")
    val isGroup: Boolean,

    @Schema(description = "ID of the representative coin (first added)", example = "550e8400-e29b-41d4-a716-446655440000")
    val representativeCoinId: String,

    @Schema(description = "Coin title/name", example = "American Gold Eagle 1 oz")
    val title: String,

    @Schema(description = "ISO 3166-1 alpha-2 country code of issuer", example = "US")
    val issuerCountry: String,

    @Schema(description = "Primary metal composition", example = "GOLD")
    val metalType: MetalType?,

    @Schema(description = "Type of coin", example = "BULLION")
    val type: CoinType,

    @Schema(description = "Total weight in grams", example = "31.1035")
    val weightInGrams: BigDecimal,

    @Schema(description = "Metal purity", example = "999")
    val purity: BigDecimal?,

    @Schema(description = "Physical shape", example = "CIRCULAR")
    val shape: CoinShape,

    @Schema(description = "Number of distinct variants in this group")
    val variantCount: Int,

    @Schema(description = "Total quantity across all variants")
    val totalQuantity: Int,

    @Schema(description = "Earliest minting year in the group", example = "2019")
    val yearMin: Int,

    @Schema(description = "Latest minting year in the group", example = "2024")
    val yearMax: Int,

    @Schema(description = "Variant summaries for this group")
    val variants: List<CoinVariantSummary>,
) {
    companion object {
        fun fromCoins(coins: List<Coin>): CoinGroupResponse {
            require(coins.isNotEmpty()) { "Cannot create CoinGroupResponse from empty list" }

            val representative = coins.minBy { it.createdAt }
            val years = coins.map { it.yearOfMinting.year }

            return CoinGroupResponse(
                numistaId = representative.numistaId,
                isGroup = coins.size > 1,
                representativeCoinId = representative.id.toString(),
                title = representative.title,
                issuerCountry = representative.issuerCountry.country,
                metalType = representative.metalType,
                type = representative.type,
                weightInGrams = representative.weightInGrams,
                purity = representative.purity,
                shape = representative.shape,
                variantCount = coins.size,
                totalQuantity = coins.sumOf { it.quantity },
                yearMin = years.min(),
                yearMax = years.max(),
                variants = coins
                    .sortedByDescending { it.yearOfMinting.year }
                    .map { CoinVariantSummary.from(it) },
            )
        }
    }
}

@Schema(description = "Summary of a single coin variant within a group")
data class CoinVariantSummary(
    @Schema(description = "Coin UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @Schema(description = "Year of minting", example = "2023")
    val yearOfMinting: Int,

    @Schema(description = "Mint mark", example = "W")
    val mintMark: String?,

    @Schema(description = "Coin grade", example = "UNCIRCULATED")
    val grade: CoinGrade?,

    @Schema(description = "Quantity of this variant", example = "1")
    val quantity: Int,
) {
    companion object {
        fun from(coin: Coin): CoinVariantSummary = CoinVariantSummary(
            id = coin.id.toString(),
            yearOfMinting = coin.yearOfMinting.year,
            mintMark = coin.mintMark?.value,
            grade = coin.grade,
            quantity = coin.quantity,
        )
    }
}

@Schema(description = "Grouped coin search response with pagination")
data class GroupedCoinSearchResponse(
    @Schema(description = "Paginated groups")
    val groups: List<CoinGroupResponse>,

    @Schema(description = "Total number of groups across all pages")
    val totalGroups: Long,

    @Schema(description = "Total number of individual coins matching the query")
    val totalCoinCount: Long,

    @Schema(description = "Total quantity across all coins (sum of all coin quantities)")
    val totalQuantityCount: Long,

    @Schema(description = "Current page number (0-based)")
    val page: Int,

    @Schema(description = "Page size")
    val size: Int,

    @Schema(description = "Total number of pages")
    val totalPages: Int,
)
