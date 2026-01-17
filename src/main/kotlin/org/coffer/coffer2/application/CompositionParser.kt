package org.coffer.coffer2.application

import org.coffer.coffer2.domain.MetalType

interface CompositionParser {
    fun parse(compositionText: String?): ParsedComposition
}

data class ParsedComposition(
    val metalType: MetalType?,
    val purity: Int?
)
