package org.coffer.coffer2.domain.coin

import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

data class IssuePrice(
    val id: String? = null,
    val issueId: String,
    val grade: CoinGrade,
    val price: BigDecimal,
    val currency: Currency,
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
