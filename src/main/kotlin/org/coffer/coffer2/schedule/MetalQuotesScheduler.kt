package org.coffer.coffer2.schedule

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MetalQuotesFetcherScheduler(

) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "\${coffer.metal-quotes.update-interval-cron}")
    fun updateMetalPricesAndValuations() {
        logger.info { "Updating metal prices" }
    }
}
