package org.coffer.coffer2.domain.coin

enum class CoinGrade {
    GOOD,
    VERY_GOOD,
    FINE,
    VERY_FINE,
    EXTREMELY_FINE,
    ABOUT_UNCIRCULATED,
    UNCIRCULATED,
    PROOF,
    ;

    companion object {
        /**
         * Maps Numista grade codes to CoinGrade enum values.
         * Numista uses abbreviated codes: vg, f, vf, xf, au, unc
         */
        fun fromNumistaGrade(numistaGrade: String): CoinGrade? =
            when (numistaGrade.lowercase()) {
                "g" -> GOOD
                "vg" -> VERY_GOOD
                "f" -> FINE
                "vf" -> VERY_FINE
                "xf" -> EXTREMELY_FINE
                "au" -> ABOUT_UNCIRCULATED
                "unc" -> UNCIRCULATED
                else -> null
            }

        /**
         * Converts CoinGrade to Numista grade code.
         * Returns null if the grade doesn't have a Numista equivalent.
         */
        fun toNumistaGrade(grade: CoinGrade): String? =
            when (grade) {
                GOOD -> "g"
                VERY_GOOD -> "vg"
                FINE -> "f"
                VERY_FINE -> "vf"
                EXTREMELY_FINE -> "xf"
                ABOUT_UNCIRCULATED -> "au"
                UNCIRCULATED -> "unc"
                else -> null
            }
    }

    /**
     * Returns the Numista grade code for this CoinGrade, or null if no mapping exists.
     */
    fun toNumistaGrade(): String? = Companion.toNumistaGrade(this)
}
