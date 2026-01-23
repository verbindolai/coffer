package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import java.util.UUID

data class CoinIssueInfo(
    val coin: Coin,
    val issueIds: List<UUID>,
    val grade: CoinGrade,
    val isExactMatch: Boolean
)
