package org.coffer.coffer2.application

import org.coffer.coffer2.api.CoinSearchQuery
import org.coffer.coffer2.domain.coin.Coin
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime
import java.util.UUID

interface CoinRepositoryAdapter {
    fun save(coin: Coin): Coin
    fun findById(id: UUID): Coin?
    fun deleteById(id: UUID)
    fun existsById(id: UUID): Boolean
    fun search(query: CoinSearchQuery, pageable: Pageable): Page<Coin>

    /**
     * Finds all active coins that have a Numista ID.
     */
    fun findByNumistaIdIsNotNull(): List<Coin>

    /**
     * Finds all active coins in the repository.
     */
    fun findAll(): List<Coin>

    /**
     * Soft-deletes a coin by setting its deletedAt timestamp.
     */
    fun softDelete(id: UUID): Coin

    /**
     * Finds all active coins plus coins deleted after the given cutoff time.
     */
    fun findAllIncludingDeletedAfter(cutoff: ZonedDateTime): List<Coin>

    /**
     * Checks if an active coin with the given Numista ID exists.
     */
    fun existsByNumistaId(numistaId: String): Boolean

    /**
     * Searches coins and groups results by numistaId.
     * Coins without a numistaId are returned as standalone groups of 1.
     */
    fun searchGrouped(query: CoinSearchQuery, pageable: Pageable): Triple<List<List<Coin>>, Long, Long>

    /**
     * Finds all active coins with the given Numista type ID.
     */
    fun findByNumistaId(numistaId: String): List<Coin>
}