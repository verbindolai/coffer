package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.repository.PortfolioSnapshotRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * Computes portfolio valuations from snapshots + live current point.
 */
@Component
class SnapshotValuationService(
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository,
    private val liveValuationService: LiveValuationService
) {

    fun computeValuation(
        coins: List<Coin>,
        timeframe: ValuationTimeframe
    ): Pair<PortfolioMetalValuationResult?, PortfolioCollectorValuationResult?> {
        val now = ZonedDateTime.now()
        val startTime = timeframe.getStartTime(now)

        val snapshots = if (startTime != null) {
            portfolioSnapshotRepository.findByCreatedAtAfterOrderByCreatedAtAsc(startTime)
        } else {
            portfolioSnapshotRepository.findAllOrderByCreatedAtAsc()
        }

        if (snapshots.isEmpty() && coins.isEmpty()) {
            return null to null
        }

        // Convert snapshots to data points
        val metalDataPoints = snapshots.mapNotNull { snapshot ->
            if (snapshot.metalValue == null) return@mapNotNull null

            PortfolioMetalPoint(
                timestamp = snapshot.createdAt,
                totalValue = snapshot.metalValue,
                goldGrams = snapshot.goldGrams ?: BigDecimal.ZERO,
                silverGrams = snapshot.silverGrams ?: BigDecimal.ZERO,
                platinumGrams = snapshot.platinumGrams ?: BigDecimal.ZERO
            )
        }

        val collectorDataPoints = snapshots.mapNotNull { snapshot ->
            val hasCollectorValues = snapshot.collectorValueMin != null || snapshot.collectorValueMax != null

            if (!hasCollectorValues) return@mapNotNull null

            PortfolioCollectorPoint(
                timestamp = snapshot.createdAt,
                minValue = snapshot.collectorValueMin,
                maxValue = snapshot.collectorValueMax
            )
        }

        // Append live data points computed from current state
        val liveMetalPoint = liveValuationService.computeLiveMetalPoint(coins, now)
        val liveCollectorPoint = liveValuationService.computeLiveCollectorPoint(coins, now)

        val finalMetalPoints = if (liveMetalPoint != null) {
            metalDataPoints + liveMetalPoint
        } else {
            metalDataPoints
        }

        val finalCollectorPoints = if (liveCollectorPoint != null) {
            collectorDataPoints + liveCollectorPoint
        } else {
            collectorDataPoints
        }

        val metalValuation = if (finalMetalPoints.isNotEmpty()) {
            PortfolioMetalValuationResult(finalMetalPoints)
        } else null

        val collectorValuation = if (finalCollectorPoints.isNotEmpty()) {
            PortfolioCollectorValuationResult(finalCollectorPoints)
        } else null

        return metalValuation to collectorValuation
    }
}
