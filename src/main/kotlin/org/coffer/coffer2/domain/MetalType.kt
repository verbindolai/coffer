package org.coffer.coffer2.domain

enum class MetalType {
    SILVER,
    GOLD,
    PLATINUM,
    NICKEL,
    OTHER,
    ;

    fun toSwissquoteSymbol(): String =
        when (this) {
            GOLD -> "XAU"
            SILVER -> "XAG"
            PLATINUM -> "XPT"
            NICKEL -> "XNIK"
            OTHER -> throw IllegalArgumentException("MetalType $this is not supported by Swissquote")
        }
}
