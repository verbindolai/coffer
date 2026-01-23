package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.MetalType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import java.util.*

@Repository
interface MetalQuoteRepository : JpaRepository<MetalQuoteEntity, UUID> {

    @Query("""
        SELECT mq FROM MetalQuoteEntity mq
        WHERE mq.metalType = :metalType
        AND mq.quotedAt >= :startTime
        ORDER BY mq.quotedAt ASC
    """)
    fun findByMetalTypeAndQuotedAtAfter(
        metalType: MetalType,
        startTime: ZonedDateTime
    ): List<MetalQuoteEntity>

    @Query("""
        SELECT mq FROM MetalQuoteEntity mq
        WHERE mq.metalType = :metalType
        ORDER BY mq.quotedAt ASC
    """)
    fun findByMetalTypeOrderByQuotedAt(metalType: MetalType): List<MetalQuoteEntity>

    @Query("""
        SELECT mq FROM MetalQuoteEntity mq
        WHERE mq.metalType = :metalType
        ORDER BY mq.quotedAt DESC
        LIMIT 1
    """)
    fun findLatestByMetalType(metalType: MetalType): MetalQuoteEntity?

    /**
     * Finds the latest quote before a given time for each metal type.
     * Used to seed forward-fill with the last known price before a time window.
     */
    @Query(value = """
        SELECT DISTINCT ON (mq.metal_type) mq.*
        FROM metal_quotes mq
        WHERE mq.quoted_at < :before
        ORDER BY mq.metal_type, mq.quoted_at DESC
    """, nativeQuery = true)
    fun findLatestBefore(before: ZonedDateTime): List<MetalQuoteEntity>
}
