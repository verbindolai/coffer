package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.application.catalog.CatalogCoinDetailsResult
import org.coffer.coffer2.domain.coin.CoinShape
import java.math.BigDecimal

@Schema(description = "Detailed coin information from Numista catalog for form pre-population")
data class CatalogCoinDetails(
    @Schema(description = "Numista type ID", example = "12345")
    val numistaId: String,

    @Schema(description = "Coin title", example = "1 Dollar - American Silver Eagle")
    val title: String,

    @Schema(description = "Type of coin (BULLION, COMMEMORATIVE_CIRCULATION, etc.)", example = "BULLION")
    val coinType: String,

    @Schema(description = "ISO 3166-1 alpha-2 country code of issuer", example = "US")
    val issuerCode: String?,

    @Schema(description = "Full name of the issuing country/entity", example = "United States")
    val issuerName: String?,

    @Schema(description = "Weight in grams", example = "31.1035")
    val weightInGrams: BigDecimal?,

    @Schema(description = "Diameter in millimeters", example = "40.6")
    val diameterInMillimeters: BigDecimal?,

    @Schema(description = "Thickness in millimeters", example = "2.98")
    val thicknessInMillimeters: BigDecimal?,

    @Schema(description = "Detected metal type (GOLD, SILVER, PLATINUM, NICKEL)", example = "SILVER")
    val metalType: String?,

    @Schema(description = "Metal purity in parts per thousand", example = "999")
    val purity: Int?,

    @Schema(description = "Full composition text from Numista", example = "Silver (.999)")
    val compositionText: String?,

    @Schema(description = "Parsed denomination value", example = "1")
    val denomination: BigDecimal?,

    @Schema(description = "Full denomination text from Numista", example = "1 Dollar")
    val valueText: String?,

    @Schema(description = "Suggested ISO 4217 currency code based on issuer", example = "USD")
    val suggestedCurrency: String?,

    @Schema(description = "Earliest year of issue", example = "1986")
    val minYear: Int?,

    @Schema(description = "Latest year of issue", example = "2023")
    val maxYear: Int?,

    @Schema(description = "URL to full-size obverse image", example = "https://en.numista.com/catalogue/photos/...")
    val obverseImageUrl: String?,

    @Schema(description = "URL to full-size reverse image", example = "https://en.numista.com/catalogue/photos/...")
    val reverseImageUrl: String?,

    @Schema(description = "URL to obverse thumbnail", example = "https://en.numista.com/catalogue/photos/...")
    val obverseThumbnailUrl: String?,

    @Schema(description = "URL to reverse thumbnail", example = "https://en.numista.com/catalogue/photos/...")
    val reverseThumbnailUrl: String?,

    @Schema(description = "Detected coin shape", example = "CIRCULAR")
    val shape: CoinShape,

    @Schema(description = "List of rulers/leaders depicted on the coin", example = "[\"Walking Liberty\"]")
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
