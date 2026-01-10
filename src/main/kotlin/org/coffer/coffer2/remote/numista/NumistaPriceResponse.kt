package org.coffer.coffer2.remote.numista

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Response from Numista API for catalogue prices endpoint
 * GET /api/v3/types/{id}/prices
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaPriceResponse(
    val currency: String,
    val prices: List<NumistaPriceByGrade>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaPriceByGrade(
    val grade: String, // Numista grade codes: vg, f, vf, xf, au, unc
    val price: Double,
)
