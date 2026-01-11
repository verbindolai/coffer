package org.coffer.coffer2.application

import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalType

interface MetalQuotesFetcherService {
    fun getMetalQuote(metalType: MetalType): MetalQuote?
}