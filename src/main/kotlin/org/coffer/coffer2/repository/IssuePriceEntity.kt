package org.coffer.coffer2.repository

import jakarta.persistence.*
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.IssuePrice
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "issue_prices")
data class IssuePriceEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val issueId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val grade: CoinGrade,

    @Column(nullable = false, precision = 19, scale = 4)
    val price: BigDecimal,

    @Column(nullable = false, length = 3)
    val currencyCode: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now()
) {
    fun toIssuePrice(): IssuePrice = IssuePrice(
        id = id.toString(),
        issueId = issueId.toString(),
        grade = grade,
        price = price,
        currency = Currency.getInstance(currencyCode),
        createdAt = createdAt
    )

    companion object {
        fun fromIssuePrice(issuePrice: IssuePrice): IssuePriceEntity = IssuePriceEntity(
            id = issuePrice.id?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            issueId = UUID.fromString(issuePrice.issueId),
            grade = issuePrice.grade,
            price = issuePrice.price,
            currencyCode = issuePrice.currency.currencyCode,
            createdAt = issuePrice.createdAt
        )
    }
}
