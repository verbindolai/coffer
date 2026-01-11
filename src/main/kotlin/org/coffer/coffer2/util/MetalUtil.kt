package org.coffer.coffer2.util

import java.math.BigDecimal
import java.math.RoundingMode

object MetalUtil {
    val GRAMS_PER_TROY_OUNCE = BigDecimal("31.1034768")

    fun troyOunceToGram(pricePerOunce: BigDecimal): BigDecimal {
        return pricePerOunce.divide(GRAMS_PER_TROY_OUNCE, 8, RoundingMode.HALF_UP)
    }
}