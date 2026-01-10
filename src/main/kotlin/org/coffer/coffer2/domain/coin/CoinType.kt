package org.coffer.coffer2.domain.coin

enum class CoinType {
    BULLION,
    COMMEMORATIVE_CIRCULATION,
    STANDARD_CIRCULATION,
    COMMEMORATIVE_NON_CIRCULATION,
    OTHER,
    ;

    companion object {
        fun fromNumistaString(value: String?): CoinType =
            when (value?.lowercase()) {
                "bullion coins" -> BULLION
                "circulating commemorative coins" -> COMMEMORATIVE_CIRCULATION
                "non-circulating coins" -> COMMEMORATIVE_NON_CIRCULATION
                "standard circulation coins" -> STANDARD_CIRCULATION
                else -> OTHER
            }
    }
}
