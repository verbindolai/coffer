package org.coffer.coffer2.api

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

data class CreateCoinRequest(

    @field:NotBlank(message = "Title is required")
    val title: String,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Denomination numeric value must be positive")
    val denomination: BigDecimal?,

    @field:Min(value = 1, message = "Year must be at least 1")
    @field:Max(value = 2100, message = "Year must be at most 2100")
    val year: Int,

    @field:NotBlank(message = "Country code is required")
    @field:Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
    val countryCode: String,

    val currency: String,

    val mintMark: String?,

    @field:NotNull(message = "Grade is required")
    val grade: CoinGrade,

    @field:NotNull(message = "Coin type is required")
    val coinType: CoinType,

    val notes: String?,

    @field:Size(max = 50, message = "Numista ID must be at most 50 characters")
    val numistaId: String?,

    val metalType: MetalType?,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Weight must be positive")
    val weightInGrams: BigDecimal,

    @field:Min(value = 1, message = "Purity must be at least 1")
    @field:Max(value = 1000, message = "Purity must be at most 1000")
    val purity: Int?,

    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1,

    @field:Min(value = 1, message = "Rarity score must be at least 1")
    @field:Max(value = 100, message = "Rarity score must be at most 100")
    val rarityScore: Int? = null,

    val diameterInMillimeters: BigDecimal?,

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