package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.Rarity
import java.math.BigDecimal
import java.time.ZonedDateTime

@Schema(description = "Coin details response")
data class CoinResponse(
    @Schema(description = "Unique coin identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @Schema(description = "Coin title/name", example = "American Gold Eagle 1 oz")
    val title: String,

    @Schema(description = "Face value denomination", example = "50")
    val denomination: BigDecimal?,

    @Schema(description = "ISO 4217 currency code", example = "USD")
    val currency: String,

    @Schema(description = "Year of minting", example = "2023")
    val yearOfMinting: Int,

    @Schema(description = "ISO 3166-1 alpha-2 country code of issuer", example = "US")
    val issuerCountry: String,

    @Schema(description = "Mint mark identifier", example = "W")
    val mintMark: String?,

    @Schema(description = "Coin grade/condition", example = "UNCIRCULATED")
    val grade: CoinGrade?,

    @Schema(description = "Type of coin", example = "BULLION")
    val type: CoinType,

    @Schema(description = "Additional notes about the coin", example = "First year of issue")
    val notes: String?,

    @Schema(description = "Numista catalog type ID", example = "12345")
    val numistaId: String?,

    @Schema(description = "Physical shape of the coin", example = "CIRCULAR")
    val shape: CoinShape,

    @Schema(description = "Total weight in grams", example = "31.1035")
    val weightInGrams: BigDecimal,

    @Schema(description = "Metal purity as decimal (e.g., 0.999 for 99.9%)", example = "999")
    val purity: BigDecimal? = null,

    @Schema(description = "Primary metal composition", example = "GOLD")
    val metalType: MetalType? = null,

    @Schema(description = "Rarity information")
    val rarity: Rarity? = null,

    @Schema(description = "Timestamp when the coin was added to the collection", example = "2023-06-15T10:30:00Z")
    val createdAt: ZonedDateTime,

    @Schema(description = "Coin diameter in millimeters", example = "32.7")
    val diameterInMillimeters: BigDecimal?,

    @Schema(description = "Coin thickness in millimeters", example = "2.87")
    val thicknessInMillimeters: BigDecimal?,

    @Schema(description = "Number of identical coins", example = "1")
    val quantity: Int,
) {
    companion object {
        fun from(coin: Coin): CoinResponse =
            with(coin) {
                CoinResponse(
                    id = id.toString(),
                    title = title,
                    denomination = denomination,
                    currency = currency.currencyCode,
                    yearOfMinting = yearOfMinting.year,
                    issuerCountry = issuerCountry.country,
                    mintMark = mintMark?.value,
                    grade = grade,
                    type = type,
                    notes = notes,
                    numistaId = numistaId,
                    shape = shape,
                    weightInGrams = weightInGrams,
                    purity = purity,
                    metalType = metalType,
                    rarity = rarity,
                    createdAt = createdAt,
                    diameterInMillimeters = diameterInMillimeters,
                    thicknessInMillimeters = thicknessInMillimeters,
                    quantity = quantity
                )
            }
    }
}