package org.coffer.coffer2.domain.coin

import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Currency

data class Coin(
    val id: String,
    val title: String,
    val denomination: String,
    val currency: CoinCurrency,
    val yearOfMinting: YearOfMinting,
    val issuerCountry: Country,
    val mintMark: MintMark?,
    val grade: CoinGrade?,
    val type: CoinType,
    val notes: String?,
    val numistaId: String?,
    val shape: CoinShape,
    val weightInGrams: BigDecimal,
    val purity: BigDecimal,
    val metalType: MetalType,
    val rarity: Rarity,
    val createdAt: ZonedDateTime,
    val diameterInMillimeters: BigDecimal?,
    val thicknessInMillimeters: BigDecimal?,
    val lastPriceUpdate: ZonedDateTime?,
)

data class YearOfMinting(
    val year: Int,
)

data class Country(
    val name: String,
    val code: String,
)

class CoinCurrency(
    val code: Currency,
)

class MintMark(
    val letter: String,
)

class Rarity(
    val score: Int,
)
