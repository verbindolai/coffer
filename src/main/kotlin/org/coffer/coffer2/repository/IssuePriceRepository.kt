package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.CoinGrade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
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

    /**
     * Finds historical prices for issues within a time range.
     */
    @Query("""
        SELECT ip FROM IssuePriceEntity ip
        WHERE ip.issueId IN :issueIds
        AND ip.grade = :grade
        AND ip.createdAt >= :startTime
        ORDER BY ip.createdAt ASC
    """)
    fun findByIssueIdsAndGradeAfter(
        issueIds: List<UUID>,
        grade: CoinGrade,
        startTime: ZonedDateTime
    ): List<IssuePriceEntity>

    /**
     * Finds all historical prices for issues with a specific grade.
     */
    @Query("""
        SELECT ip FROM IssuePriceEntity ip
        WHERE ip.issueId IN :issueIds
        AND ip.grade = :grade
        ORDER BY ip.createdAt ASC
    """)
    fun findByIssueIdsAndGrade(
        issueIds: List<UUID>,
        grade: CoinGrade
    ): List<IssuePriceEntity>

    /**
     * Finds historical prices for issues (all grades) within a time range.
     */
    @Query("""
        SELECT ip FROM IssuePriceEntity ip
        WHERE ip.issueId IN :issueIds
        AND ip.createdAt >= :startTime
        ORDER BY ip.createdAt ASC
    """)
    fun findByIssueIdsAfter(
        issueIds: List<UUID>,
        startTime: ZonedDateTime
    ): List<IssuePriceEntity>

    /**
     * Finds the latest price before a given time for each issue ID at a specific grade.
     * Used to seed forward-fill with the last known price before a time window.
     */
    @Query(value = """
        SELECT DISTINCT ON (ip.issue_id) ip.*
        FROM issue_prices ip
        WHERE ip.issue_id IN :issueIds
        AND ip.grade = CAST(:grade AS varchar)
        AND ip.created_at < :before
        ORDER BY ip.issue_id, ip.created_at DESC
    """, nativeQuery = true)
    fun findLatestBeforeByIssueIdsAndGrade(
        issueIds: List<UUID>,
        grade: CoinGrade,
        before: ZonedDateTime
    ): List<IssuePriceEntity>

    /**
     * Finds all historical prices for issues (all grades).
     */
    @Query("""
        SELECT ip FROM IssuePriceEntity ip
        WHERE ip.issueId IN :issueIds
        ORDER BY ip.createdAt ASC
    """)
    fun findByIssueIds(issueIds: List<UUID>): List<IssuePriceEntity>
}
