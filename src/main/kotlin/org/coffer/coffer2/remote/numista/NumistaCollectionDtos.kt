package org.coffer.coffer2.remote.numista

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaOAuthTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val user_id: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaCollectedItemsResponse(
    val item_count: Int,
    val items: List<NumistaCollectedItem>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaCollectedItem(
    val id: Int,
    val quantity: Int,
    val type: NumistaCollectedItemType,
    val issue: NumistaCollectedItemIssue?,
    val grade: String?,
    val for_swap: Boolean,
    val private_comment: String?,
    val public_comment: String?,
    val price: NumistaCollectedItemPrice?,
    val weight: Double?,
    val size: Double?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaCollectedItemType(
    val id: Int,
    val title: String,
    val category: String,
    val issuer: NumistaIssuer?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaCollectedItemIssue(
    val id: Int,
    val is_dated: Boolean?,
    val year: Int?,
    val gregorian_year: Int?,
    val min_year: Int?,
    val max_year: Int?,
    val mint_letter: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NumistaCollectedItemPrice(
    val value: Double?,
    val currency: String?,
)
