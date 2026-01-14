package org.coffer.coffer2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CoinImageRepository : JpaRepository<CoinImageEntity, UUID> {
    fun findByCoinId(coinId: UUID): List<CoinImageEntity>
    fun findByCoinIdAndId(coinId: UUID, id: UUID): CoinImageEntity?
}
