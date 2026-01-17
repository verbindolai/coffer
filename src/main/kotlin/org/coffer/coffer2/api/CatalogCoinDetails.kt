package org.coffer.coffer2.api

import org.coffer.coffer2.application.catalog.CatalogCoinDetailsResult
import java.math.BigDecimal

data class CatalogCoinDetails(
    val numistaId: String,

    val title: String,
    val coinType: String,
    val issuerCode: String?,
    val issuerName: String?,

    val weightInGrams: BigDecimal?,
    val diameterInMillimeters: BigDecimal?,
    val thicknessInMillimeters: BigDecimal?,

    val metalType: String?,
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

    val shape: String?,
    val rulers: List<String>
) {
    companion object {
        fun from(result: CatalogCoinDetailsResult) = CatalogCoinDetails(
            numistaId = result.numistaId,
            title = result.title,
            coinType = result.coinType.name,
            issuerCode = result.issuerCode,
            issuerName = result.issuerName,
            weightInGrams = result.weightInGrams,
            diameterInMillimeters = result.diameterInMillimeters,
            thicknessInMillimeters = result.thicknessInMillimeters,
            metalType = result.metalType?.name,
            purity = result.purity,
            compositionText = result.compositionText,
            denomination = result.denomination,
            valueText = result.valueText,
            suggestedCurrency = result.suggestedCurrency,
            minYear = result.minYear,
            maxYear = result.maxYear,
            obverseImageUrl = result.obverseImageUrl,
            reverseImageUrl = result.reverseImageUrl,
            obverseThumbnailUrl = result.obverseThumbnailUrl,
            reverseThumbnailUrl = result.reverseThumbnailUrl,
            shape = result.shape,
            rulers = result.rulers
        )
    }
}
