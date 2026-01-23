package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.ValuationTimeframe
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PortfolioValuationServiceImpl(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val realTimeMetalValuationService: RealTimeMetalValuationService,
    private val realTimeCollectorValuationService: RealTimeCollectorValuationService,
    private val snapshotValuationService: SnapshotValuationService
) : PortfolioValuationService {

    companion object {
        private val REAL_TIME_TIMEFRAMES = setOf(ValuationTimeframe.HOUR_1, ValuationTimeframe.DAY_1)
    }

    @Transactional(readOnly = true)
    override fun getValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        val coins = coinRepositoryAdapter.findAll()
        if (coins.isEmpty()) {
            return PortfolioValuationResult.empty(timeframe)
        }

        return if (timeframe in REAL_TIME_TIMEFRAMES) {
            val now = java.time.ZonedDateTime.now()
            val startTime = timeframe.getStartTime(now)!!

            PortfolioValuationResult.create(
                timeframe = timeframe,
                metalValuation = realTimeMetalValuationService.computeTimeSeries(coins, startTime, timeframe),
                collectorValuation = realTimeCollectorValuationService.computeTimeSeries(coins, startTime, timeframe)
            )
        } else {
            val (metalValuation, collectorValuation) = snapshotValuationService.computeSnapshotValuation(coins, timeframe)

            PortfolioValuationResult.create(
                timeframe = timeframe,
                metalValuation = metalValuation,
                collectorValuation = collectorValuation
            )
        }
    }
}
