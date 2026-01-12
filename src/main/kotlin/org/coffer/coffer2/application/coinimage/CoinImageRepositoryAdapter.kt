package org.coffer.coffer2.application.coinimage

import org.coffer.coffer2.domain.coin.CoinImage
import java.util.UUID

interface CoinImageRepositoryAdapter {
    fun save(coinImage: CoinImage): CoinImage
    fun findByCoinId(coinId: UUID): List<CoinImage>
}