package org.coffer.coffer2.schedule

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.issueprice.IssuePriceService
import org.coffer.coffer2.config.IssuePriceProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduler for periodic issue price updates from Numista.
 * Updates all coins with Numista IDs on a configurable schedule.
 */
@Component
class IssuePriceScheduler(
    private val issuePriceService: IssuePriceService,
    private val issuePriceProperties: IssuePriceProperties
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Updates all coin prices on the configured schedule.
     * Default: daily at 2 AM, configurable via coffer.issue-prices.update-interval-cron
     */
    @Scheduled(cron = "\${coffer.issue-prices.update-interval-cron:0 0 2 * * *}")
    fun updateAllCoins() {
        logger.info { "Starting issue price update for all coins" }
        logger.info { "Config: batchSize=${issuePriceProperties.batchSize}" }

        try {
            issuePriceService.updatePricesForAllCoins()
            logger.info { "Issue price update completed" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to update coin prices" }
        }
    }
}
