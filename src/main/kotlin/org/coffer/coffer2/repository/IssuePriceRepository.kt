package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.CoinGrade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface IssuePriceRepository : JpaRepository<IssuePriceEntity, UUID> {
    /**
     * Finds all prices for a given issue.
     */
    fun findByIssueId(issueId: UUID): List<IssuePriceEntity>

    /**
     * Finds a specific price for an issue at a given grade.
     * Note: This returns the most recent price if multiple exist.
     */
    @Query("""
        SELECT ip FROM IssuePriceEntity ip
        WHERE ip.issueId = :issueId AND ip.grade = :grade
        ORDER BY ip.createdAt DESC
        LIMIT 1
    """)
    fun findLatestByIssueIdAndGrade(issueId: UUID, grade: CoinGrade): IssuePriceEntity?

    /**
     * Finds the latest prices for each grade for a given issue.
     * Uses DISTINCT ON to get the most recent price per grade.
     */
    @Query(value = """
        SELECT DISTINCT ON (ip.grade) ip.*
        FROM issue_prices ip
        WHERE ip.issue_id = :issueId
        ORDER BY ip.grade, ip.created_at DESC
    """, nativeQuery = true)
    fun findLatestPricesByIssueId(issueId: UUID): List<IssuePriceEntity>
}
