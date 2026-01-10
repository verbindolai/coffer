package org.coffer.coffer2.remote.numista

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaSearchResponseDto(
    val count: Int,
    val types: List<NumistaSearchResultDto>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaSearchResultDto(
    val id: Int,
    val title: String,
    val category: String?,
    val issuer: NumistaIssuer?,
    @JsonProperty("min_year")
    val minYear: Int?,
    @JsonProperty("max_year")
    val maxYear: Int?,
    @JsonProperty("obverse_thumbnail")
    val obverseThumbnail: String?,
    @JsonProperty("reverse_thumbnail")
    val reverseThumbnail: String?,
)
