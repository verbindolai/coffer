package org.coffer.coffer2.repository

import jakarta.persistence.*
import java.io.Serializable
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "coin_issues")
@IdClass(CoinIssueId::class)
data class CoinIssueEntity(
    @Id
    @Column(name = "coin_id", nullable = false)
    val coinId: UUID,

    @Id
    @Column(name = "issue_id", nullable = false)
    val issueId: UUID,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)

data class CoinIssueId(
    val coinId: UUID = UUID.randomUUID(),
    val issueId: UUID = UUID.randomUUID()
) : Serializable
