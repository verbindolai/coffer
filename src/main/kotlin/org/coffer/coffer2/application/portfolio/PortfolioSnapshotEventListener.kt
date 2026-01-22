package org.coffer.coffer2.application.portfolio

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.MetalQuotesService
import org.coffer.coffer2.config.MetalQuotesProperties
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.coffer.coffer2.domain.coin.CoinDeletedEvent
import org.coffer.coffer2.domain.coin.CoinUpdatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PortfolioSnapshotEventListener(
    private val metalQuotesService: MetalQuotesService,
    private val portfolioSnapshotService: PortfolioSnapshotService,
    private val metalQuotesProperties: MetalQuotesProperties
) {

    private val logger = KotlinLogging.logger {}

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinCreated(event: CoinCreatedEvent) {
        logger.info { "Coin created (${event.coinId}), updating metal quotes and creating snapshot" }
        updateQuotesAndSnapshot()
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinUpdated(event: CoinUpdatedEvent) {
        logger.info { "Coin updated (${event.coinId}), updating metal quotes and creating snapshot" }
        updateQuotesAndSnapshot()
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinDeleted(event: CoinDeletedEvent) {
        logger.info { "Coin deleted (${event.coinId}), creating snapshot" }
        updateQuotesAndSnapshot()
    }

    private fun updateQuotesAndSnapshot() {
        try {
            val metalTypes = metalQuotesProperties.trackedMetalCodes.map { MetalType.fromSymbol(it) }
            metalQuotesService.updateMetalQuotes(metalTypes)
            portfolioSnapshotService.computeAndStoreSnapshot()
            logger.info { "Portfolio snapshot created after coin change" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to create portfolio snapshot after coin change" }
        }
    }
}
