package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.issue.IssueFetchService
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NumistaIssueFetchListener(
    private val issueFetchService: IssueFetchService
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
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch issues for coin ${event.coinId} from Numista" }
        }
    }
}