package org.coffer.coffer2.application.catalog

data class CatalogSearchResult(
    val totalCount: Int,
    val items: List<CatalogSearchResultItem>
)

data class CatalogSearchResultItem(
    val typeId: String,
    val title: String,
    val category: String?,
    val issuerCode: String?,
    val issuerName: String?,
    val minYear: Int?,
    val maxYear: Int?,
    val obverseThumbnailUrl: String?,
    val reverseThumbnailUrl: String?
)
