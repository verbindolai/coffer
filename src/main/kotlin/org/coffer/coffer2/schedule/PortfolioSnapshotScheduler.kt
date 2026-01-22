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
     * Creates portfolio snapshots on a configurable schedule.
     * Default: 9 AM, 3 PM, 10 PM daily, configurable via coffer.portfolio.snapshot-cron
     */
    @Scheduled(cron = "\${coffer.portfolio.snapshot-cron:0 0 9,15,22 * * *}")
    fun createDailySnapshot() {
        logger.info { "Creating daily portfolio snapshot" }
        try {
            val snapshot = portfolioSnapshotService.computeAndStoreSnapshot()
            logger.info {
                "Portfolio snapshot created: date=${snapshot.snapshotDate}, " +
                "coins=${snapshot.totalCoins}, quantity=${snapshot.totalQuantity}, " +
                "metalValue=${snapshot.metalValue}, collectorExact=${snapshot.collectorValueExact}, " +
                "collectorMin=${snapshot.collectorValueMin}, collectorMax=${snapshot.collectorValueMax}"
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to create portfolio snapshot" }
        }
    }
}
