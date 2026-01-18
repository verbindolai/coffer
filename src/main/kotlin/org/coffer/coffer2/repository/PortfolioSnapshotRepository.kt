package org.coffer.coffer2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface PortfolioSnapshotRepository : JpaRepository<PortfolioSnapshotEntity, UUID> {

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        WHERE ps.snapshotDate >= :startDate
        ORDER BY ps.snapshotDate ASC
    """)
    fun findBySnapshotDateAfter(startDate: LocalDate): List<PortfolioSnapshotEntity>

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        ORDER BY ps.snapshotDate ASC
    """)
    fun findAllOrderBySnapshotDateAsc(): List<PortfolioSnapshotEntity>

    @Query("""
        SELECT ps FROM PortfolioSnapshotEntity ps
        ORDER BY ps.snapshotDate DESC
        LIMIT 1
    """)
    fun findLatest(): PortfolioSnapshotEntity?

    fun findBySnapshotDate(date: LocalDate): PortfolioSnapshotEntity?
}
