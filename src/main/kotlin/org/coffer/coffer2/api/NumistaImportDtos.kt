package org.coffer.coffer2.api

data class NumistaAuthUrlResponse(val url: String)

data class NumistaImportCallbackRequest(val code: String, val redirectUri: String)

data class NumistaImportResultResponse(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<String>,
)
