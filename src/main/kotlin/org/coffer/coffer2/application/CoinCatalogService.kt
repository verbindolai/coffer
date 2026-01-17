package org.coffer.coffer2.application

import org.coffer.coffer2.application.catalog.CatalogCoinDetailsResult
import org.coffer.coffer2.application.catalog.CatalogSearchResult

interface CoinCatalogService {

    fun searchCatalog(query: String, page: Int = 1, pageSize: Int = 20): CatalogSearchResult

    fun getCoinDetails(typeId: String): CatalogCoinDetailsResult?
}
