package org.coffer.coffer2.application.issueprice

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.config.IssuePriceProperties
import org.coffer.coffer2.domain.coin.Coin
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * High-level service for orchestrating bulk issue price updates.
 * Implements deduplication, batch processing, and rate limiting.
 */
@Service
class IssuePriceService(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val issuePriceFetchService: IssuePriceFetchService,
    private val issuePriceProperties: IssuePriceProperties
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Updates prices for all coins that have a Numista ID.
     */
    @Transactional
    fun updatePricesForAllCoins() {
        val allCoins = coinRepositoryAdapter.findByNumistaIdIsNotNull()
        logger.info { "Updating prices for all ${allCoins.size} coins with Numista IDs" }
        updatePricesForCoins(allCoins)
    }

    private fun updatePricesForCoins(coins: List<Coin>) {
        if (coins.isEmpty()) {
            logger.info { "No coins to process" }
            return
        }

        // Group coins by numistaId to deduplicate coin type lookups
        val coinsByNumistaId = coins
            .filter { it.numistaId != null }
            .groupBy { it.numistaId!! }

        logger.info { "Processing ${coinsByNumistaId.size} unique coin types for ${coins.size} coins" }

        var processedCount = 0
        var successCount = 0
        var errorCount = 0

        // Track which issues have been processed in this run to avoid duplicate API calls
        val processedIssueIds = mutableSetOf<java.util.UUID>()

        coinsByNumistaId.forEach { (numistaId, coinsGroup) ->
            try {
                // Process each coin in the group
                coinsGroup.forEach { coin ->
                    try {
                        val success = issuePriceFetchService.fetchPricesForCoin(coin.id, processedIssueIds)
                        if (success) {
                            successCount++
                        }
                        processedCount++

                        // Rate limiting: add delay after each batch
                        if (processedCount % issuePriceProperties.batchSize == 0) {
                            logger.info { "Processed $processedCount/${coins.size} coins, ${processedIssueIds.size} unique issues" }
                            Thread.sleep(issuePriceProperties.rateLimitDelayMs)
                        }
                    } catch (e: RateLimitException) {
                        logger.error { "Hit rate limit after processing $processedCount coins, stopping batch" }
                        throw e // Propagate to stop entire batch
                    }
                }
            } catch (e: RateLimitException) {
                logger.error { "Rate limited after processing $processedCount coins, will resume in next scheduled run" }
                return // Exit early, will retry in next scheduled run
            } catch (e: Exception) {
                logger.error(e) { "Failed to fetch prices for coin type $numistaId" }
                errorCount += coinsGroup.size
            }
        }

        logger.info {
            "Completed price update: processed=$processedCount, success=$successCount, errors=$errorCount, unique issues fetched=${processedIssueIds.size}"
        }
    }
}
