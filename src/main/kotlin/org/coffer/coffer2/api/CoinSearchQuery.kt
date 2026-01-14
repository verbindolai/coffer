package org.coffer.coffer2.api

import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType

data class CoinSearchQuery(
    val country: String? = null,
    val denomination: String? = null,
    val grade: CoinGrade? = null,
    val coinType: CoinType? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val metalType: MetalType? = null,
    val shape: CoinShape? = null,
    val currency: String? = null,
    val title: String? = null,
    val numistaId: String? = null,
)
