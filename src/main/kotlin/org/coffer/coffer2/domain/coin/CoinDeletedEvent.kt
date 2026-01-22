package org.coffer.coffer2.domain.coin

import java.util.UUID

data class CoinDeletedEvent(
    val coinId: UUID
)
