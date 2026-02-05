package org.coffer.coffer2.application.numistaimport

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.application.CoinService
import org.coffer.coffer2.application.CompositionParser
import org.coffer.coffer2.application.CurrencyDeriver
import org.coffer.coffer2.application.IssuerCodeMapper
import org.coffer.coffer2.config.NumistaOAuthProperties
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.CreateCoinCommand
import org.coffer.coffer2.domain.coin.MintMark
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.remote.numista.NumistaClient
import org.coffer.coffer2.remote.numista.NumistaCollectedItem
import org.coffer.coffer2.remote.numista.NumistaOAuthClient
import org.coffer.coffer2.remote.numista.NumistaTypeResponse
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Service
class NumistaImportService(
    private val numistaOAuthClient: NumistaOAuthClient,
    private val numistaClient: NumistaClient,
    private val coinService: CoinService,
    private val coinRepository: CoinRepositoryAdapter,
    private val compositionParser: CompositionParser,
    private val properties: NumistaOAuthProperties,
) {
    private val logger = KotlinLogging.logger {}

    fun getAuthorizationUrl(state: String): String {
        return "https://en.numista.com/api/oauth_authorize.php" +
            "?response_type=code" +
            "&client_id=${properties.clientId}" +
            "&redirect_uri=${properties.redirectUri}" +
            "&scope=view_collection" +
            "&state=$state"
    }

    fun importCollection(code: String, redirectUri: String): NumistaImportResult {
        logger.info { "Starting Numista collection import" }

        val tokenResponse = numistaOAuthClient.exchangeCode(code, redirectUri)
        val userId = tokenResponse.user_id
        val accessToken = tokenResponse.access_token
        logger.info { "OAuth token obtained for user $userId" }

        val collectedItems = numistaOAuthClient.getCollectedItems(userId, accessToken)
        logger.info { "Fetched ${collectedItems.item_count} collected items from Numista" }

        var imported = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()

        // Cache type details to avoid redundant API calls for items sharing the same type
        val typeDetailsCache = mutableMapOf<String, NumistaTypeResponse>()

        // Build a set of already-imported (numistaId, year, mintMark) tuples for dedup on re-import
        val existingCoins = coinRepository.findByNumistaIdIsNotNull()
        val existingKeys = existingCoins.map { coin ->
            Triple(coin.numistaId, coin.yearOfMinting.year, coin.mintMark?.value)
        }.toSet()

        for (item in collectedItems.items) {
            val numistaId = item.type.id.toString()
            val year = item.issue?.gregorian_year ?: item.issue?.year ?: 0
            val mintMark = item.issue?.mint_letter
            val itemKey = Triple(numistaId, year, mintMark)

            if (itemKey in existingKeys) {
                logger.info { "Skipping already imported coin: ${item.type.title} (numistaId=$numistaId, year=$year, mint=$mintMark)" }
                skipped++
                continue
            }

            try {
                val typeDetails = typeDetailsCache.getOrPut(numistaId) {
                    numistaClient.getCoinType(numistaId).also {
                        // Rate limiting only when we actually call the API
                        Thread.sleep(100)
                    }
                }
                val command = mapToCreateCoinCommand(item, typeDetails)
                coinService.createCoin(command)
                imported++
                logger.debug { "Imported coin: ${item.type.title} (numistaId=$numistaId, year=$year, mint=$mintMark)" }
            } catch (e: Exception) {
                failed++
                val errorMsg = "Failed to import '${item.type.title}' (numistaId=$numistaId): ${e.message}"
                errors.add(errorMsg)
                logger.warn(e) { errorMsg }
            }
        }

        logger.info { "Import complete: imported=$imported, skipped=$skipped, failed=$failed" }
        return NumistaImportResult(imported, skipped, failed, errors)
    }

    private fun mapToCreateCoinCommand(
        item: NumistaCollectedItem,
        typeDetails: NumistaTypeResponse,
    ): CreateCoinCommand {
        val parsedComposition = compositionParser.parse(typeDetails.composition?.text)
        val issuerCode = IssuerCodeMapper.toIsoCode(typeDetails.issuer?.code)
        val currencyCode = CurrencyDeriver.derive(issuerCode, typeDetails.value?.text)
        val currency = currencyCode?.let { runCatching { Currency.getInstance(it) }.getOrNull() }
            ?: Currency.getInstance("USD")
        val country = issuerCode?.let { runCatching { Locale.of("", it) }.getOrNull() }
            ?: Locale.of("", "US")

        val year = item.issue?.gregorian_year
            ?: item.issue?.year
            ?: typeDetails.min_year
            ?: 0

        return CreateCoinCommand(
            title = typeDetails.title ?: item.type.title,
            denomination = typeDetails.value?.numeric_value?.toBigDecimal(),
            currency = currency,
            yearOfMinting = YearOfMinting(year),
            issuerCountry = country,
            mintMark = item.issue?.mint_letter?.let { MintMark(it) },
            grade = item.grade?.let { CoinGrade.fromNumistaGrade(it) },
            type = CoinType.fromNumistaString(typeDetails.type),
            notes = item.private_comment,
            numistaId = typeDetails.id,
            shape = CoinShape.fromNumistaString(typeDetails.shape),
            weightInGrams = typeDetails.weight?.toBigDecimal() ?: BigDecimal.ZERO,
            purity = parsedComposition.purity?.let { BigDecimal(it) },
            metalType = parsedComposition.metalType,
            diameterInMillimeters = typeDetails.size?.toBigDecimal(),
            thicknessInMillimeters = typeDetails.thickness?.toBigDecimal(),
            quantity = item.quantity,
        )
    }
}
