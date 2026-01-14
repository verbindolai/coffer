package org.coffer.coffer2.repository

import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.coin.Coin
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CoinRepositoryAdapterImpl(
    private val coinRepository: CoinRepository
): CoinRepositoryAdapter {
    override fun save(coin: Coin): Coin {
        return coinRepository.save(CoinEntity.fromCoin(coin)).toCoin()
    }

    override fun findById(id: UUID): Coin? {
        return coinRepository.findByIdOrNull(id)?.toCoin()
    }

    override fun findByNumistaIdIsNotNull(): List<Coin> {
        return coinRepository.findByNumistaIdIsNotNull().map { it.toCoin() }
    }
}