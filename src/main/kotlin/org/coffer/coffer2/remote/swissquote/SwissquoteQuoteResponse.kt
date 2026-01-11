package org.coffer.coffer2.remote.swissquote

import java.math.BigDecimal

data class SwissquoteQuoteResponse(
    val spreadProfilePrices: List<SpreadProfilePrice>,
    val ts: Long,
)

// Prices are per ounce
data class SpreadProfilePrice(
    val spreadProfile: String,
    val bid: BigDecimal,
    val ask: BigDecimal,
    val bidSpread: BigDecimal?,
    val askSpread: BigDecimal?,
) {
    /**
     * Calculate mid-price (average of bid and ask)
     */
    fun midPrice(): BigDecimal = bid.add(ask).divide(BigDecimal.valueOf(2))
}
