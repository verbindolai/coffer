package org.coffer.coffer2.repository

import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.coin.Coin
import org.springframework.stereotype.Component

@Component
class CoinRepositoryAdapterImpl(
    private val coinRepository: CoinRepository
): CoinRepositoryAdapter {
    override fun save(coin: Coin): Coin {
        return coinRepository.save(CoinEntity.fromCoin(coin)).toCoin()
    }
}