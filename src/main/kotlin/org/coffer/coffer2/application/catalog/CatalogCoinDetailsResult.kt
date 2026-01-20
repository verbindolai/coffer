package org.coffer.coffer2.application.catalog

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import java.math.BigDecimal

data class CatalogCoinDetailsResult(
    val numistaId: String,
    val title: String,
    val coinType: CoinType,
    val issuerCode: String?,
    val issuerName: String?,

    val weightInGrams: BigDecimal?,
    val diameterInMillimeters: BigDecimal?,
    val thicknessInMillimeters: BigDecimal?,

    val metalType: MetalType?,
    val purity: Int?,
    val compositionText: String?,

    val denomination: BigDecimal?,
    val valueText: String?,
    val suggestedCurrency: String?,

    val minYear: Int?,
    val maxYear: Int?,

    val obverseImageUrl: String?,
    val reverseImageUrl: String?,
    val obverseThumbnailUrl: String?,
    val reverseThumbnailUrl: String?,

    val shape: CoinShape,
    val rulers: List<String>
)
