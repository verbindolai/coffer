package org.coffer.coffer2.repository

import org.coffer.coffer2.application.coinimage.CoinImageRepositoryAdapter
import org.coffer.coffer2.domain.coin.CoinImage
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CoinImageRepositoryAdapterImpl(
    private val coinImageRepository: CoinImageRepository
): CoinImageRepositoryAdapter {
    override fun save(coinImage: CoinImage): CoinImage {
        return coinImageRepository.save(CoinImageEntity.fromCoinImage(coinImage)).toCoinImage()
    }

    override fun findByCoinId(coinId: UUID): List<CoinImage> {
        coinImageRepository.findByCoinId(coinId).let { entities ->
            return entities.map { it.toCoinImage() }
        }
    }

}