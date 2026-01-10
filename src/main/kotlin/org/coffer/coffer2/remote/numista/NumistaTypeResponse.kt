package org.coffer.coffer2.remote.numista

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaTypeResponse(
    val id: String,
    val title: String?,
    val obverse: NumistaImageInfo?,
    val reverse: NumistaImageInfo?,
    val issuer: NumistaIssuer?,
    val min_year: Int?,
    val max_year: Int?,
    val weight: Double?, // Weight in grams
    val size: Double?, // Diameter in mm
    val thickness: Double?, // Thickness in mm
    val composition: NumistaComposition?,
    val ruler: List<NumistaRuler>?,
    val mints: List<NumistaMint>?,
    val value: NumistaValue?,
    val type: String?,
    // e.g., "Tetradecagonal (14-sided)", "Round", "Circular"
    val shape: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaImageInfo(
    val picture: String?, // Full-size image URL
    val thumbnail: String?,
    val description: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaIssuer(
    val code: String?,
    val name: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaComposition(
    val text: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaRuler(
    val id: Int?,
    val name: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaMint(
    val id: Int?,
    val name: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaValue(
    val text: String?,
    val numeric_value: Double?,
)
