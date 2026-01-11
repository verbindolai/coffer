package org.coffer.coffer2.application

import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalType

interface MetalQuotesService {
    fun getMetalQuote(metalType: MetalType): MetalQuote?
}