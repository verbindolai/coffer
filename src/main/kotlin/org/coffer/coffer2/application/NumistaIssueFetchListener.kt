package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NumistaIssueFetchListener(

) {

    private val logger = KotlinLogging.logger {}

    @Async
    @TransactionalEventListener
    fun handleCoinCreatedEvent(event: CoinCreatedEvent) {

    }
}