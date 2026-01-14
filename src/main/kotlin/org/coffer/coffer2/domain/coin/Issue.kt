package org.coffer.coffer2.domain.coin

import java.time.ZonedDateTime
import java.util.UUID

data class Issue(
    val id: UUID? = null,
    val numistaId: String,
    val year: Int?,
    val mintage: Long?,
    val mintLetter: String?,
    val comment: String?,
    val isProof: Boolean = false,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val lastPriceFetchAttempt: ZonedDateTime? = null,
)
