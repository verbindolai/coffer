package org.coffer.coffer2.application

import org.coffer.coffer2.domain.MetalType
import org.springframework.stereotype.Component

@Component
class CompositionParserImpl : CompositionParser {

    companion object {
        private val PURITY_PATTERNS = listOf(
            // Parenthetical decimal: "Gold (.9999)" -> 9999
            Regex("""\(\.(\d{3,4})\)"""),
            // Permille: "Gold 900‰" or "Gold 900\u2030" -> 900
            Regex("""(\d{3,4})\s*[‰\u2030]"""),
            // Plain number after metal: "Gold 916.7" or "Silver 925"
            Regex("""(?:Gold|Silver|Platinum|Nickel)\s+(\d{3}(?:\.\d)?)""", RegexOption.IGNORE_CASE),
            // Decimal without parens: "0.9999 Gold" -> 9999
            Regex("""0\.(\d{3,4})\s+(?:Gold|Silver|Platinum)""", RegexOption.IGNORE_CASE)
        )

        private val PRECIOUS_METAL_KEYWORDS = mapOf(
            "gold" to MetalType.GOLD,
            "silver" to MetalType.SILVER,
            "platinum" to MetalType.PLATINUM
        )

        private val EXCLUSION_KEYWORDS = listOf(
            "plated", "clad", "bi-metallic", "bimetallic"
        )

        private val BASE_METAL_KEYWORDS = listOf(
            "copper", "brass", "bronze", "steel", "aluminum", "aluminium",
            "zinc", "iron", "tin", "nickel", "copper-nickel", "cupronickel"
        )
    }

    override fun parse(compositionText: String?): ParsedComposition {
        if (compositionText.isNullOrBlank()) {
            return ParsedComposition(null, null)
        }

        val text = compositionText.lowercase()

        val hasExclusion = EXCLUSION_KEYWORDS.any { text.contains(it) }

        val metalType = if (!hasExclusion) {
            // First check for precious metals
            PRECIOUS_METAL_KEYWORDS.entries.firstOrNull { (keyword, _) ->
                text.contains(keyword)
            }?.value
                // Then check for base metals
                ?: if (BASE_METAL_KEYWORDS.any { text.contains(it) }) MetalType.BASE_METAL else null
        } else {
            null
        }

        val purity = extractPurity(compositionText)

        return ParsedComposition(metalType, purity)
    }

    private fun extractPurity(text: String): Int? {
        for (pattern in PURITY_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val rawValue = match.groupValues[1]
                return normalizePurity(rawValue)
            }
        }
        return null
    }

    private fun normalizePurity(value: String): Int {
        val numericValue = value.toDoubleOrNull() ?: return 0

        return when {
            numericValue > 1000 -> (numericValue / 10).toInt().coerceIn(1, 1000)
            else -> numericValue.toInt().coerceIn(1, 1000)
        }
    }
}
