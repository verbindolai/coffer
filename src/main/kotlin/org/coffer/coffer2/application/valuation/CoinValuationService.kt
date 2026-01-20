package org.coffer.coffer2.application.valuation

import org.coffer.coffer2.domain.CoinValuationResult
import org.coffer.coffer2.domain.CurrentPricesResult
import org.coffer.coffer2.domain.ValuationTimeframe
import java.util.UUID

interface CoinValuationService {

    fun getValuation(coinId: UUID, timeframe: ValuationTimeframe): CoinValuationResult

    fun getCurrentPrices(coinId: UUID): CurrentPricesResult
}
