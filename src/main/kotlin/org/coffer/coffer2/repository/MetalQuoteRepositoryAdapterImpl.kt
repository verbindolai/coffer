package org.coffer.coffer2.repository

import org.coffer.coffer2.application.MetalQuoteRepositoryAdapter
import org.coffer.coffer2.domain.MetalQuote
import org.springframework.stereotype.Component

@Component
class MetalQuoteRepositoryAdapterImpl(
    private val metalQuoteRepository: MetalQuoteRepository
): MetalQuoteRepositoryAdapter {
    override fun save(quote: MetalQuote): MetalQuote {
        return metalQuoteRepository.save(MetalQuoteEntity.fromMetalQuote(quote)).toMetalQuote()
    }
}