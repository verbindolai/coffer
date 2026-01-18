package org.coffer.coffer2.domain.coin

import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID

data class Coin(
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
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val diameterInMillimeters: BigDecimal?,
    val thicknessInMillimeters: BigDecimal?,
    val quantity: Int = 1,
)

data class YearOfMinting(
    val year: Int,
)

class MintMark(
    val value: String,
)

class Rarity(
    val score: Int,
)
