package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.ValuationTimeframe
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PortfolioValuationServiceImpl(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val snapshotValuationService: SnapshotValuationService
) : PortfolioValuationService {

    @Transactional(readOnly = true)
    override fun getValuation(timeframe: ValuationTimeframe): PortfolioValuationResult {
        val coins = coinRepositoryAdapter.findAll()
        if (coins.isEmpty()) {
            return PortfolioValuationResult.empty(timeframe)
        }

        val (metalValuation, collectorValuation) = snapshotValuationService.computeValuation(coins, timeframe)

        return PortfolioValuationResult.create(
            timeframe = timeframe,
            metalValuation = metalValuation,
            collectorValuation = collectorValuation
        )
    }
}
