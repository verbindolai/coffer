package org.coffer.coffer2.domain.coin

import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.UUID

data class UpdateCoinCommand(
    val id: UUID,
    val title: String,
    val denomination: BigDecimal?,
    val currency: Currency,
    val yearOfMinting: YearOfMinting,
    val issuerCountry: Locale,
    val mintMark: MintMark?,
    val grade: CoinGrade?,
    val type: CoinType,
    val notes: String?,
    val numistaId: String?,
    val shape: CoinShape = CoinShape.UNKNOWN,
    val weightInGrams: BigDecimal,
    val purity: BigDecimal? = null,
    val metalType: MetalType? = null,
    val rarity: Rarity? = null,
    val diameterInMillimeters: BigDecimal?,
    val thicknessInMillimeters: BigDecimal?,
) {
    fun toCoin(existingCoin: Coin) = existingCoin.copy(
        title = title,
        denomination = denomination,
        currency = currency,
        yearOfMinting = yearOfMinting,
        issuerCountry = issuerCountry,
        mintMark = mintMark,
        grade = grade,
        type = type,
        notes = notes,
        numistaId = numistaId,
        shape = shape,
        weightInGrams = weightInGrams,
        purity = purity,
        metalType = metalType,
        rarity = rarity,
        diameterInMillimeters = diameterInMillimeters,
        thicknessInMillimeters = thicknessInMillimeters,
    )
}
