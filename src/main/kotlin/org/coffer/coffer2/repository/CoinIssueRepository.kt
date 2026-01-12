package org.coffer.coffer2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CoinIssueRepository : JpaRepository<CoinIssueEntity, CoinIssueId> {
    fun findByCoinId(coinId: UUID): List<CoinIssueEntity>

    fun existsByCoinId(coinId: UUID): Boolean

    @Query("SELECT ci.issueId FROM CoinIssueEntity ci WHERE ci.coinId = :coinId")
    fun findIssueIdsByCoinId(coinId: UUID): List<UUID>

    @Query("""
        SELECT DISTINCT ci.issueId
        FROM CoinIssueEntity ci
        JOIN CoinEntity c ON ci.coinId = c.id
        WHERE c.numistaId = :coinTypeNumistaId
    """)
    fun findIssueIdsByCoinTypeNumistaId(coinTypeNumistaId: String): List<UUID>
}
