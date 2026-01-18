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
}
