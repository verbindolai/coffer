package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.ValuationTimeframe

interface PortfolioValuationService {
    /**
     * Gets portfolio valuation data for a given timeframe.
     *
     * For short timeframes (1h, 1d): computes in real-time from current coin data and metal/issue prices.
     * For longer timeframes (1w, 1m, 1y, max): uses pre-computed daily snapshots.
     */
    fun getValuation(timeframe: ValuationTimeframe): PortfolioValuationResult
}
