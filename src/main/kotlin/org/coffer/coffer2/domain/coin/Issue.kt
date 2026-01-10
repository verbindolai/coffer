package org.coffer.coffer2.domain.coin

import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

data class Issue(
    val id: String? = null,
    val numistaId: String,
    val coinId: String,
    val year: Int?,
    val mintage: Long?,
    val mintLetter: String?,
    val comment: String?,
    val isProof: Boolean = false,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
)
