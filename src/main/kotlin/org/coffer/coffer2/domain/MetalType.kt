package org.coffer.coffer2.domain

enum class MetalType {
    SILVER,
    GOLD,
    PLATINUM,
    NICKEL;

    fun toSymbol(): String =
        when (this) {
            GOLD -> "XAU"
            SILVER -> "XAG"
            PLATINUM -> "XPT"
            NICKEL -> "XNIK"
        }

    companion object {
        fun fromSymbol(symbol: String): MetalType =
            when (symbol) {
                "XAU" -> GOLD
                "XAG" -> SILVER
                "XPT" -> PLATINUM
                "XNIK" -> NICKEL
                else -> {
                    throw IllegalArgumentException("Unknown metal symbol: $symbol")
                }
            }
    }
}
