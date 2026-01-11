package org.coffer.coffer2.repository

import jakarta.persistence.*
import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "metal_quotes")
data class MetalQuoteEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val metalType: MetalType,

    @Column(nullable = false, precision = 19, scale = 4)
    val pricePerGram: BigDecimal,

    @Column(nullable = false, length = 3)
    val currencyCode: String,

    @Column(nullable = false)
    val quotedAt: ZonedDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    val source: MetalQuoteSource,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now()
) {
    fun toMetalQuote(): MetalQuote = MetalQuote(
        id = id.toString(),
        metalType = metalType,
        pricePerGram = pricePerGram,
        currency = Currency.getInstance(currencyCode),
        quotedAt = quotedAt,
        source = source,
        createdAt = createdAt
    )

    companion object {
        fun fromMetalQuote(quote: MetalQuote): MetalQuoteEntity = MetalQuoteEntity(
            id = quote.id?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            metalType = quote.metalType,
            pricePerGram = quote.pricePerGram,
            currencyCode = quote.currency.currencyCode,
            quotedAt = quote.quotedAt,
            source = quote.source,
            createdAt = quote.createdAt
        )
    }
}