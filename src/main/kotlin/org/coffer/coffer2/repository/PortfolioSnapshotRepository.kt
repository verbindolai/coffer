package org.coffer.coffer2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Repository
interface PortfolioSnapshotRepository : JpaRepository<PortfolioSnapshotEntity, UUID> {

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        WHERE ps.createdAt >= :startTime
        ORDER BY ps.createdAt ASC
    """)
    fun findByCreatedAtAfterOrderByCreatedAtAsc(startTime: ZonedDateTime): List<PortfolioSnapshotEntity>

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        ORDER BY ps.createdAt ASC
    """)
    fun findAllOrderByCreatedAtAsc(): List<PortfolioSnapshotEntity>

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        WHERE ps.snapshotDate >= :startDate
        ORDER BY ps.snapshotDate ASC, ps.createdAt ASC
    """)
    fun findBySnapshotDateAfter(startDate: LocalDate): List<PortfolioSnapshotEntity>

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        ORDER BY ps.snapshotDate ASC, ps.createdAt ASC
    """)
    fun findAllOrderBySnapshotDateAsc(): List<PortfolioSnapshotEntity>

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        ORDER BY ps.createdAt DESC
        LIMIT 1
    """)
    fun findLatest(): PortfolioSnapshotEntity?

    fun findBySnapshotDate(date: LocalDate): List<PortfolioSnapshotEntity>
}
