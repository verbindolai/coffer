package org.coffer.coffer2.schedule

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.MetalQuotesService
import org.coffer.coffer2.config.MetalQuotesProperties
import org.coffer.coffer2.domain.MetalType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MetalQuotesFetcherScheduler(
    private val metalQuotesProperties: MetalQuotesProperties,
    private val metalQuotesService: MetalQuotesService
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "\${coffer.metal-quotes.update-interval-cron}")
    fun updateMetalPricesAndValuations() {
        logger.info { "Updating metal prices for tracked metals: ${metalQuotesProperties.trackedMetalCodes}" }

        val metalTypes = metalQuotesProperties.trackedMetalCodes.map { code ->
            MetalType.fromSymbol(code)
        }

        metalQuotesService.updateMetalQuotes(metalTypes)

        logger.info { "Metal prices update completed" }
    }
}
