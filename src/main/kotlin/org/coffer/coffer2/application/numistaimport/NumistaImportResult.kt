package org.coffer.coffer2.application.numistaimport

data class NumistaImportResult(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<String>,
)
