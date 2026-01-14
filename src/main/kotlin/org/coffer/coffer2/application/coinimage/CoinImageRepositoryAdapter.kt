package org.coffer.coffer2.application.coinimage

import org.coffer.coffer2.domain.coin.CoinImage
import java.util.UUID

interface CoinImageRepositoryAdapter {
    fun save(coinImage: CoinImage): CoinImage
    fun findById(id: UUID): CoinImage?
    fun findByCoinId(coinId: UUID): List<CoinImage>
    fun findByCoinIdAndImageId(coinId: UUID, imageId: UUID): CoinImage?
}