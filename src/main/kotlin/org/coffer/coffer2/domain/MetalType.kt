package org.coffer.coffer2.domain

enum class MetalType {
    SILVER,
    GOLD,
    PLATINUM,
    NICKEL,
    BASE_METAL;

    fun toSymbol(): String =
        when (this) {
            GOLD -> "XAU"
            SILVER -> "XAG"
            PLATINUM -> "XPT"
            NICKEL -> "XNIK"
            BASE_METAL -> "BASE"
        }

    companion object {
        fun fromSymbol(symbol: String): MetalType =
            when (symbol) {
                "XAU" -> GOLD
                "XAG" -> SILVER
                "XPT" -> PLATINUM
                "XNIK" -> NICKEL
                "BASE" -> BASE_METAL
                else -> {
                    throw IllegalArgumentException("Unknown metal symbol: $symbol")
                }
            }
    }
}
