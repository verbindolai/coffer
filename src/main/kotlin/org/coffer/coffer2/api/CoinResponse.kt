package org.coffer.coffer2.api

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.Rarity
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CoinResponse(
    val id: String,
    val title: String,
    val denomination: BigDecimal?,
    val currency: String,
    val yearOfMinting: Int,
    val issuerCountry: String,
    val mintMark: String?,
    val grade: CoinGrade?,
    val type: CoinType,
    val notes: String?,
    val numistaId: String?,
    val shape: CoinShape,
    val weightInGrams: BigDecimal,
    val purity: BigDecimal? = null,
    val metalType: MetalType? = null,
    val rarity: Rarity? = null,
    val createdAt: ZonedDateTime,
    val diameterInMillimeters: BigDecimal?,
    val thicknessInMillimeters: BigDecimal?,
    val lastPriceUpdate: ZonedDateTime? = null,
) {
    companion object {
        fun from(coin: Coin): CoinResponse =
            with(coin) {
                CoinResponse(
                    id = id!!,
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
                    lastPriceUpdate = lastPriceUpdate,
                )
            }
    }
}