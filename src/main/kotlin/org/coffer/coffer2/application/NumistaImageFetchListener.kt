package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Event listener that triggers automatic image fetching from Numista when a coin is created.
 *
 * This is a thin infrastructure component that:
 * - Listens to CoinCreatedEvent asynchronously after transaction commit
 * - Delegates to CoinImageFetchService for business logic
 * - Handles top-level error logging
 */
@Component
class NumistaImageFetchListener(
    private val coinImageFetchService: CoinImageFetchService
) {

    private val logger = KotlinLogging.logger {}

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinCreatedEvent(event: CoinCreatedEvent) {
        try {
            if (event.numistaId.isNullOrBlank()) {
                return
            }

            coinImageFetchService.fetchMissingImagesFromNumista(
                coinId = event.coinId,
                numistaId = event.numistaId
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch images for coin ${event.coinId} from Numista" }
        }
    }
}
