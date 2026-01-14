package org.coffer.coffer2.repository

import org.coffer.coffer2.application.coinimage.CoinImageRepositoryAdapter
import org.coffer.coffer2.domain.coin.CoinImage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CoinImageRepositoryAdapterImpl(
    private val coinImageRepository: CoinImageRepository
): CoinImageRepositoryAdapter {
    override fun save(coinImage: CoinImage): CoinImage {
        return coinImageRepository.save(CoinImageEntity.fromCoinImage(coinImage)).toCoinImage()
    }

    override fun findById(id: UUID): CoinImage? {
        return coinImageRepository.findByIdOrNull(id)?.toCoinImage()
    }

    override fun findByCoinId(coinId: UUID): List<CoinImage> {
        return coinImageRepository.findByCoinId(coinId).map { it.toCoinImage() }
    }

    override fun findByCoinIdAndImageId(coinId: UUID, imageId: UUID): CoinImage? {
        return coinImageRepository.findByCoinIdAndId(coinId, imageId)?.toCoinImage()
    }
}