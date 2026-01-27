package org.coffer.coffer2.schedule

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.portfolio.PortfolioSnapshotService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PortfolioSnapshotScheduler(
    private val portfolioSnapshotService: PortfolioSnapshotService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Creates portfolio snapshots hourly.
     * Configurable via coffer.portfolio.snapshot-cron
     */
    @Scheduled(cron = "\${coffer.portfolio.snapshot-cron:0 0 * * * *}")
    fun createHourlySnapshot() {
        logger.info { "Creating hourly portfolio snapshot" }
        try {
            val snapshot = portfolioSnapshotService.computeAndStoreSnapshot()
            logger.info { "Portfolio snapshot created for ${snapshot.snapshotDate}: ${snapshot.totalCoins} coins" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to create portfolio snapshot" }
        }
    }
}
