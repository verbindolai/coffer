package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.issue.IssueFetchService
import org.coffer.coffer2.application.issueprice.IssuePriceFetchService
import org.coffer.coffer2.application.portfolio.PortfolioSnapshotService
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NumistaIssueFetchListener(
    private val issueFetchService: IssueFetchService,
    private val issuePriceFetchService: IssuePriceFetchService,
    private val portfolioSnapshotService: PortfolioSnapshotService
) {

    private val logger = KotlinLogging.logger {}

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinCreatedEvent(event: CoinCreatedEvent) {
        try {
            if (event.numistaId.isNullOrBlank()) {
                logger.debug { "Skipping issue fetch for coin ${event.coinId} - no Numista ID" }
                return
            }
            issueFetchService.fetchIssuesFromNumista(
                coinId = event.coinId,
                numistaId = event.numistaId
            )

            // After issues are fetched, fetch prices and create a snapshot with full values
            logger.info { "Fetching issue prices for coin ${event.coinId}" }
            issuePriceFetchService.fetchPricesForCoin(event.coinId)
            portfolioSnapshotService.computeAndStoreSnapshot()
            logger.info { "Portfolio snapshot created after issue price fetch for coin ${event.coinId}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch issues/prices for coin ${event.coinId} from Numista" }
        }
    }
}