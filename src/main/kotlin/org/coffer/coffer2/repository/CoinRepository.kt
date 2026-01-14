package org.coffer.coffer2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CoinRepository : JpaRepository<CoinEntity, UUID>, JpaSpecificationExecutor<CoinEntity> {
    /**
     * Finds all coins that have a Numista ID.
     * Used to get all coins that can have prices fetched from Numista.
     */
    fun findByNumistaIdIsNotNull(): List<CoinEntity>
}
