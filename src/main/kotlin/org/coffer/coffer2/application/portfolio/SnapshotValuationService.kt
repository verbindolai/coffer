package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.ValuationTimeframe
import org.coffer.coffer2.domain.bucketByInterval
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.repository.PortfolioSnapshotRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class SnapshotValuationService(
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository,
    private val realTimeMetalValuationService: RealTimeMetalValuationService,
    private val realTimeCollectorValuationService: RealTimeCollectorValuationService
) {

    fun computeSnapshotValuation(
        coins: List<Coin>,
        timeframe: ValuationTimeframe
    ): Pair<PortfolioMetalValuationResult?, PortfolioCollectorValuationResult?> {
        val now = ZonedDateTime.now()
        val startTime = timeframe.getStartTime(now)

        val snapshots = if (startTime != null) {
            portfolioSnapshotRepository.findBySnapshotDateAfter(startTime.toLocalDate())
        } else {
            portfolioSnapshotRepository.findAllOrderBySnapshotDateAsc()
        }

        if (snapshots.isEmpty() && coins.isEmpty()) {
            return null to null
        }

        val zone = ZoneId.systemDefault()
        val bucketedSnapshots = bucketByInterval(snapshots, timeframe) { it.snapshotDate.atStartOfDay(zone) }

        val metalDataPoints = bucketedSnapshots.mapNotNull { (bucketTime, snapshotsInBucket) ->
            val lastSnapshot = snapshotsInBucket.last()
            if (lastSnapshot.metalValue == null) return@mapNotNull null

            PortfolioMetalPoint(
                timestamp = bucketTime,
                totalValue = lastSnapshot.metalValue,
                goldGrams = lastSnapshot.goldGrams ?: BigDecimal.ZERO,
                silverGrams = lastSnapshot.silverGrams ?: BigDecimal.ZERO,
                platinumGrams = lastSnapshot.platinumGrams ?: BigDecimal.ZERO
            )
        }

        val collectorDataPoints = bucketedSnapshots.mapNotNull { (bucketTime, snapshotsInBucket) ->
            val lastSnapshot = snapshotsInBucket.last()
            val hasCollectorValues = lastSnapshot.collectorValueExact != null ||
                lastSnapshot.collectorValueMin != null ||
                lastSnapshot.collectorValueMax != null

            if (!hasCollectorValues) return@mapNotNull null

            PortfolioCollectorPoint(
                timestamp = bucketTime,
                exactValue = lastSnapshot.collectorValueExact,
                minValue = lastSnapshot.collectorValueMin,
                maxValue = lastSnapshot.collectorValueMax
            )
        }

        // Append live data points computed from current state
        val liveMetalPoint = realTimeMetalValuationService.computeLivePoint(coins, now)
        val liveCollectorPoint = realTimeCollectorValuationService.computeLivePoint(coins, now)

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
