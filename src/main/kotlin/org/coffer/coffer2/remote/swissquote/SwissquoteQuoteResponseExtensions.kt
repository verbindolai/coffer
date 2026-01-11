package org.coffer.coffer2.remote.swissquote

import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.util.MetalUtil.troyOunceToGram
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency

/**
 * Converts Swissquote API response to domain MetalQuote.
 * Returns null if response is empty or missing required data.
 */
fun List<SwissquoteQuoteResponse>.toMetalQuote(metalType: MetalType): MetalQuote? {
    val firstPrice = firstOrNull()?.spreadProfilePrices?.firstOrNull() ?: return null
    val timestamp = firstOrNull()?.ts

    return MetalQuote(
        metalType = metalType,
        pricePerGram = troyOunceToGram(firstPrice.midPrice()),
        currency = Currency.getInstance("EUR"),
        quotedAt = timestamp?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")) }
            ?: ZonedDateTime.now(),
        source = MetalQuoteSource.SWISSQUOTE
    )
}