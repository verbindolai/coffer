package org.coffer.coffer2.application

import org.coffer.coffer2.domain.coin.Coin

interface CoinRepositoryAdapter {
    fun save(coin: Coin): Coin
}