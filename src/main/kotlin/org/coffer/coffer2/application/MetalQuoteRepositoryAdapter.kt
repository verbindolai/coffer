package org.coffer.coffer2.application

import org.coffer.coffer2.domain.MetalQuote

interface MetalQuoteRepositoryAdapter {
    fun save(quote: MetalQuote): MetalQuote
}