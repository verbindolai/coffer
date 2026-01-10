package org.coffer.coffer2.remote.numista

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaIssueResponse(
    val id: Int,
    @JsonProperty("is_dated")
    val isDated: Boolean?,
    val year: Int?,
    @JsonProperty("gregorian_year")
    val gregorianYear: Int?,
    @JsonProperty("min_year")
    val minYear: Int?,
    @JsonProperty("max_year")
    val maxYear: Int?,
    @JsonProperty("mint_letter")
    val mintLetter: String?,
    val mintage: Long?,
    val comment: String?,
) {
    val isProof = comment?.lowercase()?.trim()?.equals("proof") ?: false
}
