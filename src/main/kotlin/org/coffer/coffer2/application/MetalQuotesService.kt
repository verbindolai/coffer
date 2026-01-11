package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.MetalType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetalQuotesService(
    private val metalQuotesFetcherService: MetalQuotesFetcherService,
    private val metalQuoteRepository: MetalQuoteRepositoryAdapter
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun updateMetalQuotes(metalTypes: List<MetalType>) {
        metalTypes.forEach { metalType ->
            updateMetalQuote(metalType)
        }
    }

    private fun updateMetalQuote(metalType: MetalType) {
        try {
            val quote = metalQuotesFetcherService.getMetalQuote(metalType)

            if (quote != null) {
                metalQuoteRepository.save(quote)
                logger.info { "Saved quote for $metalType: ${quote.pricePerGram} ${quote.currency}/g" }
            } else {
                logger.warn { "No quote available for $metalType" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to update quote for $metalType" }
        }
    }


}