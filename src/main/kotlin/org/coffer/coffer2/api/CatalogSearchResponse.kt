package org.coffer.coffer2.api

import org.coffer.coffer2.application.catalog.CatalogSearchResultItem
import org.coffer.coffer2.application.catalog.CatalogSearchResult

data class CatalogSearchResponse(
    val totalCount: Int,
    val results: List<CatalogSearchResultItemResponse>
) {
    companion object {
        fun from(result: CatalogSearchResult) = CatalogSearchResponse(
            totalCount = result.totalCount,
            results = result.items.map { CatalogSearchResultItemResponse.from(it) }
        )
    }
}

data class CatalogSearchResultItemResponse(
    val typeId: String,
    val title: String,
    val category: String?,
    val issuerCode: String?,
    val issuerName: String?,
    val minYear: Int?,
    val maxYear: Int?,
    val obverseThumbnailUrl: String?,
    val reverseThumbnailUrl: String?
) {
    companion object {
        fun from(item: CatalogSearchResultItem) = CatalogSearchResultItemResponse(
            typeId = item.typeId,
            title = item.title,
            category = item.category,
            issuerCode = item.issuerCode,
            issuerName = item.issuerName,
            minYear = item.minYear,
            maxYear = item.maxYear,
            obverseThumbnailUrl = item.obverseThumbnailUrl,
            reverseThumbnailUrl = item.reverseThumbnailUrl
        )
    }
}
