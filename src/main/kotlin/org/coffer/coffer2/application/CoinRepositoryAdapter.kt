package org.coffer.coffer2.application

import org.coffer.coffer2.domain.coin.Coin
import java.util.UUID

interface CoinRepositoryAdapter {
    fun save(coin: Coin): Coin
    fun findById(id: UUID): Coin?
}