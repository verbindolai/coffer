package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.remote.swissquote.SwissquoteClient
import org.coffer.coffer2.remote.swissquote.toMetalQuote
import org.springframework.stereotype.Service

@Service
class SwissquoteMetalQuotesServiceImpl(
    private val swissquoteClient: SwissquoteClient
): MetalQuotesService {

    private val logger = KotlinLogging.logger {}

    override fun getMetalQuote(metalType: MetalType): MetalQuote? {
        logger.info { "Getting metal quote for metal type $metalType" }
        return swissquoteClient.getMetalPrice(metalType.toSwissquoteSymbol()).toMetalQuote(metalType)
    }

}