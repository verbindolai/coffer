package org.coffer.coffer2.application

import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.coffer.coffer2.domain.coin.CreateCoinCommand
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class CoinService(
    private val coinRepository: CoinRepositoryAdapter,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun createCoin(command: CreateCoinCommand): Coin {
        val coin = command.toCoin()
        val savedCoin = coinRepository.save(coin)
        applicationEventPublisher.publishEvent(CoinCreatedEvent(savedCoin.id, savedCoin.numistaId))
        return savedCoin
    }
}