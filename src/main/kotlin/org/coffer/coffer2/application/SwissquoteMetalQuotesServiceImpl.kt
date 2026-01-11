package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.remote.swissquote.SwissquoteClient
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency

@Service
class SwissquoteMetalQuotesServiceImpl(
    private val swissquoteClient: SwissquoteClient
): MetalQuotesService {

    private val logger = KotlinLogging.logger {}

    override fun getMetalQuote(metalType: MetalType): MetalQuote? {

        logger.info { "Getting metal quote for metal type $metalType" }

        val response = swissquoteClient.getMetalPrice(metalType.toSwissquoteSymbol())
        val firstPrice = response.firstOrNull()?.spreadProfilePrices?.firstOrNull() ?: return null
        return MetalQuote(
            metalType = metalType,
            pricePerGram = firstPrice.bid,
            currency = Currency.getInstance("EUR"),
            quotedAt = response.firstOrNull()?.ts?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")) } ?: ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )
    }

}