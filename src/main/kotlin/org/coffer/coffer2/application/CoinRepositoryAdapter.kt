package org.coffer.coffer2.application

import org.coffer.coffer2.api.CoinSearchQuery
import org.coffer.coffer2.domain.coin.Coin
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CoinRepositoryAdapter {
    fun save(coin: Coin): Coin
    fun findById(id: UUID): Coin?
    fun deleteById(id: UUID)
    fun existsById(id: UUID): Boolean
    fun search(query: CoinSearchQuery, pageable: Pageable): Page<Coin>

    /**
     * Finds all coins that have a Numista ID.
     */
    fun findByNumistaIdIsNotNull(): List<Coin>

    /**
     * Finds all coins in the repository.
     */
    fun findAll(): List<Coin>
}