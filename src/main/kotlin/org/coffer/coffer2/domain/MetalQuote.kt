package org.coffer.coffer2.domain

import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

data class MetalQuote(
    val id: String? = null,
    val metalType: MetalType,
    val pricePerGram: BigDecimal,
    val currency: Currency,
    val quotedAt: ZonedDateTime,
    val source: MetalQuoteSource,
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)