package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.catalog.CatalogCoinDetailsResult
import org.coffer.coffer2.application.catalog.CatalogSearchResultItem
import org.coffer.coffer2.application.catalog.CatalogSearchResult
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.remote.numista.NumistaClient
import org.coffer.coffer2.remote.numista.NumistaTypeResponse
import org.springframework.stereotype.Service

@Service
class CoinCatalogServiceImpl(
    private val numistaClient: NumistaClient,
    private val compositionParser: CompositionParser
) : CoinCatalogService {

    private val logger = KotlinLogging.logger {}

    override fun searchCatalog(query: String, page: Int, pageSize: Int): CatalogSearchResult {
        logger.info { "Searching catalog for: $query (page $page, pageSize $pageSize)" }

        val response = numistaClient.searchTypes(query, page, pageSize)

        val items = response.types.map { searchResult ->
            CatalogSearchResultItem(
                typeId = searchResult.id.toString(),
                title = searchResult.title,
                category = searchResult.category,
                issuerCode = searchResult.issuer?.code,
                issuerName = searchResult.issuer?.name,
                minYear = searchResult.minYear,
                maxYear = searchResult.maxYear,
                obverseThumbnailUrl = searchResult.obverseThumbnail,
                reverseThumbnailUrl = searchResult.reverseThumbnail
            )
        }

        return CatalogSearchResult(
            totalCount = response.count,
            items = items
        )
    }

    override fun getCoinDetails(typeId: String): CatalogCoinDetailsResult? {
        logger.info { "Fetching coin details for type: $typeId" }

        return try {
            val typeResponse = numistaClient.getCoinType(typeId)
            mapToCatalogCoinDetailsResult(typeResponse)
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch coin details for type: $typeId" }
            null
        }
    }

    private fun mapToCatalogCoinDetailsResult(type: NumistaTypeResponse): CatalogCoinDetailsResult {
        val parsedComposition = compositionParser.parse(type.composition?.text)

        return CatalogCoinDetailsResult(
            numistaId = type.id,
            title = type.title ?: "",
            coinType = CoinType.fromNumistaString(type.type),
            issuerCode = type.issuer?.code,
            issuerName = type.issuer?.name,
            weightInGrams = type.weight?.toBigDecimal(),
            diameterInMillimeters = type.size?.toBigDecimal(),
            thicknessInMillimeters = type.thickness?.toBigDecimal(),
            metalType = parsedComposition.metalType,
            purity = parsedComposition.purity,
            compositionText = type.composition?.text,
            denomination = type.value?.numeric_value?.toBigDecimal(),
            valueText = type.value?.text,
            suggestedCurrency = CurrencyDeriver.derive(type.issuer?.code, type.value?.text),
            minYear = type.min_year,
            maxYear = type.max_year,
            obverseImageUrl = type.obverse?.picture,
            reverseImageUrl = type.reverse?.picture,
            obverseThumbnailUrl = type.obverse?.thumbnail,
            reverseThumbnailUrl = type.reverse?.thumbnail,
            shape = type.shape,
            rulers = type.ruler?.mapNotNull { it.name } ?: emptyList()
        )
    }
}
