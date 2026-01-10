package org.coffer.coffer2.repository

import jakarta.persistence.*
import org.coffer.coffer2.domain.coin.Issue
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "issues")
data class IssueEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 50)
    val numistaId: String,

    @Column(nullable = false)
    val coinId: UUID,

    @Column
    val year: Int?,

    @Column
    val mintage: Long?,

    @Column(length = 10)
    val mintLetter: String?,

    @Column(columnDefinition = "TEXT")
    val comment: String?,

    @Column(nullable = false)
    val isProof: Boolean = false,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now()
) {
    fun toIssue(): Issue = Issue(
        id = id.toString(),
        numistaId = numistaId,
        coinId = coinId.toString(),
        year = year,
        mintage = mintage,
        mintLetter = mintLetter,
        comment = comment,
        isProof = isProof,
        createdAt = createdAt
    )

    companion object {
        fun fromIssue(issue: Issue): IssueEntity = IssueEntity(
            id = issue.id?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            numistaId = issue.numistaId,
            coinId = UUID.fromString(issue.coinId),
            year = issue.year,
            mintage = issue.mintage,
            mintLetter = issue.mintLetter,
            comment = issue.comment,
            isProof = issue.isProof,
            createdAt = issue.createdAt
        )
    }
}
