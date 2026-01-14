package org.coffer.coffer2.repository

import jakarta.persistence.*
import org.coffer.coffer2.domain.coin. Issue
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "issues")
data class IssueEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 50, unique = true)
    val numistaId: String,

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
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column
    val lastPriceFetchAttempt: ZonedDateTime? = null
) {
    fun toIssue(): Issue = Issue(
        id = id,
        numistaId = numistaId,
        year = year,
        mintage = mintage,
        mintLetter = mintLetter,
        comment = comment,
        isProof = isProof,
        createdAt = createdAt,
        lastPriceFetchAttempt = lastPriceFetchAttempt
    )

    companion object {
        fun fromIssue(issue: Issue): IssueEntity = IssueEntity(
            id = issue.id ?: UUID.randomUUID(),
            numistaId = issue.numistaId,
            year = issue.year,
            mintage = issue.mintage,
            mintLetter = issue.mintLetter,
            comment = issue.comment,
            isProof = issue.isProof,
            createdAt = issue.createdAt,
            lastPriceFetchAttempt = issue.lastPriceFetchAttempt
        )
    }
}
