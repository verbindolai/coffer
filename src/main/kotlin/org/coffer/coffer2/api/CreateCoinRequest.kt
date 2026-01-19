package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.CreateCoinCommand
import org.coffer.coffer2.domain.coin.MintMark
import org.coffer.coffer2.domain.coin.Rarity
import org.coffer.coffer2.domain.coin.YearOfMinting
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Schema(description = "Request payload for creating a new coin")
data class CreateCoinRequest(

    @field:NotBlank(message = "Title is required")
    @Schema(description = "Coin title/name", example = "American Gold Eagle 1 oz", required = true)
    val title: String,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Denomination numeric value must be positive")
    @Schema(description = "Face value denomination", example = "50")
    val denomination: BigDecimal?,

    @field:Min(value = 1, message = "Year must be at least 1")
    @field:Max(value = 2100, message = "Year must be at most 2100")
    @Schema(description = "Year of minting", example = "2023", minimum = "1", maximum = "2100", required = true)
    val year: Int,

    @field:NotBlank(message = "Country code is required")
    @field:Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
    @Schema(description = "ISO 3166-1 alpha-2 country code of issuer", example = "US", minLength = 2, maxLength = 2, required = true)
    val countryCode: String,

    @Schema(description = "ISO 4217 currency code", example = "USD", required = true)
    val currency: String,

    @Schema(description = "Mint mark identifier", example = "W")
    val mintMark: String?,

    @field:NotNull(message = "Grade is required")
    @Schema(description = "Coin grade/condition", example = "UNCIRCULATED", required = true)
    val grade: CoinGrade,

    @field:NotNull(message = "Coin type is required")
    @Schema(description = "Type of coin", example = "BULLION", required = true)
    val coinType: CoinType,

    @Schema(description = "Additional notes about the coin", example = "First year of issue")
    val notes: String?,

    @field:Size(max = 50, message = "Numista ID must be at most 50 characters")
    @Schema(description = "Numista catalog type ID for automatic data fetching", example = "12345", maxLength = 50)
    val numistaId: String?,

    @Schema(description = "Primary metal composition", example = "GOLD")
    val metalType: MetalType?,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Weight must be positive")
    @Schema(description = "Total weight in grams", example = "31.1035", required = true)
    val weightInGrams: BigDecimal,

    @field:Min(value = 1, message = "Purity must be at least 1")
    @field:Max(value = 1000, message = "Purity must be at most 1000")
    @Schema(description = "Metal purity in parts per thousand (e.g., 999 for 99.9%)", example = "999", minimum = "1", maximum = "1000")
    val purity: Int?,

    @field:Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Number of identical coins", example = "1", defaultValue = "1", minimum = "1")
    val quantity: Int = 1,

    @field:Min(value = 1, message = "Rarity score must be at least 1")
    @field:Max(value = 100, message = "Rarity score must be at most 100")
    @Schema(description = "Rarity score from 1 (common) to 100 (extremely rare)", example = "25", minimum = "1", maximum = "100")
    val rarityScore: Int? = null,

    @Schema(description = "Coin diameter in millimeters", example = "32.7")
    val diameterInMillimeters: BigDecimal?,

    @Schema(description = "Coin thickness in millimeters", example = "2.87")
    val thicknessInMillimeters: BigDecimal?

) {
    fun toCommand() = CreateCoinCommand(
        title = title,
        denomination = denomination,
        yearOfMinting = YearOfMinting(year),
        issuerCountry = Locale.of("", countryCode),
        mintMark = mintMark?.let { MintMark(it) },
        grade = grade,
        type = coinType,
        notes = notes,
        numistaId = numistaId,
        currency = Currency.getInstance(currency),
        weightInGrams = weightInGrams,
        purity = purity?.let { BigDecimal(it) },
        metalType = metalType,
        diameterInMillimeters = diameterInMillimeters,
        thicknessInMillimeters = thicknessInMillimeters,
        rarity = rarityScore?.let { Rarity(it) },
        quantity = quantity
    )
}